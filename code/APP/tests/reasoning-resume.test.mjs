import test from 'node:test';
import assert from 'node:assert/strict';
import {uts} from './loader.mjs';
const {mergeReasoningSnapshot}=await uts('core/stream.uts');
test('reconnect snapshots and delayed status responses cannot rewind visible reasoning',()=>{
  let text='正在整理时间';
  for(const stale of ['', '正在', '正在整理时间']) {
    text=mergeReasoningSnapshot(text,stale);
    assert.equal(text,'正在整理时间');
  }
  text=mergeReasoningSnapshot(text,'正在整理时间，明天上午九点🔔');
  assert.equal(text,'正在整理时间，明天上午九点🔔');
  assert.equal(mergeReasoningSnapshot(text,'正在整理时间，明天'),text);
});
test('a new job starts independently and cumulative text survives fragmented streams',()=>{
  const complete='中文与 emoji 🔔 的连续思考';
  let text='';
  for(let i=1;i<=complete.length;i++) {
    text=mergeReasoningSnapshot(text,complete.slice(0,i));
    text=mergeReasoningSnapshot(text,complete.slice(0,Math.max(0,i-3)));
    assert.equal(text,complete.slice(0,i));
  }
  assert.equal(mergeReasoningSnapshot('', '新任务'), '新任务');
});
