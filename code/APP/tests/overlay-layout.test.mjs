import test from 'node:test';
import assert from 'node:assert/strict';
import { uts } from './loader.mjs';
const { dropdownRect } = await uts('core/overlayLayout.uts');

test('dropdown opens below with both language choices fully visible', () => {
  const anchor = {left: 33, top: 284, width: 336, height: 46};
  const panel = dropdownRect(anchor, 402, 874, 62, 34, 2);
  assert.equal(panel.top, 338);
  assert.equal(panel.height, 110);
  assert.equal(panel.width, anchor.width);
  assert.equal(panel.left, anchor.left);
});
test('dropdown flips above near bottom and constrains long lists to safe viewport', () => {
  for (const count of [2, 20]) {
    const panel = dropdownRect({left: 33, top: 720, width: 336, height: 46}, 402, 874, 62, 34, count);
    assert.ok(panel.top >= 74);
    assert.equal(panel.top + panel.height, 712);
    assert.ok(panel.height <= 274);
    assert.ok(panel.left + panel.width <= 390);
  }
});
test('dropdown preserves scrollable options on narrow and short screens', () => {
  const panel = dropdownRect({left: 5, top: 130, width: 360, height: 46}, 320, 360, 24, 20, 30);
  assert.ok(panel.width <= 296);
  assert.ok(panel.height > 48);
  assert.ok(panel.top + panel.height <= 328);
});
