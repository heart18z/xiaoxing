import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import {dependency,root} from './loader.mjs';
let serial=0;
async function fixture(reply){
 const session={token:'old',refresh:'refresh',userId:'1',revision:0};let refreshed=0,cleared=0;
 const env={session,saveSession:data=>{session.token=data.access_token;refreshed++},clearSession:()=>{session.token='';session.revision++;cleared++},applyBootstrap:()=>{},sm2Encrypt:()=>'',sm2EncryptResponsive:async()=>'',randomHex:()=>'',BASE_URL:'https://fixture.invalid',BASIC_AUTH:'fixture',TENANT:'000000',SM2_PUBLIC_KEY:'',UTSAndroid:{getDispatcher:()=>({async:fn=>queueMicrotask(()=>fn(null))})}};
 globalThis.__requestFixture=env;globalThis.UTSAndroid=env.UTSAndroid;JSON.parseObject=JSON.parse;
 globalThis.uni={request:options=>{queueMicrotask(()=>reply(options,session));return{}},reLaunch:()=>{}};
 const imports=['@/store/session.uts','@/core/sm2.uts','@/uni_modules/xiaoxing-native','@/config/environment.uts'];
 const bundle=await dependency('esbuild').build({entryPoints:[root+'api/request.uts'],bundle:true,write:false,format:'esm',platform:'node',loader:{'.uts':'ts'},plugins:[{name:'fixture',setup(build){build.onResolve({filter:/.*/},args=>imports.includes(args.path)?{path:args.path,namespace:'fixture'}:args.path.startsWith('@/')?{path:path.join(root,args.path.slice(2))}:null);build.onLoad({filter:/.*/,namespace:'fixture'},()=>({contents:'export const {'+Object.keys(env).join(',')+'}=globalThis.__requestFixture;',loader:'js'}))}}]});
 const module=await import('data:text/javascript;base64,'+Buffer.from(bundle.outputFiles[0].text+`\n// ${serial++}`).toString('base64'));
 return {module,session,refreshed:()=>refreshed,cleared:()=>cleared};
}
test('concurrent 401 responses share one token refresh and retry with fresh headers',async()=>{
 let refreshes=0;
 const f=await fixture(o=>{
  if(o.url.includes('/oauth/token')){refreshes++;setTimeout(()=>o.success({statusCode:200,data:{access_token:'new'}}),10);return}
  o.success(o.header['Blade-Auth']==='bearer old'?{statusCode:401,data:{msg:'expired'}}:{statusCode:200,data:{code:200,data:'ok'}});
 });
 const result=await Promise.all([f.module.post('/a'),f.module.post('/b')]);
 assert.equal(refreshes,1);assert.equal(f.refreshed(),1);assert.equal(result[1].data,'ok');
});
test('temporary refresh network failure preserves login',async()=>{
 const f=await fixture(o=>o.url.includes('/oauth/token')?o.fail({}):o.success({statusCode:401,data:{}}));
 await assert.rejects(f.module.post('/a'));assert.equal(f.cleared(),0);assert.equal(f.session.token,'old');
});
test('late response rejects after account switch, except explicit push cleanup response',async()=>{
 const f=await fixture((o,session)=>{session.revision++;session.userId='2';o.success({statusCode:200,data:{code:200,data:{bindingId:'binding-old'}}})});
 await assert.rejects(f.module.post('/a'),/账号已变化/);
 const binding=await f.module.post('/app/push/register',null,{},15000,true,true);assert.equal(binding.data.bindingId,'binding-old');
});
test('multipart headers omit JSON Content-Type',async()=>{
 const f=await fixture(()=>{});assert.equal(f.module.headers(false)['Content-Type'],undefined);assert.match(f.module.headers()['Content-Type'],/json/);
});

