import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const source=readFileSync('src/page/smart-reminder/pendingChat.js','utf8').replace(/^import .*;\r?\n/gm,'').replace('const wait=ms=>new Promise(resolve=>setTimeout(resolve,ms));','const wait=async()=>{};');
let serial=0;
async function fixture(get,submit){
  const values=new Map(),state={getters:{userInfo:{user_id:'1'}}};
  const env={store:state,isNative:false,document:{hidden:false},localStorage:{getItem:k=>values.get(k),setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},getChatJob:async id=>({data:{data:await get(id,state)}}),submitChatJob:async r=>({data:{data:await submit(r)}})};
  globalThis.__pendingFixture=env;
  const prefix='const {'+Object.keys(env).join(',')+'}=globalThis.__pendingFixture;';
  const module=await import('data:text/javascript;base64,'+Buffer.from(prefix+source+'\n//'+ ++serial).toString('base64'));
  const request=await module.rememberPendingChat({requestId:'abcdefghijklmnop',content:'已发送内容',fileIds:[]});return{module,request,state};
}
test('lost acknowledgement retries the same request id, never a new mutation identity',async()=>{
  let queries=0,submits=[];
  const f=await fixture(()=>{queries++;if(queries<3)return{status:'NOT_FOUND'};return{status:'SUCCEEDED',result:{reply:'完成'}};},r=>{submits.push(r.requestId);throw Error('ack lost');});
  assert.deepEqual(await f.module.followChatJob(f.request),{reply:'完成'});assert.deepEqual(submits,['abcdefghijklmnop','abcdefghijklmnop']);assert.equal(f.module.pendingChat(),null);
});
test('uncertain execution retains the pending record and does not resubmit',async()=>{
  const f=await fixture(()=>({status:'UNKNOWN'}),()=>{throw Error('must not resubmit');});
  await assert.rejects(f.module.followChatJob(f.request),e=>e.pending===true);assert.equal(f.module.pendingChat().requestId,f.request.requestId);
});
test('switching accounts during status response does not reveal or clear another account result',async()=>{
  const f=await fixture((id,state)=>{state.getters.userInfo.user_id='2';return{status:'SUCCEEDED',result:{reply:'private'}};},()=>{});
  await assert.rejects(f.module.followChatJob(f.request),e=>e.pending===true);assert.equal(f.module.pendingChat(),null);
  f.state.getters.userInfo.user_id='1';assert.equal(f.module.pendingChat().requestId,f.request.requestId);
});
test('only definite failure/cancellation clears pending work',async()=>{
  const f=await fixture(()=>({status:'CANCELLED',message:'已停止生成'}),()=>{});
  await assert.rejects(f.module.followChatJob(f.request),e=>e.cancelled&&e.definite);assert.equal(f.module.pendingChat(),null);
});
