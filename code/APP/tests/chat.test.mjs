import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import fs from 'node:fs';
import {dependency,root} from './loader.mjs';
let serial=0;
async function fixture(handler,stream=[],history=null,native=false){
  const storage=new Map(),session={userId:'1',revision:0,visible:true},calls=[];
  class ApiError extends Error{constructor(status,message){super(message);this.status=status}}
  const env={session,persist:(k,v)=>storage.set(k,v),requestId:()=>`request-id-${'a'.repeat(24)}`,secureRead:k=>storage.get(k)||'{}',headers:()=>({}),BASE_URL:'https://fixture.invalid',reactive:v=>v,ApiError,
    reminder:async(action,data,params)=>{calls.push([action,data,params]);if(action==='chat/messages'||action==='chat/read')return {value:history?await history(action,data):action==='chat/messages'?[]:true};return {value:await handler(action,data,session)}}};
  const streams=[...stream];let aborted=0;
  env.nativeCall=(action,input,callback)=>{
    if(action==='stream.cancel'){aborted++;return;}
    queueMicrotask(()=>{
      const batch=streams.shift();if(batch===null)return;
      for(const frame of batch||[])callback(JSON.stringify({text:`data: ${JSON.stringify(frame)}\n\n`}));
      callback(JSON.stringify({ended:true}));
    });
  };
  globalThis.uni={request:options=>{let stopped=false;return{abort(){if(stopped)return;stopped=true;aborted++;options.fail?.();options.complete?.()},onChunkReceived(callback){queueMicrotask(()=>{const batch=streams.shift();if(batch===null)return;for(const frame of batch||[]){if(stopped)break;const bytes=Buffer.from(`data: ${JSON.stringify(frame)}\n\n`);for(const byte of bytes){if(stopped)break;callback({data:Uint8Array.of(byte).buffer})}}if(!stopped){options.success?.({statusCode:200});options.complete?.()}})}}}};
  globalThis.__chatFixture=env;JSON.parseObject=JSON.parse;
  const imports=['vue','@/store/session.uts','@/uni_modules/xiaoxing-native','@/api/request.uts','@/config/environment.uts'];
  const bundle=await dependency('esbuild').build({entryPoints:[root+'services/chat.uts'],bundle:true,write:false,format:'esm',platform:'node',loader:{'.uts':'ts'},plugins:[{name:'fixture',setup(build){build.onLoad({filter:/services[\\/]chat\.uts$/},args=>({contents:fs.readFileSync(args.path,'utf8').replace(native?/\/\/ #ifndef APP[\r\n][\s\S]*?\/\/ #endif/g:/\/\/ #ifdef APP-ANDROID[\s\S]*?\/\/ #endif/g,''),loader:'ts'}));build.onResolve({filter:/.*/},args=>imports.includes(args.path)?{path:args.path,namespace:'fixture'}:args.path.startsWith('@/')?{path:path.join(root,args.path.slice(2))}:null);build.onLoad({filter:/.*/,namespace:'fixture'},()=>({contents:'export const {'+Object.keys(env).join(',')+'}=globalThis.__chatFixture;',loader:'js'}))}}]});
  const module=await import('data:text/javascript;base64,'+Buffer.from(bundle.outputFiles[0].text+`\n// fixture ${serial++}`).toString('base64'));
  return{module,session,calls,storage,ApiError,aborted:()=>aborted};
}
test('same request ID submits once and clears only on committed success',async()=>{
  let step=0;const f=await fixture(action=>{if(action==='chat/jobs/status')return step++===0?{status:'NOT_FOUND'}:{status:'SUCCEEDED',result:{reply:'完成'}};return{status:'RUNNING'}});
  await f.module.submit('明天提醒',[]);
  assert.equal(f.calls.filter(x=>x[0]==='chat/jobs/submit').length,1);assert.equal(f.module.savedPending().requestId,undefined);
  assert.equal(f.calls.find(x=>x[0]==='chat/jobs/submit')[1].requestId,'request-id-'+ 'a'.repeat(24));
});
test('account changes during status response never submit for the new user',async()=>{
  const f=await fixture((action,data,session)=>{session.userId='2';session.revision++;return{status:'NOT_FOUND'}});
  await f.module.submit('保留消息',[]);assert.equal(f.calls.filter(x=>x[0]==='chat/jobs/submit').length,0);assert.ok(f.storage.get('pending:1').includes('保留消息'));
});
test('hidden app pauses observation without cancelling accepted work',async()=>{
  const f=await fixture((action,data,session)=>{session.visible=false;return{status:'RUNNING'}});
  await f.module.submit('提醒',[]);f.module.pauseChat();assert.ok(f.module.savedPending().requestId);assert.equal(f.calls.filter(x=>x[0]==='chat/stop').length,0);
});
test('UNKNOWN preserves the durable pending message and requests review',async()=>{
  const f=await fixture(()=>({status:'UNKNOWN'}));await f.module.submit('提醒',[]);assert.ok(f.module.savedPending().requestId);assert.match(f.module.chat.notice,/核对/);
});
test('authorization failure retains pending work without mutation retry',async()=>{
  let f;f=await fixture(()=>{throw new f.ApiError(401,'登录过期')});await f.module.submit('提醒',[]);assert.ok(f.module.savedPending().requestId);assert.equal(f.calls.length,1);
});
test('POST SSE RUNNING result continues observing and replaces reasoning snapshots',async()=>{
  const f=await fixture(()=>({status:'RUNNING',streamSupported:true}),[
    [{type:'snapshot',reasoning:'思考'},{type:'result',data:{status:'RUNNING',streamSupported:true}}],
    [{type:'snapshot',reasoning:'思考完成🔔'},{type:'result',data:{status:'SUCCEEDED',result:{reply:'已处理'}}}]
  ]);
  await f.module.submit('提醒',[]);assert.equal(f.module.chat.reasoning,'思考完成🔔');assert.equal(f.calls.filter(x=>x[0]==='chat/jobs/submit').length,0);assert.equal(f.aborted(),2);
});

test('definite failure clears pending recovery and rejects so composer can restore draft',async()=>{
 const f=await fixture(()=>({status:'FAILED',message:'模型服务暂不可用'}));
 await assert.rejects(f.module.submit('保留我的提醒',[]),/模型服务暂不可用/);
 assert.equal(f.module.chat.notice,'');assert.equal(f.module.savedPending().requestId,undefined);assert.equal(f.module.chat.busy,false);
});

test('sync revision advances without replacing unchanged messages, including during live generation',async()=>{
 let step=0;const f=await fixture(()=>step++===0?{revision:'r1',messages:[{id:'1',messageType:'TEXT',content:'server echo'},{id:'2',messageType:'REMINDER',isRead:false,content:'new reminder'}]}:{revision:'r1'});
 f.module.setChatVisible(true);f.module.chat.busy=true;f.module.chat.pending={requestId:'active'};f.module.chat.messages=[{id:'old',content:'retained'}];
 await f.module.syncHistory();await f.module.syncHistory();
 assert.deepEqual(f.module.chat.messages.map(m=>m.id),['old','2']);
 assert.equal(f.calls.filter(c=>c[0]==='chat/sync')[1][2].revision,'r1');
 assert.deepEqual(f.calls.find(c=>c[0]==='chat/read')[1].messageIds,['2']);
});
test('clearing context invalidates an in-flight sync response',async()=>{
 let resolve;const f=await fixture(()=>new Promise(r=>resolve=r));f.module.setChatVisible(true);
 const pending=f.module.syncHistory();f.module.invalidateHistory();resolve({revision:'old',messages:[{id:'old'}]});await pending;
 assert.equal(f.module.chat.messages.length,0);
});
test('committed reply stays visible when history enrichment fails and composer unlocks',async()=>{
 const f=await fixture(()=>({status:'SUCCEEDED',result:{reply:'已创建'}}),[],()=>{throw Error('history offline')});
 await f.module.submit('', ['file1'], ['合同.pdf']);await new Promise(r=>setTimeout(r,0));
 assert.equal(f.module.chat.busy,false);assert.equal(f.module.chat.pending.requestId,undefined);
 assert.equal(f.module.chat.messages.at(-1).content,'已创建');assert.deepEqual(f.module.chat.messages[0].payload.fileNames,['合同.pdf']);
});
test('read receipt failure does not turn a loaded conversation into a history failure',async()=>{
 const f=await fixture(()=>({}),[],action=>{if(action==='chat/read')throw Error('receipt offline');return[{id:'1',isRead:false}]});
 f.module.setChatVisible(true);await f.module.loadHistory();await new Promise(r=>setTimeout(r,0));assert.equal(f.module.chat.error,'');assert.equal(f.module.chat.messages.length,1);
});
test('stop obeys the backend stopped flag instead of pretending every request cancelled',async()=>{
 let accept=false;const f=await fixture(()=>({stopped:accept}));f.module.chat.pending={requestId:'pending'};
 assert.equal(await f.module.stopGeneration(),false);assert.equal(f.module.chat.notice,'');accept=true;
 assert.equal(await f.module.stopGeneration(),true);assert.match(f.module.chat.notice,/服务器确认/);
});

test('committed candidate appears even if history reload fails',async()=>{
 const f=await fixture(()=>({status:'SUCCEEDED',result:{intent:'create_event',reply:'请确认',candidateId:'321',events:[{summary:'明天交标书'}]}}),[],()=>{throw new Error('offline')});
 await f.module.submit('提醒我明天交标书',[]);
 const card=f.module.chat.messages.find(x=>x.messageType==='CANDIDATE');
 assert.equal(card.payload.candidateId,'321');assert.equal(card.payload.events[0].summary,'明天交标书');assert.equal(f.module.chat.busy,false);
});


test('buffered stream falls back to status polling without resubmitting the job',async()=>{
 let status=0;
 const f=await fixture(action=>{
  if(action==='chat/jobs/status')return ++status===1?{status:'NOT_FOUND'}:{status:'SUCCEEDED',result:{reply:'完成',reasoningContent:'逐步确认完成'}};
  return {status:'RUNNING',streamSupported:true};
 },[null],()=>{throw new Error("history unavailable")});
 await f.module.submit('明天提醒我',[]);
 assert.equal(f.calls.filter(c=>c[0]==='chat/jobs/submit').length,1);
 assert.equal(f.aborted(),1);
 assert.equal(f.module.chat.messages.at(-1).reasoningContent,'逐步确认完成');
 assert.equal(f.module.chat.busy,false);
});


test('committed reply is revealed progressively and survives hiding without resubmission',async()=>{
 const text='这是服务器已确认的回复，包含中文和表情😀，逐步展示最终结果。';
 const f=await fixture(()=>({status:'SUCCEEDED',result:{reply:text}}),[],()=>{throw Error('offline')});
 const sending=f.module.submit('测试展示',[]);
 await new Promise(resolve=>setTimeout(resolve,65));
 assert.ok(f.module.chat.reply.length>0 && f.module.chat.reply.length<text.length);
 assert.equal(f.module.chat.busy,true);
 f.session.visible=false;f.module.pauseChat();await sending;
 assert.ok(f.module.savedPending().requestId);
 f.session.visible=true;await f.module.resume();
 assert.equal(f.module.chat.messages.at(-1).content,text);
 assert.equal(f.calls.filter(c=>c[0]==='chat/jobs/submit').length,0);
 assert.equal(f.module.chat.busy,false);
});


test('Android and iOS native snapshots stream immediately and hiding releases observation for resume',async()=>{
 let status=0;
 const f=await fixture(action=>{
  if(action==='chat/jobs/status')return ++status===1?{status:'NOT_FOUND'}:{status:'SUCCEEDED',result:{reply:'已完成'}};
  return {status:'RUNNING',streamSupported:true};
 },[[{type:'snapshot',reasoning:'正在核对时间'}]],()=>{throw Error('offline')},true);
 const send=f.module.submit('明天提醒',[]);
 await new Promise(resolve=>setTimeout(resolve,10));
 assert.equal(f.module.chat.reasoning,'正在核对时间');
 await send;
 assert.equal(f.module.chat.messages.at(-1).content,'已完成');
 const g=await fixture(action=>action==='chat/jobs/submit'?{status:'RUNNING',streamSupported:true}:{status:'NOT_FOUND'},[null],null,true);
 const pending=g.module.submit('暂停连接',[]);
 await new Promise(resolve=>setTimeout(resolve,20));
 g.session.visible=false;g.module.pauseChat();
 await Promise.race([pending,new Promise((_,reject)=>setTimeout(()=>reject(Error('native observe did not settle')),500))]);
 assert.equal(g.aborted(),1);assert.ok(g.module.savedPending().requestId);
});
