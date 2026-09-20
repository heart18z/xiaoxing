import test from 'node:test';
import assert from 'node:assert/strict';
import {dependency,root} from './loader.mjs';
let serial=0;
async function fixture(t){
 const {outputFiles}=await dependency('esbuild').build({entryPoints:[root+'store/tip.uts'],bundle:true,write:false,format:'esm',platform:'node',loader:{'.uts':'ts'},plugins:[{name:'reactive',setup(build){build.onResolve({filter:/^vue$/},()=>({path:'vue',namespace:'stub'}));build.onLoad({filter:/.*/,namespace:'stub'},()=>({contents:'export const reactive = value => value;'}));}}]});
 const module=await import('data:text/javascript;base64,'+Buffer.from(outputFiles[0].text+`\n// ${serial++}`).toString('base64'));
 t.mock.timers.enable({apis:['setTimeout']});
 return module;
}
test('expired tip removes its view state after fade, so it cannot intercept popup close',async t=>{
 const {tip,showTip}=await fixture(t);
 showTip('提示');t.mock.timers.tick(30);assert.equal(tip.visible,true);assert.equal(tip.rendered,true);
 t.mock.timers.tick(3170);assert.equal(tip.visible,false);assert.equal(tip.rendered,true);
 t.mock.timers.tick(180);assert.equal(tip.rendered,false);assert.equal(tip.text,'');
});
test('replacement during exit cancels removal of the new tip',async t=>{
 const {tip,showTip}=await fixture(t);
 showTip('旧消息');t.mock.timers.tick(3200);
 showTip('智能提醒已创建','success');t.mock.timers.tick(180);
 assert.equal(tip.rendered,true);assert.equal(tip.visible,true);assert.equal(tip.tone,'success');assert.equal(tip.text,'智能提醒已创建');
 t.mock.timers.tick(3020);t.mock.timers.tick(180);assert.equal(tip.rendered,false);
});
test('successive visible tips update without flicker and restart expiry',async t=>{
 const {tip,showTip}=await fixture(t);
 showTip('第一条');t.mock.timers.tick(2000);showTip('第二条');
 assert.equal(tip.visible,true);t.mock.timers.tick(1400);assert.equal(tip.visible,true);
 t.mock.timers.tick(1800);t.mock.timers.tick(180);assert.equal(tip.rendered,false);
});
test('dismiss before entry cannot let the old entry timer show a removed tip',async t=>{
 const {tip,showTip,hideTip}=await fixture(t);
 showTip('请求失败');hideTip();t.mock.timers.tick(180);
 assert.equal(tip.visible,false);assert.equal(tip.rendered,false);
});
