import test from 'node:test';
import assert from 'node:assert/strict';
import {alarmPlan,alarmTimestamp} from '../src/native/alarmPlan.mjs';
import {tokenCookieOptions} from '../src/utils/tokenExpiry.mjs';
const detail=()=>({event:{id:'99',event_status:'ACTIVE',event_time:'2030-09-14 15:30:00',event_summary:'全局任务'},branches:[{recipientUserId:'1',branchStatus:'ACTIVE',latestSummary:'自己的任务',taskEventTime:'2030-09-14 16:30:00'}]});
test('fixed Shanghai time, not the device timezone or first AI evaluation time',()=>{
  const plan=alarmPlan(detail(),'1',0);
  assert.equal(plan.timestamp,Date.parse('2030-09-14T08:30:00Z')/1000);
  assert.equal(plan.title,'自己的任务');assert.equal(plan.eventId,'99');
  assert.equal(alarmTimestamp('2030-09-14T16:30'),plan.timestamp);
  for(const value of ['2030-02-30 12:00','明天','2030-09-14T16:30Z','2030-09-14 24:01:00'])assert.ok(Number.isNaN(alarmTimestamp(value)));
});
test('reject other recipients, stopped tasks, past dates and explicit cleared scoped time',()=>{
  assert.throws(()=>alarmPlan(detail(),'2',0),/自己/);
  const d=detail();d.event.event_status='STOPPED';assert.throws(()=>alarmPlan(d,'1',0),/停止/);
  d.event.event_status='ACTIVE';d.branches[0].branchStatus='STOPPED';assert.throws(()=>alarmPlan(d,'1',0),/停止/);
  d.branches[0].branchStatus='ACTIVE';d.branches[0].taskEventTime=null;assert.throws(()=>alarmPlan(d,'1',0),/未来时间/);
  assert.throws(()=>alarmPlan(detail(),'1',Date.parse('2031-01-01')/1000),/未来时间/);
});
test('cookie survives browser restart only until the server JWT expiry',()=>{
  const exp=1900000000,token='header.'+Buffer.from(JSON.stringify({exp})).toString('base64url')+'.signature';
  assert.equal(tokenCookieOptions(token).expires.getTime(),exp*1000);
  assert.equal(tokenCookieOptions('invalid').expires,undefined);
});
