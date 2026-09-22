import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import {dependency,root} from './loader.mjs';

async function composer() {
  const source=fs.readFileSync(root+'pages/chat/index.uvue','utf8');
  assert.match(source,/:value="inputValue"/);
  assert.match(source,/@input="composerInput"/);
  assert.doesNotMatch(source,/v-model="draft"/);
  const handlers=source.slice(source.indexOf('function composerInput('),source.indexOf('function releaseAttachment('))
    .replace(/\/\/ #ifdef WEB[\s\S]*?\/\/ #endif/g,'');
  const {code}=await dependency('esbuild').transform(handlers,{loader:'ts'});
  let writes=0, text='',marked='',mounted=true;
  const element={set value(value){writes++;text=value;marked='';}};
  const state={draft:{value:''},inputValue:{value:''},uni:{getElementById:()=>mounted?element:null}};
  vm.createContext(state);vm.runInContext(code,state);
  return {state,type(value){text=value;state.composerInput({detail:{value}});},
    compose(value){marked=value;}, read:()=>({writes,text,marked}),mount:value=>{mounted=value;}};
}

test('Pinyin stays owned by the native editor across input events and parent refreshes',async()=>{
  const c=await composer();
  c.type('王处汇报');c.compose('ding zai');
  c.type('王处汇报ding zai');
  for(let refresh=0;refresh<20;refresh++)assert.equal(c.state.inputValue.value,'');
  assert.equal(c.state.draft.value,'王处汇报ding zai');
  assert.deepEqual(c.read(),{writes:0,text:'王处汇报ding zai',marked:'ding zai'});
  c.type('王处汇报定在');
  assert.equal(c.state.draft.value,'王处汇报定在');
  assert.equal(c.read().writes,0);
});

test('explicit clears work repeatedly without remounting; voice and failed-send restore remain available',async()=>{
  const c=await composer();
  c.type('第一条');c.state.setDraft('');assert.equal(c.read().text,'');
  c.type('第二条');c.state.setDraft('');assert.equal(c.read().text,'');
  assert.equal(c.read().writes,2);
  c.state.setDraft('发送失败的草稿');assert.equal(c.read().text,'发送失败的草稿');
  c.mount(false);c.state.setDraft('语音识别结果');
  assert.equal(c.state.inputValue.value,'语音识别结果');
  assert.equal(c.state.draft.value,'语音识别结果');
});
