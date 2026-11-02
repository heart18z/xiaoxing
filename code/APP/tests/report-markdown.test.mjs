import test from 'node:test';
import assert from 'node:assert/strict';
import { uts } from './loader.mjs';
const { reportBlocks } = await uts('core/report-markdown.uts');
test('report headings, lists and inline emphasis preserve meaningful text',()=>{
  const rows=reportBlocks('### 本周重点\n- **已完成**：材料提交。\n1. **下一步** 核对时间。');
  assert.equal(rows[0].heading,true);
  assert.equal(rows[1].bullet,true);
  assert.deepEqual(rows[1].spans,[{text:'已完成',bold:true},{text:'：材料提交。',bold:false}]);
  assert.equal(rows[2].bullet,true);
});
test('legacy prose, HTML-looking content and unmatched markup remain literal text',()=>{
  const value='会议继续推进。<img src=x onerror=alert(1)> **未闭合';
  assert.deepEqual(reportBlocks(value)[0].spans,[{text:value,bold:false}]);
  assert.deepEqual(reportBlocks('\n\r\n'),[]);
});
