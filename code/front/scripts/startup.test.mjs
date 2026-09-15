import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {runInNewContext} from 'node:vm';
const source=readFileSync(new URL('../public/startup.js',import.meta.url),'utf8');
function fixture(enabled=true){
  const timers=new Map(),listeners=new Map();let next=0;
  const panel={style:{},removed:false,attrs:{},remove(){this.removed=true;},setAttribute(k,v){this.attrs[k]=v;}};
  const message={textContent:''},retry={hidden:true};
  const document={documentElement:{hasAttribute:()=>enabled,removeAttribute(){}},getElementById:id=>id==='xiaoxing-startup'?panel:id.endsWith('message')?message:retry};
  const window={addEventListener:(n,f)=>listeners.set(n,f),removeEventListener:n=>listeners.delete(n)};
  let reloads=0;
  runInNewContext(source,{window,document,location:{reload:()=>reloads++},setTimeout:(fn,delay)=>{timers.set(++next,{fn,delay});return next;},clearTimeout:id=>timers.delete(id)});
  return{panel,message,retry,window,timers,listeners,reloads:()=>reloads};
}
test('startup stays branded during slow loading and exposes retry without claiming failure',()=>{
  const f=fixture();[...f.timers.values()].find(t=>t.delay===15000).fn();
  assert.match(f.message.textContent,/稍久/);assert.equal(f.retry.hidden,false);f.retry.onclick();assert.equal(f.reloads(),1);
});
test('module loading failure stays on branded cover; readiness removes cover and listeners',()=>{
  const f=fixture();f.listeners.get('error')({target:{tagName:'SCRIPT',type:'module'}});
  assert.match(f.message.textContent,/检查网络/);assert.equal(f.retry.hidden,false);
  f.window.xiaoxingStartup.ready();assert.equal(f.panel.style.opacity,'0');assert.equal(f.listeners.size,0);
  [...f.timers.values()].find(t=>t.delay===260).fn();assert.equal(f.panel.removed,true);
  f.window.xiaoxingStartup.fail();assert.equal(f.panel.removed,true);
});
test('admin startup is untouched',()=>{const f=fixture(false);assert.equal(f.panel.removed,true);assert.equal(f.window.xiaoxingStartup,undefined);});
