import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

test('double-tap protection covers nested mobile scroll targets and native teleports', () => {
  const native = readFileSync('src/native/native.css', 'utf8');
  const mobile = readFileSync('src/page/smart-reminder/mobile.css', 'utf8');
  assert.match(native, /html\.native-app,html\.native-app \*\s*\{touch-action:manipulation\}/);
  assert.match(mobile, /\.sr-shell,\.sr-shell \*\s*\{\s*touch-action:manipulation;/);
  assert.doesNotMatch(native + mobile, /touch-action:\s*none/);
  const entry = readFileSync('native.html', 'utf8');
  assert.doesNotMatch(entry, /user-scalable\s*=\s*no|maximum-scale\s*=\s*1/);
  assert.match(native, /html\.native-app input[^\n]*font-size:16px!important/);
});
