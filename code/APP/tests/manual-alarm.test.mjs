import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import {dependency,root} from './loader.mjs';
let serial=0;
async function fixture(records, detail){
 const calls=[],session={userId:'me',visible:true,revision:0,language:'zh-cn'};
 const env={session,reactive:v=>v,persist:()=>{},clearSession:()=>{},secureRead:()=>'',randomHex:()=> 'a'.repeat(32),openFriendRequests:()=>{},openEvent:()=>{},resetChat:()=>{},BASE_URL:'https://fixture.invalid',BASIC_AUTH:'fixture',TENANT:'test',BUNDLE_ID:'test',PUSH_ENVIRONMENT:'production',post:async()=>({}),reminder:async()=>({value:typeof detail==='function'?await detail(session):detail}),nativeCall:(action,input,callback)=>{
  const data=JSON.parse(input);calls.push({action,data});
  callback(JSON.stringify(action==='alarm.list'?{supported:true,alarms:records}:action==='alarm.schedule'?{id:'alarm'}:{supported:true}));
 }};
 globalThis.__alarmFixture=env;JSON.parseObject=JSON.parse;
 const stubs=['vue','@/uni_modules/xiaoxing-native','@/store/session.uts','@/api/request.uts','@/config/environment.uts','@/store/navigation.uts','@/core/ui.uts','./chat.uts'];
 const bundle=await dependency('esbuild').build({entryPoints:[root+'services/native.uts'],bundle:true,write:false,format:'esm',platform:'node',loader:{'.uts':'ts'},plugins:[{name:'fixture',setup(build){build.onResolve({filter:/.*/},args=>stubs.includes(args.path)?{path:args.path,namespace:'fixture'}:args.path.startsWith('@/')?{path:path.join(root,args.path.slice(2))}:null);build.onLoad({filter:/.*/,namespace:'fixture'},()=>({contents:'export const {'+Object.keys(env).join(',')+'}=globalThis.__alarmFixture;',loader:'js'}));}}]});
 const api=await import('data:text/javascript;base64,'+Buffer.from('const setInterval=()=>0; const clearInterval=()=>{};\n'+bundle.outputFiles[0].text+`\n// ${serial++}`).toString('base64'));
 return {api,calls,session};
}
const future=Date.now()/1000+86400;
const detail={event:{id:'event',event_status:'ACTIVE',event_time:'2099-01-01 09:00:00'},branches:[{recipientUserId:'me',branchStatus:'ACTIVE',latestSummary:'updated title'}]};
const record={id:'alarm',eventId:'event',active:true,timeMode:'manual',timestamp:future,title:'old title'};
test('native synchronization updates title while preserving manual time and its mode',async()=>{
 const f=await fixture([record],detail);await f.api.reconcileAlarms();
 const plan=f.calls.find(c=>c.action==='alarm.schedule').data;
 assert.equal(plan.timestamp,future);assert.equal(plan.timeMode,'manual');assert.equal(plan.title,'updated title');
});
test('legacy alarms still follow suggested time and manual mode can be explicitly reset',async()=>{
 const f=await fixture([{...record,timeMode:undefined}],detail);await f.api.reconcileAlarms();
 const plan=f.calls.find(c=>c.action==='alarm.schedule').data;
 assert.equal(plan.timestamp,Date.parse('2099-01-01T01:00:00Z')/1000);assert.equal(plan.timeMode,'suggested');
 await f.api.setAlarm(detail,future);assert.equal(f.calls.at(-1).data.timeMode,'manual');
 await f.api.setAlarm(detail);assert.equal(f.calls.at(-1).data.timeMode,'suggested');
});
test('completed branches cancel even a manual alarm; fired alarms are not resurrected',async()=>{
 const f=await fixture([record],{...detail,branches:[{...detail.branches[0],branchStatus:'COMPLETED'}]});await f.api.reconcileAlarms();
 assert.equal(f.calls.at(-1).action,'alarm.cancel');
 const fired=await fixture([{...record,active:false}],detail);await fired.api.reconcileAlarms();
 assert.equal(fired.calls.some(c=>['alarm.schedule','alarm.cancel'].includes(c.action)),false);
});
test('an account change during event lookup cannot cancel or replace the new account alarm',async()=>{
 const f=await fixture([record],session=>{session.userId='someone';session.revision++;return detail;});
 await f.api.reconcileAlarms();assert.equal(f.calls.some(c=>['alarm.schedule','alarm.cancel'].includes(c.action)),false);
});
test('an in-flight background lookup cannot replace a newer manually edited alarm',async()=>{
 let resolve;
 const f=await fixture([record],()=>new Promise(r=>resolve=r));
 const sync=f.api.reconcileAlarms();await new Promise(r=>setImmediate(r));
 const edited=future+3600;await f.api.setAlarm(detail,edited);
 resolve(detail);await sync;
 const schedules=f.calls.filter(c=>c.action==='alarm.schedule');
 assert.equal(schedules.length,1);assert.equal(schedules[0].data.timestamp,edited);
});
