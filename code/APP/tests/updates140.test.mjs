import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import {dependency,root} from './loader.mjs';
let serial=0;
const release={id:'900',versionCode:141,versionName:'1.0.141',fileSize:10485760,sha256:'a'.repeat(64),notes:'更新说明',forceUpdate:false,required:false};
async function fixture(options={}){
 const calls=[], session={userId:'1'};
 const env={session,reactive:v=>v,BASE_URL:'https://fixture.invalid',headers:()=>({}),reminder:async(name,body,params)=>{calls.push({name,params});return {value:await (options.release?.()??{...release})}},callNative:async(action,input)=>{calls.push({action,input});if(action==='update.info')return options.local??{versionName:'1.0.140',versionCode:140};if(action==='update.status')return options.status??{progress:42,downloaded:4404019};if(action==='update.install')return options.install?options.install(input):{installerOpened:true};return {};}};
 globalThis.__updateFixture=env;JSON.parseObject=JSON.parse;globalThis.uni={hideKeyboard(){}};
 const modules=['vue','@/api/request.uts','@/config/environment.uts','@/services/native.uts','@/store/session.uts'];
 const bundle=await dependency('esbuild').build({entryPoints:[root+'services/updates.uts'],bundle:true,write:false,format:'esm',platform:'node',loader:{'.uts':'ts'},plugins:[{name:'fixture',setup(build){build.onResolve({filter:/.*/},args=>modules.includes(args.path)?{path:args.path,namespace:'fixture'}:args.path.startsWith('@/')?{path:path.join(root,args.path.slice(2))}:null);build.onLoad({filter:/.*/,namespace:'fixture'},()=>({contents:'export const {'+Object.keys(env).join(',')+'}=globalThis.__updateFixture;',loader:'js'}));}}]});
 const api=await import('data:text/javascript;base64,'+Buffer.from(bundle.outputFiles[0].text+`\n//fixture ${serial++}`).toString('base64'));
 return {api,calls,session};
}
test('optional launch update can be postponed and is not repeated on each foreground check',async()=>{
 const {api,calls}=await fixture();await api.checkUpdates(true);
 assert.equal(api.updates.prompt,true);assert.equal(api.updates.required,false);
 assert.equal(calls.find(c=>c.name).params.currentVersionCode,140);
 api.dismissUpdate();await api.checkUpdates(true);assert.equal(api.updates.prompt,false);
 api.resetUpdateSession();await api.checkUpdates(true);assert.equal(api.updates.prompt,true);
});
test('mandatory release cannot be dismissed; installed version clears the gate',async()=>{
 let latest={...release,required:true};const {api}=await fixture({release:()=>latest});
 await api.checkUpdates(true);api.dismissUpdate();assert.equal(api.updates.prompt,true);assert.equal(api.updates.required,true);
 latest={...release,versionCode:140,required:false};await api.checkUpdates(true);
 assert.equal(api.updates.available,false);assert.equal(api.updates.required,false);assert.equal(api.updates.prompt,false);
});
test('a subsequent optional release clears the previous mandatory gate',async()=>{
 let latest={...release,forceUpdate:true,required:true};const {api}=await fixture({release:()=>latest});
 await api.checkUpdates(true);assert.equal(api.updates.required,true);
 latest={...release,versionCode:142,versionName:'1.0.142',forceUpdate:false,required:false};
 await api.checkUpdates(true);assert.equal(api.updates.required,false);api.dismissUpdate();assert.equal(api.updates.prompt,false);
});
test('late release response from a previous account cannot display a mandatory prompt',async()=>{
 let resolve;const {api,session}=await fixture({release:()=>new Promise(r=>resolve=r)});
 const waiting=api.checkUpdates(true);await new Promise(r=>setImmediate(r));session.userId='2';api.resetUpdateSession();
 resolve({...release,required:true});await waiting;assert.equal(api.updates.prompt,false);assert.equal(api.updates.required,false);
});
test('real download progress remains visible and mandatory gate survives opening or cancelling installer',async()=>{
 let finish;const {api}=await fixture({release:()=>({...release,required:true}),install:()=>new Promise(r=>finish=r)});
 await api.checkUpdates(true);const installing=api.installUpdate();await new Promise(r=>setTimeout(r,310));
 assert.equal(api.updates.installing,true);assert.equal(api.updates.progress,42);assert.equal(api.updates.downloaded,4404019);
 finish({installerOpened:true});await installing;
 assert.equal(api.updates.progress,100);assert.equal(api.updates.installing,false);api.dismissUpdate();assert.equal(api.updates.prompt,true);
 await api.checkUpdates(true);assert.equal(api.updates.required,true);
});
test('permission request and failed download both permit retry without dismissing required update',async()=>{
 let attempts=0;const {api}=await fixture({release:()=>({...release,required:true}),install:()=>{if(++attempts===1)return {permissionRequired:true};throw new Error('network failed');}});
 await api.checkUpdates(true);await api.installUpdate();assert.equal(api.updates.progress,0);assert.equal(api.updates.installing,false);
 await api.installUpdate();assert.match(api.updates.feedback,/network failed/);assert.equal(api.updates.required,true);assert.equal(api.updates.prompt,true);assert.equal(api.updates.installing,false);
});
