import test from 'node:test';
import assert from 'node:assert/strict';
import { uts } from './loader.mjs';
const { alarmTime, alarmPlan } = await uts('core/alarms.uts');

test('display uses the saved AlarmKit Unix seconds and Beijing time', () => {
  assert.equal(alarmTime({ timestamp: Date.parse('2026-09-22T01:05:00Z') / 1000 }), '2026-09-22 09:05');
  assert.equal(alarmTime({ timestamp: Date.parse('2026-09-21T16:30:00Z') / 1000 }), '2026-09-22 00:30');
});

test('missing or invalid native alarm times never display an invented date', () => {
  for (const timestamp of [0, -1, Infinity, NaN]) assert.equal(alarmTime({ timestamp }), '');
  assert.equal(alarmTime({}), '');
});

test('manual alarm uses selected time without applying event lead time again',()=>{
 const now=Date.parse('2026-10-30T00:00:00Z');
 const detail={event:{id:'e',event_status:'ACTIVE',alarm_lead_minutes:60},branches:[{recipientUserId:'me',branchStatus:'ACTIVE',taskEventTime:'2026-10-31 10:00:00',latestSummary:'任务'}]};
 const custom=Date.parse('2026-11-01T02:30:00Z')/1000;
 assert.equal(alarmPlan(detail,'me',now,custom).timestamp,custom);
 assert.equal(alarmPlan(detail,'me',now,custom).timeMode,'manual');
 assert.equal(alarmPlan(detail,'me',now).timeMode,'suggested');
 detail.branches[0].taskEventTime='2026-10-31 12:00:00';
 assert.equal(alarmPlan(detail,'me',now,custom).timestamp,custom);
 assert.equal(alarmPlan(detail,'me',now).timestamp,Date.parse('2026-10-31T03:00:00Z')/1000);
 for(const timestamp of [NaN,Infinity,-1,now/1000])assert.throws(()=>alarmPlan(detail,'me',now,timestamp));
 assert.throws(()=>alarmPlan(detail,'someone-else',now,custom));
 detail.branches[0].branchStatus='COMPLETED';
 assert.throws(()=>alarmPlan(detail,'me',now,custom));
 detail.branches[0].branchStatus='ACTIVE';detail.event.event_status='STOPPED';
 assert.throws(()=>alarmPlan(detail,'me',now,custom));
});
