import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {dependency,root} from './loader.mjs';

test('collapsed and hidden reasoning unmount native text without losing reopen content',()=>{
 const source=fs.readFileSync(root+'components/ThinkingPanel.uvue','utf8');
 const template=dependency('@vue/compiler-sfc').parse(source).descriptor.template.content;
 const {code}=dependency('@vue/compiler-dom').compile(template,{mode:'function'});
 const vue=dependency('vue');
 const render=new Function('Vue',code)({...vue,resolveComponent:name=>name});
 const text='完整的历史思考文字'.repeat(5000);
 function nodes(open,visible){
  const ctx={panelId:'memory-fixture',open,windowOpen:open,visible,streaming:true,pulse:false,shine:0,displayed:text,following:true,t:s=>s,emit:()=>{}};
  const found=[];
  function walk(node){if(node==null||typeof node!=='object')return;found.push(node);if(Array.isArray(node.children))node.children.forEach(walk)}
  walk(render(ctx,[]));return found;
 }
 for(const [open,visible] of [[false,true],[true,false],[false,false]]){
  const tree=nodes(open,visible);
  assert.equal(tree.filter(n=>n.type==='scroll-view').length,0);
  assert.equal(tree.some(n=>n.children===text),false);
 }
 const reopened=nodes(true,true);
 assert.equal(reopened.filter(n=>n.type==='scroll-view').length,1);
 assert.equal(reopened.some(n=>n.children===text),true);
});
