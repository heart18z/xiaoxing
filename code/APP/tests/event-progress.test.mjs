import test from 'node:test';
import assert from 'node:assert/strict';
import { uts } from './loader.mjs';
const { latestProgress } = await uts('core/events.uts');

test('event progress falls back for empty API values as the Web list does', () => {
  for (const row of [{}, {latestFact: null}, {latestFact: ''}, {latestFact: '   '}]) {
    assert.equal(latestProgress(row), '暂无反馈，等待下一次评估');
  }
});
test('event progress shows the server latestFact rather than inventing progress', () => {
  assert.equal(latestProgress({latestFact: '已完成第一版标书，等待复核'}), '已完成第一版标书，等待复核');
});
