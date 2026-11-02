import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import {dependency,root} from './loader.mjs';
async function fixture(handler) {
  const calls=[],session={userId:'1',token:'live',revision:0};
  const source=fs.readFileSync(root+'services/native.uts','utf8');
  const script=source.slice(source.indexOf('export async function logout(')).replace('export async','async');
  const {code}=await dependency('esbuild').transform(script,{loader:'ts'});
  const env={session,alarmState:{ringing:false},pushState:{registered:true},message:e=>e.message,
    post:async(path,...args)=>{calls.push(['server',path,session.token]);await handler(session);},
    queueRevoke:v=>calls.push(['queue',v]),read:()=> 'binding',persist:()=>{},
    clearSession:()=>{calls.push(['clear']);session.token='';session.userId='';session.revision++;},
    resetChat:()=>calls.push(['reset']),callNative:async(action)=>calls.push([action]),
    enqueue:async(fn)=>fn(),revokePending:async()=>{},uni:{reLaunch:()=>calls.push(['login'])}};
  vm.runInNewContext(code+'\nglobalThis.logout=logout;',env);return {logout:env.logout,calls,session};
}
test('logout revokes the authenticated server session before deleting local credentials',async()=>{
  let resolve;const p=await fixture(()=>new Promise(r=>resolve=r));const pending=p.logout();
  assert.equal(p.calls.length,1);assert.equal(p.calls[0][1],'/blade-auth/oauth/logout');assert.equal(p.session.token,'live');
  resolve();await pending;
  assert.equal(p.calls[0][2],'live');assert.equal(p.session.token,'');assert.equal(p.calls.at(-1)[0],'login');
});
test('failed server logout remains retryable instead of claiming a successful local-only logout',async()=>{
  let fail=true;const p=await fixture(()=>{if(fail)throw Error('offline');});
  await assert.rejects(p.logout(),/offline/);assert.equal(p.session.token,'live');assert.equal(p.calls.length,1);
  fail=false;await p.logout();assert.equal(p.session.token,'');
});
test('late logout cannot clear a new account or a new session for the same account',async()=>{
  for(const next of ['2','1']) {
    const p=await fixture(s=>{s.userId=next;s.token='new';s.revision++;});
    await assert.rejects(p.logout(),/账号已变化/);assert.equal(p.session.token,'new');assert.equal(p.calls.length,1);
  }
});
test('account closure that already revoked authentication still performs native cleanup',async()=>{
  const p=await fixture(()=>{throw Error('must not call server twice');});await p.logout(true);
  assert.equal(p.calls.some(c=>c[0]==='server'),false);assert.equal(p.session.token,'');assert.equal(p.calls.at(-1)[0],'login');
});
