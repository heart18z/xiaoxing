import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import {dependency, root} from './loader.mjs';

async function pageFixture() {
  const hooks={}, requests=[], routes=[], tips=[];
  const env={
    ref:value=>({value}), computed:fn=>({get value(){return fn();}}),
    session:{userId:'101'}, requireSession:()=>true, updates:{},
    _s:(row,key,fallback='')=>row[key]??fallback, _n:(row,key)=>Number(row[key]??0),
    _arr:(row,key)=>row[key]??[], obj:(row,key)=>row[key]??{}, message:e=>e.message,
    _t:v=>v, _asset:v=>v, _groupPersonName:p=>p.name, _groupMemberRole:v=>v,
    _groupRoleLabel:v=>v, _groupTagTone:()=>'', groupCanManage:r=>['OWNER','ADMIN'].includes(r),
    groupAvatars:[], toggleGroupSelection:()=>[], toast:v=>tips.push(v), confirm:async()=>true,
    reminder:(path)=>new Promise((resolve,reject)=>requests.push({path,resolve,reject})),
    getCurrentPages:()=>[{route:'pages/me/index'},{route:'pages/group-detail/index'}],
    uni:{navigateBack:options=>routes.push(options),switchTab:options=>routes.push(options)},
    onLoad:fn=>hooks.load=fn,onShow:fn=>hooks.show=fn,onUnload:fn=>hooks.unload=fn,
    onBeforeUnmount:fn=>hooks.beforeUnmount=fn,onBackPress:()=>{},
  };
  const file=fs.readFileSync(root+'pages/group-detail/index.uvue','utf8');
  const script=file.match(/<script setup lang="uts">([\s\S]*?)<\/script>/)[1]
    .replace(/^import[\s\S]*?from ['"][^'"]+['"];\s*/gm,'');
  const {code}=await dependency('esbuild').transform(script+`
    globalThis.api={load,mutate,openInvite,goBack,group,friends,inviting,loading,saving,formError};`,{loader:'ts'});
  vm.runInNewContext(code,env);hooks.load({id:'1001'});
  return {...env.api,hooks,requests,routes,tips,env};
}

test('return rejects late detail and mutation responses and ignores repeated back taps',async()=>{
  const p=await pageFixture();
  const reading=p.load(),saving=p.mutate('admins',{userIds:[]});
  p.goBack();p.goBack();assert.equal(p.routes.length,1);
  p.requests[0].resolve({value:{id:'1001',name:'late detail'}});
  p.requests[1].resolve({value:{id:'1001',name:'late mutation'}});
  await reading;assert.equal(await saving,false);
  assert.equal(p.group.value.id,undefined);assert.equal(p.tips.length,0);
  assert.equal(await p.mutate('dissolve',{}),false);assert.equal(p.requests.length,2);
});

test('unloaded group page ignores invitation results and error toasts',async()=>{
  const p=await pageFixture();p.group.value={id:'1001',myRole:'OWNER'};
  const invite=p.openInvite(),saving=p.mutate('member/tags',{});
  p.hooks.unload();p.requests[0].resolve({value:[{id:'202'}]});
  p.requests[1].reject(new Error('late error'));
  await invite;assert.equal(await saving,false);
  assert.equal(p.friends.value.length,0);assert.equal(p.formError.value,'');assert.equal(p.tips.length,0);
});

test('navigation failure permits retry and reloads the still-open page',async()=>{
  const p=await pageFixture();p.goBack();p.routes[0].fail();
  assert.equal(p.requests[0].path,'groups/detail');
  p.requests[0].resolve({value:{id:'1001',name:'reloaded'}});
  await new Promise(resolve=>setImmediate(resolve));
  assert.equal(p.group.value.name,'reloaded');p.goBack();assert.equal(p.routes.length,2);
});

test('native swipe teardown invalidates callbacks without the custom back button',async()=>{
  const p=await pageFixture();const reading=p.load();
  p.hooks.beforeUnmount();p.requests[0].resolve({value:{id:'1001',name:'late swipe result'}});
  await reading;assert.equal(p.group.value.id,undefined);assert.equal(p.routes.length,0);
  p.hooks.unload();p.goBack();assert.equal(p.routes.length,0);
});

test('normal load and save still update an open group page',async()=>{
  const p=await pageFixture();const reading=p.load();
  p.requests[0].resolve({value:{id:'1001',name:'before'}});await reading;
  const saving=p.mutate('save',{});p.requests[1].resolve({value:{id:'1001',name:'after'}});
  assert.equal(await saving,true);assert.equal(p.group.value.name,'after');assert.equal(p.saving.value,false);
});

test('group navigation suppresses double taps and unlocks after completion or failure',async()=>{
  const routes=[];let pages=[{route:'pages/me/index'}];
  const env={exports:{},getCurrentPages:()=>pages,uni:{navigateTo:options=>routes.push(options)}};
  const {code}=await dependency('esbuild').transform(fs.readFileSync(root+'core/group-navigation.uts','utf8'),{loader:'ts',format:'cjs'});
  const module={exports:env.exports};env.module=module;vm.runInNewContext(code,env);
  const {openGroup}=module.exports;
  openGroup('1001');openGroup('1001');assert.equal(routes.length,1);
  routes[0].complete();pages.push({route:'pages/group-detail/index'});openGroup('1001');assert.equal(routes.length,1);
  pages.pop();openGroup('1001');assert.equal(routes.length,2);
  routes[1].complete();openGroup('bad-id');assert.equal(routes.length,2);
  openGroup('1002');assert.equal(routes.length,3);
});
