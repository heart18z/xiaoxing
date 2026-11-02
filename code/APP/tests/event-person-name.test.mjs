import test from 'node:test';
import assert from 'node:assert/strict';
import { uts } from './loader.mjs';
const { eventPersonName } = await uts('core/events.uts');
test('timeline names fall back through empty, whitespace and legacy null names', () => {
  assert.equal(eventPersonName({name:'',realName:'高文彬',account:'2026'}),'高文彬');
  assert.equal(eventPersonName({name:'  ',realName:null,account:'2026'}),'2026');
  assert.equal(eventPersonName({name:'null',realName:'undefined',account:''}),'群成员');
  assert.equal(eventPersonName({name:'昵称',realName:'姓名',account:'2026'}),'昵称');
});
