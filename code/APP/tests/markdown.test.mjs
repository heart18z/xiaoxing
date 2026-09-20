import test from 'node:test';
import assert from 'node:assert/strict';
import { uts } from './loader.mjs';
const {nativeMarkdown}=await uts('core/markdown.uts');
test('native reply formatting preserves code literally and escapes untrusted HTML',()=>{
  const html=nativeMarkdown('# 标题\n- **提醒**\n```html\n<img src=x onerror=alert(1)>\n**literal**\n```');
  assert.match(html,/<strong>标题<\/strong>/);
  assert.match(html,/• <strong>提醒<\/strong>/);
  assert.match(html,/&lt;img/);
  assert.ok(!html.includes('<img'));
  assert.match(html,/\*\*literal\*\*/);
  assert.match(html,/<\/pre>/);
});
