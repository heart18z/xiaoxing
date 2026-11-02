import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import {dependency,root} from './loader.mjs';
async function fixture(position=900,following=true) {
  const source=fs.readFileSync(root+'pages/chat/index.uvue','utf8');
  const functions=source.slice(source.indexOf('function measureScroll()'),source.indexOf('const keyboardChange ='))+
    source.slice(source.indexOf('function stopPage()'),source.indexOf('let syncCallback:'))+
    source.slice(source.indexOf('onShow(() => {'),source.indexOf('onHide(stopPage);'));
  const setup=`let active=true,shownOwner='1',restoringUntil=0,actualScrollTop=${position},touching=false,userScrolling=false,scrollHeight=100,scrollLockedUntil=0,layoutTimer=0,revealTimer=0,scrollTimer=0,initialScrollPending=false,keyboardLayoutTimer=0,keyboardId=-1,timer=0;
    const follow={value:${following}},scrollAnimated={value:false},scrollTop={value:0},keyboard={value:0},pageShown={value:true},drawer={value:false};
    const session={userId:'1',visible:true},chat={messages:[{id:'1'}],busy:false};`;
  const {code}=await dependency('esbuild').transform(setup+functions+`
    globalThis.api={hide:stopPage,follow,scrollTop,scrollAnimated,session,read:()=>({actualScrollTop,initialScrollPending})};`,{loader:'ts'});
  const ticks=[],timers=[],requests=[];let show;let height=1000;
  const env={reportViewer:{open:true},requireSession:()=>true,nextTick:fn=>ticks.push(fn),onShow:fn=>show=fn,
    setChatVisible:()=>{},cancelRecording:()=>{},keyboardChange:()=>{},
    loadHistory:async()=>requests.push('history'),resume:async()=>{},bootstrap:async()=>{},scheduleSync:()=>requests.push('sync'),
    setTimeout:fn=>{timers.push(fn);return timers.length;},clearTimeout:()=>{},setInterval:fn=>{timers.push(fn);return timers.length;},clearInterval:()=>{},
    uni:{getElementById:id=>({getBoundingClientRect:()=>({height:id==='chat-scroll'?100:height})}),offKeyboardHeightChange:()=>{},onKeyboardHeightChange:()=>1},
    document:{getElementById:id=>({clientHeight:100,getBoundingClientRect:()=>({height:id==='chat-scroll'?100:height})})}};
  vm.runInNewContext(code,env);
  return {...env.api,requests,timers,show:()=>show(),layout:()=>ticks.splice(0).forEach(f=>f()),resize:value=>height=value};
}
test('warm resume at bottom keeps the saved offset without history reload or scroll settling loop',async()=>{
  const p=await fixture();p.hide();assert.equal(p.scrollTop.value,900);
  p.show();p.layout();assert.equal(p.scrollTop.value,900);assert.equal(p.scrollAnimated.value,false);
  assert.deepEqual(p.requests,['sync']);assert.equal(p.timers.length,0);assert.equal(p.read().initialScrollPending,false);
});
test('warm resume preserves the position of someone reading older messages',async()=>{
  const p=await fixture(400,false);p.hide();p.show();p.layout();
  assert.equal(p.follow.value,false);assert.equal(p.scrollTop.value,400);assert.equal(p.timers.length,0);
});
test('viewport changes restore a followed bottom immediately without an animated sweep',async()=>{
  const p=await fixture();p.hide();p.resize(1200);p.show();p.layout();
  assert.equal(p.scrollTop.value,1100);assert.equal(p.scrollAnimated.value,false);
});
test('switching accounts resets the saved scroll and reloads the correct history',async()=>{
  const p=await fixture(400,false);p.hide();p.session.userId='2';p.show();
  assert.equal(p.read().actualScrollTop,0);assert.equal(p.follow.value,true);assert.equal(p.read().initialScrollPending,true);
  assert.deepEqual(p.requests,['history','sync']);
});
