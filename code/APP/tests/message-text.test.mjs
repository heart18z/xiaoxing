import test from 'node:test';
import assert from 'node:assert/strict';
import {uts} from './loader.mjs';
const {messageBlocks}=await uts('core/message-text.uts');
test('native text keeps ordinary, partial streamed and code content visible without HTML rendering',()=>{
  const blocks=messageBlocks('# 标题\n- **提醒**\n未完成的 **流式\n```html\n<img src=x onerror=alert(1)>\n**literal**\n```');
  assert.equal(blocks[0].kind,'heading');
  assert.equal(blocks[1].text,'• 提醒');
  assert.equal(blocks[2].text,'未完成的 **流式');
  assert.equal(blocks[3].text,'<img src=x onerror=alert(1)>');
  assert.equal(blocks[4].text,'**literal**');
});
test('large messages use bounded native blocks without losing text or splitting emoji',()=>{
  const content='字'.repeat(899)+'😀'+'消息'.repeat(3000);
  const blocks=messageBlocks(content);
  assert.ok(blocks.length>5);
  assert.ok(blocks.every(b=>b.text.length<=900));
  assert.equal(blocks.map(b=>b.text).join(''),content);
  assert.ok(blocks.every(b=>!/[\ud800-\udbff]$/.test(b.text)));
});

test('summary headings and inline emphasis retain native font information',()=>{
  const blocks=messageBlocks('### 本周待办\n- **准备演示**：**11月8日**前确认时间。\n1. __确认场地__，用 `编号` 查询。');
  assert.ok(blocks[0].spans.every(s=>s.bold));
  assert.equal(blocks[1].kind,'list');
  assert.deepEqual(blocks[1].spans.filter(s=>s.bold).map(s=>s.text),['准备演示','11月8日']);
  assert.equal(blocks[2].spans.find(s=>s.text==='编号').code,true);
  assert.ok(blocks[2].spans.find(s=>s.text==='确认场地').bold);
});

test('long emphasized spans remain bold across native layout boundaries',()=>{
  const text='字'.repeat(899)+'😀'+'继续'.repeat(1000);
  const blocks=messageBlocks('**'+text+'**');
  assert.equal(blocks.map(b=>b.text).join(''),text);
  assert.ok(blocks.every(b=>b.text.length<=900 && b.spans.every(s=>s.bold)));
  assert.ok(blocks.every(b=>!/[\ud800-\udbff]$/.test(b.text)));
});
