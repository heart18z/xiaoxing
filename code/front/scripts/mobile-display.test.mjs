import test from 'node:test';
import assert from 'node:assert/strict';
import { displayText } from '../src/page/smart-reminder/displayText.mjs';

test('ISO dates in event summaries display a space without timezone conversion', () => {
  assert.equal(displayText('时间：2026-09-13T15:00，截止：2026-09-13T16:00:00+08:00'), '时间：2026-09-13 15:00，截止：2026-09-13 16:00:00+08:00');
  assert.equal(displayText('TASK-T-15 / 2026-09-13 15:00'), 'TASK-T-15 / 2026-09-13 15:00');
  assert.equal(displayText(null), '');
});
