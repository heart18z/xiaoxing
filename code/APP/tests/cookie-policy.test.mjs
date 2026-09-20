import test from 'node:test';
import assert from 'node:assert/strict';
import policy from '../../../deploy/app/cookie-policy.cjs';
test('app proxy isolates legacy cookies while preserving explicit authentication and other cookies',()=>{
 const input={cookie:'saber-access-token=old-user; preference=zh; saber-refresh-token=old-refresh','blade-auth':'bearer new-user',confirm:'true'};
 const output=policy.requestHeaders(input);
 assert.equal(output.cookie,'preference=zh');assert.equal(output['blade-auth'],'bearer new-user');assert.equal(output.confirm,'true');assert.match(input.cookie,/old-user/);
 assert.equal(policy.requestHeaders({cookie:'saber-access-token=x'}).cookie,undefined);
});
test('app proxy does not overwrite the other port login cookie during silent renewal',()=>{
 assert.deepEqual(policy.responseHeaders({'set-cookie':['saber-access-token=renewed; Path=/','preference=zh; Path=/']}),{'set-cookie':['preference=zh; Path=/']});
 assert.equal(policy.responseHeaders({'set-cookie':'saber-access-token=x'} )['set-cookie'],undefined);
});
