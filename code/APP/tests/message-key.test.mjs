import test from 'node:test';
import assert from 'node:assert/strict';
import { uts } from './loader.mjs';
const {messageKey}=await uts('core/messages.uts');
test('candidate keeps its component key through optimistic history reconciliation and confirmation',()=>{
 const key=messageKey({id:'completed-ai-job',messageType:'CANDIDATE',payload:{candidateId:'42'}});
 assert.equal(messageKey({id:'server-123',messageType:'CANDIDATE_CONFIRMED',payload:{candidateId:'42'}}),key);
 assert.notEqual(messageKey({id:'server-124',messageType:'CANDIDATE',payload:{candidateId:'43'}}),key);
 assert.notEqual(messageKey({id:'server-125',messageType:'TEXT',payload:{candidateId:'42'}}),key);
 assert.equal(messageKey({id:'plain',messageType:'CANDIDATE',payload:{}}),'plain');
});
