import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const source=readFileSync('src/page/smart-reminder/pendingChat.js','utf8').replace(/^import .*;\r?\n/gm,'').replace('const wait=ms=>new Promise(resolve=>setTimeout(resolve,ms));','const wait=async()=>{};');
let serial=0;
async function fixture(get,submit,online=true,watch=async()=>{throw Error('watch not expected');}){
  const values=new Map(),state={getters:{userInfo:{user_id:'1'}}};
  const env={store:state,isNative:false,document:{hidden:false},navigator:{onLine:online},localStorage:{getItem:k=>values.get(k),setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},getChatJob:async id=>({data:{data:await get(id,state)}}),submitChatJob:async r=>({data:{data:await submit(r)}})};
  globalThis.__pendingFixture=env;
  env.watchChatJob=watch;
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

test('normal processing never reports a connection interruption',async()=>{
  let queries=0;const notices=[];
  const f=await fixture(()=>++queries<3?{status:'RUNNING'}:{status:'SUCCEEDED',result:{reply:'完成'}},()=>{});
  await f.module.followChatJob(f.request,{onWaiting:v=>notices.push(v)});
  assert.ok(notices.every(v=>v===''));
});
test('one failed poll is silent; repeated server errors are not mislabeled as offline and clear on recovery',async()=>{
  let queries=0;const notices=[];
  const f=await fixture(()=>{queries++;if(queries<=3)throw Object.assign(Error('server error'),{response:{status:500}});return queries===4?{status:'RUNNING'}:{status:'SUCCEEDED',result:{reply:'完成'}};},()=>{});
  await f.module.followChatJob(f.request,{onWaiting:v=>notices.push(v)});
  assert.match(notices[0],/服务端查询/);assert.equal(notices.filter(Boolean).length,1);assert.equal(notices.at(-1),'');assert.ok(!notices.some(v=>v.includes('连接暂时中断')));
});
test('a transient timeout does not flash an error banner or duplicate the job',async()=>{
  let queries=0;const notices=[];
  const f=await fixture(()=>{if(++queries===1)throw Object.assign(Error('timeout'),{code:'ECONNABORTED'});return{status:'SUCCEEDED',result:{reply:'完成'}};},()=>{throw Error('must not resubmit');});
  await f.module.followChatJob(f.request,{onWaiting:v=>notices.push(v)});assert.ok(notices.every(v=>!v));
});
test('authorization and missing endpoints retain pending work without endlessly retrying',async()=>{
  for(const status of [401,403,404]){let calls=0;const f=await fixture(()=>{calls++;throw Object.assign(Error('HTTP error'),{status});},()=>{});
    await assert.rejects(f.module.followChatJob(f.request),e=>e.pending===true);assert.equal(calls,1);assert.ok(f.module.pendingChat());}
});
test('account switch during a missing-job response never submits for the new owner',async()=>{
  const f=await fixture((id,state)=>{state.getters.userInfo.user_id='2';return{status:'NOT_FOUND'};},()=>{throw Error('must not submit');});
  await assert.rejects(f.module.followChatJob(f.request),e=>e.pending===true);
});
test('an actually offline device gets an immediate, accurate notice',async()=>{
  let queries=0;const notices=[];
  const f=await fixture(()=>{if(++queries===1)throw Error('offline');return{status:'SUCCEEDED',result:{reply:'完成'}};},()=>{},false);
  await f.module.followChatJob(f.request,{onWaiting:v=>notices.push(v)});assert.match(notices[0],/设备离线/);assert.equal(notices.at(-1),'');
});
test('queued and running messages have no queue banner',async()=>{
  let queries=0;const notices=[];
  const f=await fixture(()=>++queries===1?{status:'QUEUED'}:{status:'SUCCEEDED',result:{reply:'你好'}},()=>{});
  await f.module.followChatJob(f.request,{onWaiting:v=>notices.push(v)});assert.ok(notices.every(v=>v===''));
});
test('accepted jobs use live snapshots without resubmission or duplicated reasoning',async()=>{
  let reads=0,watches=0;const thoughts=[];
  const f=await fixture(()=>{reads++;return{status:'RUNNING',streamSupported:true};},()=>{throw Error('duplicate submit');},true,async(id,handlers)=>{watches++;handlers.onReasoning('正在');handlers.onReasoning('正在确认');return{status:'SUCCEEDED',result:{reply:'你好'}};});
  const result=await f.module.followChatJob(f.request,{onReasoning:v=>thoughts.push(v)});
  assert.equal(result.reply,'你好');assert.deepEqual(thoughts,['正在','正在确认']);assert.equal(watches,1);assert.equal(reads,1);
});
test('lost observation stream falls back to status only, silently and without resubmission',async()=>{
  let reads=0;const notices=[];
  const f=await fixture(()=>++reads===1?{status:'RUNNING',streamSupported:true}:{status:'SUCCEEDED',result:{reply:'已提交'}},()=>{throw Error('duplicate submit');},true,async()=>{throw Error('connection suspended');});
  const result=await f.module.followChatJob(f.request,{onWaiting:v=>notices.push(v)});
  assert.equal(result.reply,'已提交');assert.ok(notices.every(v=>v===''));assert.equal(reads,2);
});
