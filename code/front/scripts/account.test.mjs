import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {DEFAULT_PASSWORD,registrationError,passwordError,profileError} from '../src/page/smart-reminder/accountRules.mjs';
const valid=()=>({account:'staff_123',name:'测试用户',password:DEFAULT_PASSWORD,phone:'',email:'fixture@example.com'});
test('registration requires a contact and uses the new editable default password',()=>{
  assert.equal(DEFAULT_PASSWORD,'admin@123');
  assert.ok(registrationError({...valid(),phone:'',email:''}));
  assert.equal(registrationError({...valid(),phone:'13800138000',email:''}),'');
  assert.equal(registrationError({...valid(),phone:'',email:'fixture@example.com'}),'');
  assert.equal(registrationError({...valid(),password:'custom-password'}),'');
});
test('profile edits validate contact details and allow clearing optional fields',()=>{
  assert.equal(profileError(valid()),'');
  assert.equal(profileError({...valid(),phone:'13800138000',email:'fixture@example.com'}),'');
  for(const patch of [{name:' '},{name:'人'.repeat(21)},{phone:'123'},{email:'bad'},{email:'x'.repeat(46)}])assert.ok(profileError({...valid(),...patch}));
});
test('registration rejects short/oversize/invalid accounts and accepts exact boundaries',()=>{
  for(const account of ['','abc','x'.repeat(33),'人员号123','user name','_abc','<img>'])assert.ok(registrationError({...valid(),account}));
  for(const account of ['1234','A'.repeat(32),'staff-123'])assert.equal(registrationError({...valid(),account}),'');
});
test('registration enforces name, password and optional contact bounds',()=>{
  for(const patch of [{name:''},{name:' '},{name:'人'.repeat(21)},{password:'1234567'},{password:' '.repeat(8)},{password:'x'.repeat(65)},{phone:'123'},{email:'bad'},{email:'x'.repeat(40)+'@example.com'}])assert.ok(registrationError({...valid(),...patch}));
  assert.equal(registrationError(valid()),'');
  assert.equal(registrationError({...valid(),phone:'13800138000',email:'test@example.com'}),'');
});
test('password change requires old password and matching new passwords',()=>{
  const form={oldPassword:'old-fixture',password:'new-fixture',confirmation:'new-fixture'};
  assert.equal(passwordError(form),'');
  for(const patch of [{oldPassword:''},{password:'short'},{confirmation:'different'}])assert.ok(passwordError({...form,...patch}));
});
test('native keyboard uses one resize owner and a smaller thinking viewport',()=>{
  assert.equal(JSON.parse(readFileSync('capacitor.config.json')).plugins.Keyboard.resize,'none');
  assert.match(readFileSync('src/page/smart-reminder/ThinkingPanel.vue','utf8'),/max-height:138px/);
  assert.match(readFileSync('ios/App/App/Info.plist','utf8'),/NSSpeechRecognitionUsageDescription/);
});
