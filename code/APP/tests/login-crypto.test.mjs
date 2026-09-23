import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {randomBytes} from 'node:crypto';
import {dependency,root,uts} from './loader.mjs';
const source=fs.readFileSync(root+'core/sm2.uts','utf8').replace(/\/\/ #ifdef APP-ANDROID[\s\S]*?\/\/ #endif/g,'');
const result=await dependency('esbuild').transform(source+'\nexport {mul,fromHex,hexOf};',{loader:'ts',format:'esm'});
const field=await import('data:text/javascript;base64,'+Buffer.from(result.code).toString('base64'));
const prime=BigInt('0xfffffffeffffffffffffffffffffffffffffffff00000000ffffffffffffffff');
test('SM2 field reduction matches independent BigInt oracle, including carries and borrows',()=>{
 const values=[0n,1n,2n,prime-1n,prime-2n,1n<<224n,1n<<96n,1n<<64n];
 for(let i=0;i<300;i++) values.push(BigInt('0x'+randomBytes(32).toString('hex'))%prime);
 for(const a of values)for(const b of [prime-1n,prime-2n,a,values[(Number(a%BigInt(values.length)))]]) {
  const out=field.hexOf(field.mul(field.fromHex(a.toString(16).padStart(64,'0')),field.fromHex(b.toString(16).padStart(64,'0'))));
  assert.equal(BigInt('0x'+out),a*b%prime);
 }
});
test('responsive and sync SM2 remain interoperable for fresh random nonces',async()=>{
 const sm2=dependency('sm-crypto').sm2,pair=sm2.generateKeyPairHex();
 for(let i=0;i<8;i++){
  const nonce=randomBytes(32).toString('hex'),password='密码🔔'+i;
  const a=field.sm2Encrypt(password,pair.publicKey,nonce);
  const b=await field.sm2EncryptResponsive(password,pair.publicKey,nonce);
  assert.equal(a,b);assert.equal(sm2.doDecrypt(b,pair.privateKey,0),password);
 }
});
