import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import { dependency, root } from './loader.mjs';

test('native startup bar moves before the minimum splash duration and releases its timer', async () => {
  const source = fs.readFileSync(root + 'components/StartupMask.uvue', 'utf8');
  const script = source.match(/<script setup lang="uts">([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*;\r?\n/gm, '');
  const {code} = await dependency('esbuild').transform(script, {loader:'ts'});
  let now=0, mounted, unmounted, changed, nextId=0;
  const timers=new Map();
  const startup={visible:true,leaving:false};
  const context={startup,Date:{now:()=>now},ref:value=>({value}),
    onMounted:fn=>mounted=fn,onUnmounted:fn=>unmounted=fn,watch:(_,fn)=>changed=fn,
    setInterval:fn=>{timers.set(++nextId,fn);return nextId;},clearInterval:id=>timers.delete(id)};
  vm.createContext(context);
  vm.runInContext(code+'\nglobalThis.readPosition = () => position.value;',context);
  mounted();
  assert.equal(timers.size,1);
  now=100; for(const tick of timers.values())tick();
  const first=context.readPosition();
  assert.ok(first>0, 'Must move within 100 ms, not wait until the splash closes');
  now=300; for(const tick of timers.values())tick();
  assert.ok(context.readPosition()>first);
  startup.visible=false;changed(false);
  assert.equal(timers.size,0);
  startup.visible=true;changed(true);
  assert.equal(timers.size,1);
  unmounted();
  assert.equal(timers.size,0);
});
