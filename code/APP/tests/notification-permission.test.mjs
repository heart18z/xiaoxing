import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import {dependency,root} from './loader.mjs';
let serial=0;
async function fixture(handler){
 const calls=[],session={userId:'',visible:true,revision:0,language:'zh-cn'};
 const env={session,reactive:v=>v,persist:()=>{},clearSession:()=>{},secureRead:()=>'',randomHex:()=> 'a'.repeat(32),openFriendRequests:()=>{},openEvent:()=>{},alarmPlan:()=>({}),resetChat:()=>{},BASE_URL:'https://fixture.invalid',BASIC_AUTH:'fixture',TENANT:'test',BUNDLE_ID:'test',PUSH_ENVIRONMENT:'production',post:async()=>({}),reminder:async()=>({}),nativeCall:(action,input,callback)=>{calls.push({action,input:JSON.parse(input)});Promise.resolve().then(()=>handler(action)).then(result=>callback(JSON.stringify(result)));}};
 globalThis.__permissionFixture=env;JSON.parseObject=JSON.parse;
 const stubs=['vue','@/uni_modules/xiaoxing-native','@/store/session.uts','@/api/request.uts','@/config/environment.uts','@/store/navigation.uts','@/core/ui.uts','@/core/alarms.uts','./chat.uts'];
 const bundle=await dependency('esbuild').build({entryPoints:[root+'services/native.uts'],bundle:true,write:false,format:'esm',platform:'node',loader:{'.uts':'ts'},plugins:[{name:'fixture',setup(build){build.onResolve({filter:/.*/},args=>stubs.includes(args.path)?{path:args.path,namespace:'fixture'}:args.path.startsWith('@/')?{path:path.join(root,args.path.slice(2))}:null);build.onLoad({filter:/.*/,namespace:'fixture'},()=>({contents:'export const {'+Object.keys(env).join(',')+'}=globalThis.__permissionFixture;',loader:'js'}));}}]});
 const code='const setInterval=()=>0; const clearInterval=()=>{};\n'+bundle.outputFiles[0].text+`\n// ${serial++}`;
 const api=await import('data:text/javascript;base64,'+Buffer.from(code).toString('base64'));
 return {api,calls,session};
}
test('first launch requests device permission before login, without backend registration',async()=>{
 const {api,calls}=await fixture(action=>({supported:true,permission:action==='push.onboarding'?'authorized':'unknown'}));
 await api.startNative();
 assert.deepEqual(calls.slice(0,2).map(c=>c.action),['initialize','push.onboarding']);
 assert.equal(api.pushState.permission,'authorized');assert.equal(api.needsNotificationPermission(),false);
 assert.equal(calls.some(c=>c.action==='push.request'),false);
});
test('red dot follows OS permission even if backend says registered',async()=>{
 const {api,session}=await fixture(()=>({supported:true,permission:'denied',registered:true}));
 session.userId='1';await api.syncPush();
 assert.equal(api.pushState.registered,true);assert.equal(api.needsNotificationPermission(),true);
});
test('returning from system settings removes attention without requiring a push token',async()=>{
 let permission='denied';const {api}=await fixture(()=>({supported:true,permission}));
 assert.equal(api.needsNotificationPermission(),false);
 await api.refreshNotificationPermission();assert.equal(api.needsNotificationPermission(),true);
 permission='authorized';await api.refreshNotificationPermission();
 assert.equal(api.pushState.registered,false);assert.equal(api.needsNotificationPermission(),false);
});
test('status checks remain silent; explicit requests serialize after an existing permission check',async()=>{
 let resolve;const {api,calls}=await fixture(action=>action==='push.permission'?new Promise(r=>resolve=r):{supported:true,permission:'authorized'});
 const reading=api.refreshNotificationPermission();const requesting=api.requestNotificationPermission();
 await new Promise(r=>setImmediate(r));assert.deepEqual(calls.map(c=>c.action),['push.permission']);
 resolve({supported:true,permission:'denied'});await reading;await requesting;
 assert.deepEqual(calls.map(c=>c.action),['push.permission','push.authorize']);assert.equal(api.needsNotificationPermission(),false);
});
test('web preview does not display a false notification warning',async()=>{
 const {api}=await fixture(()=>({supported:false}));await api.refreshNotificationPermission();
 assert.equal(api.needsNotificationPermission(),false);
});
