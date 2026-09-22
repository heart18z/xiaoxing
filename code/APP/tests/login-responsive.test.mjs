import test from 'node:test';
import assert from 'node:assert/strict';
import {uts,dependency} from './loader.mjs';
const crypto=await uts('core/sm2.uts');
test('responsive login encryption yields to UI timers and preserves SM2 mode 0',async()=>{
 const pair=dependency('sm-crypto').sm2.generateKeyPairHex();
 const nonce='1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef';
 let frames=0;
 const timer=setInterval(()=>frames++,5);
 let result;
 try {result=await crypto.sm2EncryptResponsive('登录密码🔔',pair.publicKey,nonce);} finally {clearInterval(timer);}
 assert.ok(frames>10,'Encryption must allow animation timers to run throughout its work');
 assert.equal(dependency('sm-crypto').sm2.doDecrypt(result,pair.privateKey,0),'登录密码🔔');
 assert.equal(result,crypto.sm2Encrypt('登录密码🔔',pair.publicKey,nonce));
 await assert.rejects(crypto.sm2EncryptResponsive('a',pair.publicKey,'0'.repeat(64)));
});
