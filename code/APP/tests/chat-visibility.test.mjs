import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {dependency,root} from './loader.mjs';

async function fixture() {
 const source=fs.readFileSync(root+'pages/chat/index.uvue','utf8');
 const script=source.slice(source.indexOf('function measureScroll()'),source.indexOf('const keyboardChange ='));
 const {code}=await dependency('esbuild').transform(script,{loader:'ts'});
 const timers=[],ticks=[];let reads=0;
 const dom={getElementById:()=>{reads++;return {clientHeight:100,getBoundingClientRect:()=>({height:100})}}};
 const result=new Function('uni','document','setTimeout','clearTimeout','setInterval','clearInterval','nextTick',`
 let active=true;const session={visible:true},follow={value:true},scrollAnimated={value:false},scrollTop={value:0},chat={busy:false},keyboard={value:0};
 let touching=false,userScrolling=false,scrollHeight=0,actualScrollTop=0,scrollLockedUntil=0,layoutTimer=0,revealTimer=0,scrollTimer=0,initialScrollPending=false,keyboardLayoutTimer=0;
 ${code}
 return {measureScroll,moveScroll,scrollLatest,revealThinking,updateKeyboard,scrollTop,setVisibility:(page,app)=>{active=page;session.visible=app}};
 `)(dom,dom,fn=>{timers.push(fn);return timers.length},()=>{},fn=>{timers.push(fn);return timers.length},()=>{},fn=>ticks.push(fn));
 return {...result,timers,ticks,reads:()=>reads};
}

test('hidden/background chat ignores late layout and keyboard work',async()=>{
 for(const visibility of [[false,true],[true,false]]){
  const f=await fixture();f.setVisibility(...visibility);
  f.measureScroll();f.scrollLatest();f.revealThinking('panel');f.updateKeyboard(300);f.moveScroll(400,false);
  assert.equal(f.reads(),0);assert.equal(f.timers.length,0);assert.equal(f.scrollTop.value,0);
 }
});

test('already queued layout and nextTick scroll stop after backgrounding',async()=>{
 const f=await fixture();f.scrollLatest();f.revealThinking('panel');f.moveScroll(100,false);f.moveScroll(100,false);
 assert.ok(f.timers.length>0);assert.ok(f.ticks.length>0);
 f.setVisibility(true,false);f.timers.forEach(fn=>fn());f.ticks.forEach(fn=>fn());
 assert.equal(f.reads(),0);assert.equal(f.scrollTop.value,0);
 f.setVisibility(true,true);f.scrollLatest();f.timers.at(-1)();assert.ok(f.reads()>0);
});
