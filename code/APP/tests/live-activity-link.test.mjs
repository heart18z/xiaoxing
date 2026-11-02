import test from 'node:test';
import assert from 'node:assert/strict';
import {uts} from './loader.mjs';
const {liveActivityEvent} = await uts('core/live-activity-link.uts');

test('live activity link only routes the currently signed-in recipient', () => {
  assert.equal(liveActivityEvent('xiaoxing://live?owner=12&eventId=9007199254740993','12'),'9007199254740993');
  assert.equal(liveActivityEvent('xiaoxing://live?owner=12&eventId=33','13'),'');
  assert.equal(liveActivityEvent('xiaoxing://live?owner=12&eventId=33',''),'');
});
test('malformed and foreign links cannot inject navigation', () => {
  for (const url of ['https://live?owner=12&eventId=33','xiaoxing://live?owner=12&eventId=../login',
    'xiaoxing://live?owner=12&eventId=%E0','xiaoxing://live?owner=12&eventId=abc']) {
    assert.equal(liveActivityEvent(url,'12'),'');
  }
});
