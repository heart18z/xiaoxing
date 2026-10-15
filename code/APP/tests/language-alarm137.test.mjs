import test from 'node:test';
import assert from 'node:assert/strict';
import { uts } from './loader.mjs';
const locale=await uts('core/language.uts');
const alarms=await uts('core/alarms.uts');
const {timeOf}=await uts('core/time.uts');
test('system language resolves Chinese variants and English fallback; manual selection wins',()=>{
  globalThis.uni={getSystemInfoSync:()=>({osLanguage:'zh-Hans-CN'})};
  assert.equal(locale.effectiveLanguage('system'),'zh-cn');
  assert.equal(locale.effectiveLanguage('en-us'),'en-us');
  globalThis.uni.getSystemInfoSync=()=>({osLanguage:'en-GB'});
  assert.equal(locale.effectiveLanguage('system'),'en-us');
  assert.equal(locale.effectiveLanguage('zh-cn'),'zh-cn');
  assert.equal(locale.normalizeLanguage('zh-Hant'),'zh-cn');
  assert.equal(locale.normalizeLanguage('fr-FR'),'en-us');
});
test('own active future task uses preparation lead and tracks task time changes',()=>{
  const now=timeOf('2026-10-15 09:00:00');
  const detail={event:{id:'1',event_status:'ACTIVE',alarm_lead_minutes:30,alarm_requested:true},branches:[{recipientUserId:'me',branchStatus:'ACTIVE',taskEventTime:'2026-10-15 15:00:00'}]};
  assert.equal(alarms.alarmPlan(detail,'me',now).timestamp,timeOf('2026-10-15 14:30:00')/1000);
  assert.equal(alarms.canSetAlarm(detail,'other',now),false);
  detail.branches[0].taskEventTime='2026-10-15 16:00:00';
  assert.equal(alarms.alarmPlan(detail,'me',now).timestamp,timeOf('2026-10-15 15:30:00')/1000);
  detail.branches[0].branchStatus='STOPPED';assert.equal(alarms.canSetAlarm(detail,'me',now),false);
});
test('candidate alarms require own recipient and future explicit alarm or event time',()=>{
  const now=timeOf('2026-10-15 09:00:00');
  const event={recipients:[{id:'me'}],alarmTime:'2026-10-15 14:30:00'};
  assert.equal(alarms.candidateAlarmAvailable(event,'me',now),true);
  assert.equal(alarms.candidateAlarmAvailable(event,'other',now),false);
  assert.equal(alarms.candidateAlarmAvailable({recipients:event.recipients,firstEvaluateTime:event.alarmTime},'me',now),false);
  assert.equal(alarms.candidateAlarmAvailable(event,'me',timeOf('2026-10-15 15:00:00')),false);
});
