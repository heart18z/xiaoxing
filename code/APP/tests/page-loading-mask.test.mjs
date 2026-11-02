import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import {dependency,root} from './loader.mjs';
async function fixture() {
  const source=fs.readFileSync(root+'components/PageLoadingMask.uvue','utf8');
  const script=source.match(/<script setup lang="uts">([\s\S]*?)<\/script>/)[1].replace(/^import .*;\r?\n/gm,'');
  const {code}=await dependency('esbuild').transform(script+'\nglobalThis.state={rendered,visible,position};',{loader:'ts'});
  const props={active:false},timeouts=new Map(),intervals=new Map();let now=0,id=0,change,dispose;
  const env={Date:{now:()=>now},Math,ref:value=>({value}),defineProps:()=>props,_t:v=>v,
    watch:(_,fn)=>{change=fn;fn(false);},onUnmounted:fn=>dispose=fn,
    setInterval:fn=>{intervals.set(++id,fn);return id;},clearInterval:id=>intervals.delete(id),
    setTimeout:(fn,delay)=>{timeouts.set(++id,{fn,due:now+delay});return id;},clearTimeout:id=>timeouts.delete(id)};
  vm.runInNewContext(code,env);
  return {...env.state,intervals,timeouts,change:value=>{props.active=value;change(value);},dispose:()=>dispose(),
    advance:ms=>{now+=ms;for(const fn of intervals.values())fn();for(const [key,t] of [...timeouts])if(t.due<=now){timeouts.delete(key);t.fn();}}};
}
test('page mask covers immediately and fades only after the minimum display time',async()=>{
  const p=await fixture();assert.equal(p.rendered.value,false);assert.equal(p.timeouts.size,0);
  p.change(true);assert.equal(p.rendered.value,true);assert.equal(p.visible.value,true);
  p.advance(80);assert.ok(p.position.value>0);p.change(false);assert.equal(p.intervals.size,0);
  p.advance(159);assert.equal(p.visible.value,true);p.advance(1);assert.equal(p.visible.value,false);
  p.advance(160);assert.equal(p.rendered.value,false);assert.equal(p.timeouts.size,0);
});
test('restarting loading during fade cancels stale removal and keeps a single animation clock',async()=>{
  const p=await fixture();p.change(true);p.advance(400);p.change(false);p.advance(0);
  assert.equal(p.visible.value,false);p.change(true);p.advance(200);
  assert.equal(p.visible.value,true);assert.equal(p.rendered.value,true);assert.equal(p.intervals.size,1);
  p.change(false);p.advance(0);p.advance(160);assert.equal(p.rendered.value,false);
});
test('page disposal cancels animation and delayed removal callbacks',async()=>{
  const p=await fixture();p.change(true);p.change(false);p.dispose();
  assert.equal(p.intervals.size,0);assert.equal(p.timeouts.size,0);
});
