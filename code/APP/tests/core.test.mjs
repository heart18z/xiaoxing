import test from 'node:test';
import assert from 'node:assert/strict';
import {dependency,uts} from './loader.mjs';
const crypto=await uts('core/sm2.uts');
test('SM3 official abc digest',()=>assert.equal(crypto.bytesHex(crypto.sm3([...Buffer.from('abc')])),'66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0'));
test('UTS SM2 mode 0 decrypts with existing sm-crypto for Unicode passwords',()=>{
  const old=dependency('sm-crypto').sm2,pair=old.generateKeyPairHex();
  for(const password of ['123456','小醒密码🔔test']){
    const encrypted=crypto.sm2Encrypt(password,pair.publicKey,'1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef');
    assert.equal(old.doDecrypt(encrypted,pair.privateKey,0),password);
  }
});
test('SM2 rejects zero nonce',()=>assert.throws(()=>crypto.sm2Encrypt('a','04'+'1'.repeat(128),'0'.repeat(64))));
const {Utf8Stream,SseFrames}=await uts('core/stream.uts');
test('SSE survives every byte boundary, CRLF and non-BMP characters',()=>{
  const decoder=new Utf8Stream(),frames=new SseFrames(),out=[];
  const text=': heartbeat\r\n\r\ndata: '+JSON.stringify({type:'snapshot',reasoning:'提醒🔔中文'})+'\r\n\r\ndata: {"type":"result","data":{"status":"RUNNING"}}\n\n';
  for(const byte of Buffer.from(text))out.push(...frames.push(decoder.decode(Uint8Array.of(byte).buffer)));
  assert.equal(out.length,2);assert.equal(JSON.parse(out[0]).reasoning,'提醒🔔中文');assert.equal(JSON.parse(out[1]).data.status,'RUNNING');
});
const time=await uts('core/time.uts');
test('Beijing dates cross midnight independently of local timezone',()=>{
  assert.equal(time.dayOf(Date.parse('2026-09-16T16:30:00Z')),'2026-09-17');
  assert.equal(time.timeOf('2026-09-17 09:00:00'),Date.parse('2026-09-17T01:00:00Z'));
  assert.equal(time.dayOffset('2024-02-28',1),'2024-02-29');
});
const {reminderGroups}=await uts('core/reminders.uts');
test('drawer groups use reminder time, deduplicate branches and filter before merge',()=>{
  const base={eventId:'1',eventStatus:'ACTIVE',branchStatus:'ACTIVE',eventSummary:'开会'};
  const rows=[{...base,branchId:'1',recipientName:'甲',lastRemindedAt:'2026-09-17 08:00:00',nextEvaluateTime:'2026-09-17 15:00:00'}, {...base,branchId:'2',recipientName:'乙',nextEvaluateTime:'2026-09-18 15:00:00'}];
  const groups=reminderGroups(rows,time.timeOf('2026-09-17 10:00:00'));
  assert.equal(groups[0].items.length,1);assert.equal(groups[1].items.length,1);assert.equal(groups[2].items.length,1);
  const filtered=reminderGroups(rows,time.timeOf('2026-09-17 10:00:00'),'2026-09-18','2026-09-18');
  assert.equal(filtered[0].items.length,0);assert.equal(filtered[2].items[0].people,'乙');
});
const {alarmPlan}=await uts('core/alarms.uts');
test('alarm is bound to own active branch and explicit null never falls back',()=>{
 const event={id:'e1',event_status:'ACTIVE',event_time:'2026-09-18 15:00:00'};
 const own={recipientUserId:'1',branchStatus:'ACTIVE',taskEventTime:'2026-09-18 16:00:00'};
 const now=time.timeOf('2026-09-17 10:00:00');
 assert.equal(alarmPlan({event,branches:[own]},'1',now).timestamp,time.timeOf(own.taskEventTime)/1000);
 assert.throws(()=>alarmPlan({event,branches:[own]},'2',now));
 assert.throws(()=>alarmPlan({event,branches:[{...own,taskEventTime:null}]},'1',now));
 assert.throws(()=>alarmPlan({event,branches:[{...own,branchStatus:'STOPPED'}]},'1',now));
 assert.throws(()=>alarmPlan({event,branches:[own]},'1',now+86400000*2));
 const legacy={recipientUserId:'1',branchStatus:'ACTIVE'};
 assert.equal(alarmPlan({event,branches:[legacy]},'1',now).timestamp,time.timeOf(event.event_time)/1000);
});
const json=await uts('core/json.uts');
test('fresh storage is empty without attempting JSON parsing',()=>assert.deepEqual(json.parseObject(''),{}));

test('invalid calendar dates cannot be normalized into an unintended alarm date',()=>{assert.ok(Number.isNaN(time.timeOf('2026-02-30 10:00:00')));assert.ok(Number.isNaN(time.timeOf('2026-02-29 10:00:00')));assert.ok(Number.isNaN(time.timeOf('2026-09-17 25:00:00')));assert.ok(Number.isFinite(time.timeOf('2024-02-29 10:00:00')));});
