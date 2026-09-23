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

const {reconcileMessageKeys}=await uts('core/messages.uts');
test('live message retains its component through completion and history reconciliation',()=>{
 const live={id:'pending-ai-job',_renderKey:'live-ai-job',messageRole:'assistant',messageType:'TEXT',content:'你好'};
 const completed={...live,id:'completed-ai-job'};
 assert.equal(messageKey(live),messageKey(completed));
 const rows=reconcileMessageKeys([{id:'old',messageRole:'assistant',messageType:'TEXT',content:'你好'},{id:'new',messageRole:'assistant',messageType:'TEXT',content:'你好'}],[{id:'old',messageRole:'assistant',messageType:'TEXT',content:'你好'},completed]);
 assert.equal(messageKey(rows[0]),'old');assert.equal(messageKey(rows[1]),'live-ai-job');
 const next=reconcileMessageKeys([{...rows[1],_renderKey:undefined}],rows);
 assert.equal(messageKey(next[0]),'live-ai-job');
});
test('ambiguous repeated replies do not share a render key',()=>{
 const old={id:'completed-ai-job',_renderKey:'live-ai-job',messageRole:'assistant',messageType:'TEXT',content:'收到'};
 const rows=reconcileMessageKeys(['1','2'].map(id=>({...old,id,_renderKey:undefined})),[old]);
 assert.deepEqual(rows.map(messageKey),['1','2']);
});
test('candidate reconciliation preserves the live bubble when confirmation changes its type',()=>{
 const old={id:'completed-ai-job',_renderKey:'live-ai-job',messageRole:'assistant',messageType:'CANDIDATE',content:'已整理',payload:{candidateId:'42'}};
 const row={id:'real',messageRole:'assistant',messageType:'CANDIDATE_CONFIRMED',content:'已整理',payload:{candidateId:'42'}};
 assert.equal(messageKey(reconcileMessageKeys([row],[old])[0]),'live-ai-job');
});
