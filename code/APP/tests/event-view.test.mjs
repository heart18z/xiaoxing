import test from 'node:test';
import assert from 'node:assert/strict';
import {uts} from './loader.mjs';
const {eventTimeline,eventOverview,eventIsAi}=await uts('core/event-view.uts');
test('event overview falls back on empty latest value like Web',()=>{
  assert.equal(eventOverview({latest_summary:'',event_summary:'会议 2026-09-20T10:00'}),'会议 2026-09-20 10:00');
});
test('timeline includes global updates but not other branch updates and deduplicates read receipts',()=>{
  const timeline=[
    {id:'1',nodeType:'EVENT_UPDATED'},
    {id:'2',nodeType:'EVENT_UPDATED',branchId:'b2'},
    {id:'3',nodeType:'NOTIFICATION_READ',branchId:'b1',actorUserId:'1',createTime:'2026-09-20 10:00:01'},
    {id:'4',nodeType:'NOTIFICATION_READ',branchId:'b1',actorUserId:'1',createTime:'2026-09-20 10:00:25'},
    {id:'5',nodeType:'NOTIFICATION_READ',branchId:'b1',actorUserId:'2',createTime:'2026-09-20 10:00:25'},
    {id:'6',nodeType:'ASK_RECIPIENT',branchId:'b1'},
  ];
  assert.deepEqual(eventTimeline({timeline},true,'b1').map(x=>x.id),['1','3','5','6']);
  assert.deepEqual(eventTimeline({timeline},false,'b1').map(x=>x.id),['1','3','5']);
  assert.equal(eventIsAi({nodeType:'CREATOR_EVALUATION_REQUESTED'}),false);
});
