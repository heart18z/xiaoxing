import test from 'node:test';
import assert from 'node:assert/strict';
import {reminderGroups} from '../src/page/smart-reminder/reminderGroups.mjs';
test('date range is inclusive and filters branches before deduplication',()=>{
  const groups=reminderGroups([
    row('a',{recipientName:'甲',eventTime:'2026-09-20T14:00:00'}),
    row('a',{recipientName:'乙',eventTime:'2026-09-22T14:00:00'}),
    row('undated'),row('b',{eventTime:'2026-09-21T16:00:00+08:00'})
  ],now,['2026-09-20','2026-09-21']);
  assert.deepEqual(groups[3].items.map(x=>x.eventId),['a','b']);
  assert.deepEqual(groups[3].items[0].people,['甲']);
  assert.equal(groups[3].items[0].differentTimes,undefined);
});
const now=Date.parse('2026-09-15T10:00:00+08:00');
const row=(id,extra={})=>({eventId:id,branchId:id,eventStatus:'ACTIVE',branchStatus:'ACTIVE',summary:'测试',recipientName:'朋友',...extra});
test('Shanghai day boundaries, actual sent records, future and undated groups',()=>{
  const groups=reminderGroups([
    row('today',{nextEvaluateTime:'2026-09-15T14:00:00'}),
    row('sent',{lastRemindedAt:'2026-09-15T09:00:00',nextEvaluateTime:'2026-09-15T08:00:00'}),
    row('tomorrow',{nextEvaluateTime:'2026-09-16T00:01:00'}),
    row('future',{eventTime:'2026-09-30T18:00:00'}),row('undated'),
    row('stopped',{eventStatus:'STOPPED'}),row('expired',{eventTime:'2026-09-13T00:00:00',branchStatus:'STOPPED'})
  ],now);
  assert.deepEqual(groups.map(g=>g.items.map(i=>i.eventId)),[['today'],['sent'],['tomorrow'],['future','undated']]);
});
test('one card per event/group without conflating recipients or claiming evaluation is delivery',()=>{
  const groups=reminderGroups([
    row('a',{nextEvaluateTime:'2026-09-15 13:00:00',recipientName:'甲'}),
    row('a',{nextEvaluateTime:'2026-09-15 15:00:00',recipientName:'乙'}),
    row('b',{lastRemindedAt:'2026-09-15 09:00:00',nextEvaluateTime:'2026-09-15 11:00:00'})
  ],now);
  assert.equal(groups[0].items.length,2);assert.equal(groups[0].items.find(i=>i.eventId==='a').differentTimes,true);
  assert.deepEqual(groups[1].items.map(i=>i.eventId),['b']);
});
