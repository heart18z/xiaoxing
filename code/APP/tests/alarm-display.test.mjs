import test from 'node:test';
import assert from 'node:assert/strict';
import { uts } from './loader.mjs';
const { alarmTime } = await uts('core/alarms.uts');

test('display uses the saved AlarmKit Unix seconds and Beijing time', () => {
  assert.equal(alarmTime({ timestamp: Date.parse('2026-09-22T01:05:00Z') / 1000 }), '2026-09-22 09:05');
  assert.equal(alarmTime({ timestamp: Date.parse('2026-09-21T16:30:00Z') / 1000 }), '2026-09-22 00:30');
});

test('missing or invalid native alarm times never display an invented date', () => {
  for (const timestamp of [0, -1, Infinity, NaN]) assert.equal(alarmTime({ timestamp }), '');
  assert.equal(alarmTime({}), '');
});
