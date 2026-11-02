import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import {dependency, root} from './loader.mjs';

async function fixture() {
  const props={open:false},timers=new Map(),ticks=[],requests=[];let nextId=0,changed,dispose;
  const env={defineProps:()=>props,defineEmits:()=>()=>{},ref:value=>({value}),computed:f=>({get value(){return f();}}),
    uni:{getWindowInfo:()=>({safeAreaInsets:{top:20}})},session:{userId:'1'},
    _s:(r,k,f='')=>String(r[k]??f),_b:(r,k)=>r[k]===true,_firstText:()=>'',_t:v=>v,_displayTime:v=>v,
    _openEvent:()=>{},message:e=>e.message,reminderGroups:()=>[],
    HorizontalSwipe:class{cancel(){}},Date,
    nextTick:f=>ticks.push(f),onUnmounted:f=>dispose=f,watch:(_,f)=>changed=f,
    setTimeout:f=>{timers.set(++nextId,f);return nextId;},clearTimeout:id=>timers.delete(id),
    reminder:()=>new Promise(resolve=>requests.push(resolve))};
  const script=fs.readFileSync(root+'components/ReminderDrawer.uvue','utf8').match(/<script setup lang="uts">([\s\S]*?)<\/script>/)[1]
    .replace(/^import[\s\S]*?from ['"][^'"]+['"];\s*/gm,'');
  const {code}=await dependency('esbuild').transform(script+'\nglobalThis.api={rendered,visible,loading,rows,load,type};',{loader:'ts'});
  vm.runInNewContext(code,env);
  return {...env.api,env,props,requests,timers,ticks,open:value=>{props.open=value;changed(value);},dispose:()=>dispose(),
    layout:()=>{for(const fn of ticks.splice(0))fn();},frame:()=>{const work=[...timers.values()];timers.clear();work.forEach(fn=>fn());}};
}
test('drawer waits for layout and ignores obsolete transitions during rapid close/open or unmount',async()=>{
  const p=await fixture();p.type.value=1;p.open(true);
  assert.equal(p.rendered.value,true);assert.equal(p.visible.value,false);assert.equal(p.timers.size,0);
  p.open(false);p.layout();p.frame();assert.equal(p.rendered.value,false);assert.equal(p.visible.value,false);
  p.open(true);p.layout();p.frame();assert.equal(p.visible.value,true);
  p.open(false);p.open(true);p.layout();p.frame();assert.equal(p.rendered.value,true);assert.equal(p.visible.value,true);
  p.open(false);p.open(true);p.dispose();p.layout();assert.equal(p.timers.size,0);
});
test('drawer keeps loaded cards during refresh and ignores closed or previous-account responses',async()=>{
  const p=await fixture();p.type.value=1;p.rows.value=[{eventId:'old'}];
  // Establish the owner of this cached list.
  const initial=p.load();p.requests[0]({value:[{eventId:'old'}]});await initial;
  const refresh=p.load();assert.equal(p.rows.value[0].eventId,'old');
  p.open(false);p.requests[1]({value:[{eventId:'late'}]});await refresh;
  assert.equal(p.rows.value[0].eventId,'old');assert.equal(p.loading.value,false);
  p.env.session.userId='2';const next=p.load();assert.equal(p.rows.value.length,0);
  p.requests[2]({value:[{eventId:'new'}]});await next;assert.equal(p.rows.value[0].eventId,'new');
});
