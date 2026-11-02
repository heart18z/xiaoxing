import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import { dependency, root } from './loader.mjs';

async function loginPage(rejectFirst = false) {
  const source = fs.readFileSync(root + 'pages/login/index.uvue', 'utf8');
  const script = source.match(/<script setup lang="uts">([\s\S]*?)<\/script>/)[1].replace(/^import .*;\r?\n/gm, '');
  const { code } = await dependency('esbuild').transform(script, { loader: 'ts' });
  const calls = [], storage = [];
  class ApiError extends Error { constructor() { super('duplicate login'); this.status = 601; } }
  const context = {
    ref: value => ({value}), computed: getter => ({get value(){ return getter(); }}),
    nextTick: async () => {}, onUnmounted() {}, onHide() {}, onResize() {}, onReady() {}, onBackPress() {},
    setTimeout: fn => {queueMicrotask(fn);return 1;}, clearTimeout() {},
    _t: s => s, toast() {}, LEGAL_VERSION:'2026-10-30', session:{language:'zh-cn'},
    uni:{getWindowInfo:()=>({statusBarHeight:20,windowHeight:844,windowWidth:390}),getStorageSync:()=>'',
      setStorageSync:(...args)=>storage.push(args), reLaunch:o=>{calls.push(['navigate']);o.success();}},
    login:async (...args)=>{calls.push(['password',...args]);if(rejectFirst && !args[4])throw new ApiError();},
    emailLogin:async (...args)=>{calls.push(['email',...args]);if(rejectFirst && !args[2])throw new ApiError();},
    syncPush:async()=>{}, ApiError, message:e=>e.message, Date, Promise,
  };
  vm.createContext(context); vm.runInContext(code, context);
  return {calls,storage,run:expression=>vm.runInContext(expression,context)};
}

for (const email of [false,true]) {
  test(`${email?'email':'password'} login never sends credentials before agreement; confirm continues once`,async()=>{
    const p=await loginPage();
    p.run(`account.value='tester@example.com';password.value='test-password';emailCode.value='123456';codeMode.value=${email}`);
    await p.run('submit()');
    assert.equal(p.run('consentOpen.value'),true);assert.equal(p.calls.length,0);assert.equal(p.storage.length,0);
    p.run('consentOpen.value=false'); // Cancel leaves form untouched and sends nothing.
    assert.equal(p.run('accepted.value'),false);assert.equal(p.calls.length,0);
    await p.run('submit()');p.run('agreeAndLogin();agreeAndLogin()');
    await new Promise(resolve=>setImmediate(resolve));
    assert.equal(p.calls.filter(c=>c[0]===(email?'email':'password')).length,1);
    assert.equal(p.run('accepted.value'),true);assert.equal(p.calls.at(-1)[0],'navigate');
  });
  test(`${email?'email':'password'} policy consent never bypasses another-device confirmation`,async()=>{
    const p=await loginPage(true);
    p.run(`account.value='tester@example.com';password.value='test-password';emailCode.value='123456';codeMode.value=${email};accepted.value=true`);
    await p.run('submit()');
    assert.equal(p.run('confirmOpen.value'),true);assert.equal(p.calls.length,1);
    await p.run('continueLogin()');
    assert.equal(p.calls[1].at(-1),true);assert.equal(p.calls.at(-1)[0],'navigate');
  });
}
test('invalid fields do not open a policy prompt or submit a login',async()=>{
  const p=await loginPage();await p.run('submit()');
  assert.equal(p.run('consentOpen.value'),false);assert.equal(p.calls.length,0);
});
