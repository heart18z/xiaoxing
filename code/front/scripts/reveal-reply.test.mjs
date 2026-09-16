import test from 'node:test';
import assert from 'node:assert/strict';
import {revealReply} from '../src/page/smart-reminder/revealReply.mjs';
test('committed reply appears progressively without splitting emoji',async()=>{
  const values=[];await revealReply('你好🙂，我在。',value=>values.push(value));
  assert.ok(values.length>1);assert.equal(values.at(-1),'你好🙂，我在。');
  for(const value of values)assert.ok(!/[\uD800-\uDBFF]$/.test(value));
});
test('reduced motion is immediate and cancellation prevents late text updates',async()=>{
  const values=[];await revealReply('已保存',v=>values.push(v),{instant:true});assert.deepEqual(values,['已保存']);
  const controller=new AbortController(),partial=[];
  const done=revealReply('正在展示这段已完成的回复',v=>partial.push(v),{signal:controller.signal});controller.abort();await done;
  const count=partial.length;await new Promise(r=>setTimeout(r,40));assert.equal(partial.length,count);assert.ok(count<=1);
});
