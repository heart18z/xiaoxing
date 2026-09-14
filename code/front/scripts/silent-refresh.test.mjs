import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

test('silent refresh avoids overlap, ignores stale hidden results, and stops on disposal',async()=>{
  const hooks={},timers=new Map();let id=0,calls=0,finish;
  const document=new EventTarget();document.hidden=false;
  const window=new EventTarget();
  const fixture={document,window,setTimeout:cb=>{timers.set(++id,cb);return id;},clearTimeout:id=>timers.delete(id)};
  for(const key of ['onMounted','onActivated','onDeactivated','onBeforeUnmount'])fixture[key]=cb=>{hooks[key]=cb;};
  globalThis.__refreshFixture=fixture;
  try{
    let source=readFileSync('src/page/smart-reminder/useSilentRefresh.js','utf8');
    source=source.replace(/^import[^\n]+\n/, 'const {onMounted,onActivated,onDeactivated,onBeforeUnmount,document,window,setTimeout,clearTimeout}=globalThis.__refreshFixture;\n');
    const {useSilentRefresh}=await import('data:text/javascript;base64,'+Buffer.from(source).toString('base64'));
    const applied=[];
    useSilentRefresh(current=>{calls++;return new Promise(resolve=>{finish=()=>{applied.push(current());resolve();};});});
    hooks.onMounted();assert.equal(timers.size,1);
    const tick=[...timers.values()][0]();assert.equal(calls,1);assert.equal(timers.size,0);
    window.dispatchEvent(new Event('smart-reminder:push-refresh'));assert.equal(calls,1);
    document.hidden=true;document.dispatchEvent(new Event('visibilitychange'));finish();await tick;
    assert.deepEqual(applied,[false]);assert.equal(timers.size,0);
    document.hidden=false;document.dispatchEvent(new Event('visibilitychange'));assert.equal(calls,2);
    hooks.onBeforeUnmount();finish();await Promise.resolve();await Promise.resolve();
    assert.deepEqual(applied,[false,false]);assert.equal(timers.size,0);
    window.dispatchEvent(new Event('online'));assert.equal(calls,2);
  }finally{delete globalThis.__refreshFixture;}
});
