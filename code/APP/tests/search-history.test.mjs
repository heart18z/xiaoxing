import test from 'node:test';
import assert from 'node:assert/strict';
import {uts} from './loader.mjs';
const {groupPersonMatches,groupPersonName}=await uts('core/groups.uts');
const {reconcileMessageKeys}=await uts('core/messages.uts');
test('group search matches original names, remark and account independently',()=>{
  const person={friendRemark:'高老师',name:'高文彬',realName:'高文斌',account:'User2026'};
  for(const query of ['高文彬','高文斌','高老师',' user2026 ','文彬','']) assert.equal(groupPersonMatches(person,query),true);
  assert.equal(groupPersonMatches(person,'其他人'),false);
  assert.equal(groupPersonName(person),'高老师');
  assert.equal(groupPersonMatches({account:'2026'},'2026'),true);
});
test('unchanged history reuses the native list and message objects even with reordered JSON keys',()=>{
  const before=[{id:'1',content:'原消息',payload:{attachmentIds:['1']},_renderKey:'stable'},{id:'2',content:'最后一条'}];
  const result=reconcileMessageKeys([{payload:{attachmentIds:['1']},content:'原消息',id:'1'},{id:'2',content:'最后一条'}],before);
  assert.equal(result,before);assert.equal(result[0],before[0]);
});
test('real message edits, deletions and additions still update history while preserving unchanged rows',()=>{
  const before=[{id:'1',content:'原消息'},{id:'2',content:'最后一条'}];
  const added=reconcileMessageKeys([{...before[0]},{...before[1]},{id:'3',content:'新消息'}],before);
  assert.notEqual(added,before);assert.equal(added[0],before[0]);assert.equal(added[1],before[1]);
  const changed=reconcileMessageKeys([{...before[0]},{id:'2',content:'变更',payload:{attachmentIds:['2']}}],before);
  assert.equal(changed[0],before[0]);assert.equal(changed[1].content,'变更');
  assert.equal(reconcileMessageKeys([{...before[1]}],before).length,1);
});
