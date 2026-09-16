import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {consumeChatStream,requestChatStream} from '../src/api/chatStream.mjs';
import {mobileError} from '../src/page/smart-reminder/mobileError.mjs';

const frame=x=>'data:'+JSON.stringify(x)+'\n\n';
const response=(text,close=true)=>new Response(new ReadableStream({start(c){c.enqueue(new TextEncoder().encode(text));if(close)c.close();}}));
test('committed result completes even if server keeps stream open; Unicode survives chunks',async()=>{
  const text=frame({type:'reasoning',content:'判断时间'})+frame({type:'result',data:{reply:'已处理 1 个事件。'}});
  let thought='';
  const value=await consumeChatStream(response(text,false),{onReasoning:s=>thought+=s});
  assert.equal(value.reply,'已处理 1 个事件。');assert.equal(thought,'判断时间');
  const bytes=new TextEncoder().encode(text);
  const chunked=new Response(new ReadableStream({start(c){for(const byte of bytes)c.enqueue(Uint8Array.of(byte));c.close();}}));
  assert.deepEqual(await consumeChatStream(chunked),value);
});
test('EOF without commit or server errors never count as success',async()=>{
  await assert.rejects(consumeChatStream(response(frame({type:'delta',content:'已处理'}))),/结果待确认/);
  await assert.rejects(consumeChatStream(response(frame({type:'error',message:'没有权限'}))),/没有权限/);
});
test('job observer snapshots are replacements and lease endings need not imply task completion',async()=>{
  const snapshots=[];
  const value=await consumeChatStream(response(frame({type:'snapshot',reasoning:'第一段'})+frame({type:'snapshot',reasoning:'第一段第二段'})+frame({type:'result',data:{status:'RUNNING',streamSupported:true}})),{onSnapshot:value=>snapshots.push(value.reasoning)});
  assert.deepEqual(snapshots,['第一段','第一段第二段']);assert.equal(value.status,'RUNNING');
});
test('retry exactly once on HTTP 401 only, never on network or business errors',async()=>{
  let sends=0,refreshes=0;
  const r=await requestChatStream(async()=>new Response('',{status:++sends===1?401:200}),async()=>{refreshes++;});
  assert.equal(r.status,200);assert.equal(sends,2);assert.equal(refreshes,1);
  sends=0;await requestChatStream(async()=>{sends++;return new Response('',{status:500});},()=>{throw Error('must not refresh');});assert.equal(sends,1);
  await assert.rejects(requestChatStream(async()=>{throw Error('offline');},()=>{throw Error('must not retry');}),/offline/);
  sends=0;const denied=await requestChatStream(async()=>{sends++;return new Response('',{status:401});},async()=>{});assert.equal(denied.status,401);assert.equal(sends,2);
});
test('mobile errors do not show raw Axios or SQL',()=>{
  assert.match(mobileError(new Error('AxiosError: timeout of 10000ms exceeded')),/连接超时/);
  assert.match(mobileError(new Error('AxiosError: Network Error')),/网络连接失败/);
  assert.equal(mobileError(new Error('Jdbc SQLSyntax exception')),'操作未完成，请稍后重试');
  assert.equal(mobileError(new Error('密码不正确')),'密码不正确');
});
test('native login memory stores account only and web memory survives module reload',async()=>{
  const source=readFileSync('src/page/smart-reminder/loginMemory.js','utf8').replace(/^import .*;\r?\n/gm,'');
  const state={},web=new Map();globalThis.__loginFixture={state,web};
  try{
    const prefix='const {state,web}=globalThis.__loginFixture;const secureGet=k=>state[k],secureSet=(k,v)=>state[k]=v;const localStorage={getItem:k=>web.get(k),setItem:(k,v)=>web.set(k,v)};';
    for(const native of [true,false]){
      const module=await import('data:text/javascript;base64,'+Buffer.from(prefix+'const isNative='+native+';'+source).toString('base64'));
      assert.equal(module.lastLoginAccount(),'');module.rememberLoginAccount(' 2026091503 ');assert.equal(module.lastLoginAccount(),'2026091503');
    }
    assert.deepEqual(state,{lastLoginAccount:'2026091503'});assert.equal(web.size,1);
  }finally{delete globalThis.__loginFixture;}
});
