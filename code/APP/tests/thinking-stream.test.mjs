import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {dependency,root} from './loader.mjs';
test('continuous reasoning snapshots do not starve visible text before completion',async()=>{
 const vue=dependency('vue'),cleanup=[];
 const props=vue.reactive({content:'',open:true,streaming:true,panelId:'fixture'});
 let script=fs.readFileSync(root+'components/ThinkingPanel.uvue','utf8').match(/<script setup lang="uts">([\s\S]*?)<\/script>/)[1];
 script=script.replace(/^import .*;\r?$/gm,'');
 const {code}=await dependency('esbuild').transform(script,{loader:'ts',format:'cjs'});
 const result=new Function('ref','watch','nextTick','onMounted','onUnmounted','defineProps','defineEmits','_t',code+'\nreturn {displayed};')(
  vue.ref,vue.watch,vue.nextTick,fn=>fn(),fn=>cleanup.push(fn),()=>props,()=>()=>{},s=>s);
 const feeding=setInterval(()=>{props.content+='正在核对时间和接收人。';},8);
 try {
  await new Promise(resolve=>setTimeout(resolve,180));
  assert.ok(result.displayed.value.length>0,'Text must appear while snapshots are still arriving');
  assert.ok(props.content.startsWith(result.displayed.value));
  clearInterval(feeding);props.streaming=false;await vue.nextTick();assert.equal(result.displayed.value,props.content);
 }finally{clearInterval(feeding);cleanup.forEach(fn=>fn());}
});