test('duplicate login never retries or saves a session without explicit confirmation',async()=>{
 const sent=[];
 const f=await fixture(o=>{sent.push(o.header);o.success({statusCode:401,data:{error:'need_confirm_login',error_description:'账号已在其他地址登录，是否继续？'}})});
 await assert.rejects(f.module.login('fixture','secret'),e=>e.status===601);
 assert.equal(sent.length,1);assert.equal(sent[0].confirm,undefined);assert.equal(f.refreshed(),0);assert.equal(f.session.token,'old');
});
test('confirmed login sends the existing confirm header and only saves successful credentials',async()=>{
 const sent=[];
 const f=await fixture(o=>{if(o.url.includes('/oauth/token')){sent.push(o.header);o.success({statusCode:200,data:{access_token:'confirmed',refresh_token:'refresh',user_id:'1',role_name:'app_user'}})}else o.success({statusCode:200,data:{code:200,data:{}}})});
 await f.module.login('fixture','secret','','',true);
 assert.equal(sent.length,1);assert.equal(sent[0].confirm,'true');assert.equal(f.session.token,'confirmed');
});
test('wrapped code 601 and string OAuth responses both require confirmation',async()=>{
 for(const data of [{data:{code:601,message:'duplicate'}},JSON.stringify({error:'need_confirm_login'})]){
  const f=await fixture(o=>o.success({statusCode:200,data}));
  await assert.rejects(f.module.login('fixture','secret'),e=>e.status===601);assert.equal(f.refreshed(),0);
 }
});

test('new login bootstrap rejection stays on login screen without token refresh',async()=>{
 let tokens=0,relaunches=0;
 const f=await fixture(o=>{if(o.url.includes('/oauth/token')){tokens++;o.success({statusCode:200,data:{access_token:'new',refresh_token:'r',user_id:'1',role_name:'app_user'}})}else o.success({statusCode:401,data:{msg:'登录状态校验失败，请重试'}})});
 globalThis.uni.reLaunch=()=>{relaunches++};
 await assert.rejects(f.module.login('fixture','secret','','',true),/登录状态校验失败/);
 assert.equal(tokens,1);assert.equal(relaunches,0);assert.equal(f.cleared(),1);
});

test('invalid refresh business response clears session and returns to login once',async()=>{
 let launches=0;
 const f=await fixture(o=>o.success(o.url.includes('/oauth/token')?{statusCode:200,data:{code:500,msg:'令牌刷新错误或无效'}}:{statusCode:401,data:{msg:'expired'}}));
 globalThis.uni.reLaunch=()=>launches++;
 await Promise.allSettled([f.module.post('/a'),f.module.post('/b')]);
 assert.equal(f.cleared(),1);assert.equal(launches,1);
});
test('OAuth invalid_grant without a business code ends the session',async()=>{
 const f=await fixture(o=>o.success(o.url.includes('/oauth/token')?{statusCode:200,data:{error:'invalid_grant',error_description:'Refresh token expired'}}:{statusCode:401,data:{}}));
 await assert.rejects(f.module.post('/a'));assert.equal(f.cleared(),1);
});
test('a rejected retry after successful refresh returns to login',async()=>{
 const f=await fixture(o=>o.success(o.url.includes('/oauth/token')?{statusCode:200,data:{access_token:'new'}}:{statusCode:401,data:{}}));
 await assert.rejects(f.module.post('/a'));assert.equal(f.refreshed(),1);assert.equal(f.cleared(),1);
});
test('unrelated refresh server failure preserves the session',async()=>{
 const f=await fixture(o=>o.success(o.url.includes('/oauth/token')?{statusCode:500,data:{msg:'服务暂时不可用'}}:{statusCode:401,data:{}}));
 await assert.rejects(f.module.post('/a'));assert.equal(f.cleared(),0);
});
test('late unauthorized response cannot refresh or clear a new account',async()=>{
 let refreshes=0;
 const f=await fixture((o,session)=>{if(o.url.includes('/oauth/token'))refreshes++;session.userId='2';session.revision++;o.success({statusCode:401,data:{}})});
 await assert.rejects(f.module.post('/a'),/账号已变化/);assert.equal(refreshes,0);assert.equal(f.cleared(),0);
});
