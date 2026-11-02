import test from 'node:test';
import assert from 'node:assert/strict';
import {uts} from './loader.mjs';
const {eventReply,replyPayload}=await uts('core/event-reply.uts');
test('quote binds the selected card rather than a subsequently arriving event',()=>{
 const selected=eventReply({eventId:'1001',content:'提醒内容',payload:{eventSummary:'项目会议'}});
 eventReply({eventId:'1002',content:'新到提醒'});
 assert.deepEqual(replyPayload(selected),{replyEventId:'1001',replyEventSummary:'项目会议'});
 assert.deepEqual(replyPayload({}),{});
});
test('legacy payload cards are supported; non-event messages never become a quote',()=>{
 assert.equal(eventReply({payloadJson:JSON.stringify({eventId:'9001',eventSummary:'旧卡片'})}).replyEventId,'9001');
 for(const eventId of ['', '0', '-1', 'not-an-event', '1 OR 1=1'])assert.deepEqual(eventReply({eventId}),{});
 assert.equal(replyPayload({replyEventId:'123',replyEventSummary:'长'.repeat(500)}).replyEventSummary.length,240);
});
