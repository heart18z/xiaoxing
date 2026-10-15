// Run with playwright-cli run-code --filename. Local fixtures never mutate production configuration.
async (page) => {
  const assert=(value,name)=>{if(!value)throw new Error(name);console.log('PASS '+name);};
  const settings={pageUrl:'https://47.100.172.149/download/',androidEnabled:true,iosEnabled:false,harmonyEnabled:true,iosMode:'testflight',appStoreUrl:'',testflightUrl:'',harmonyApkEnabled:true,harmonyNativeEnabled:false,harmonyStoreUrl:''};
  const version={id:'930000000000000001',versionCode:134,versionName:'1.0.134',notes:'优化好友申请列表\n更新下载方式',fileSize:25690112,sha256:'a'.repeat(64)};
  let offline=false,available=true,downloadCount=0,lastSaved=null;
  await page.unrouteAll({behavior:'wait'});
  await page.route('**/api/**',async route=>{
    const path=new URL(route.request().url()).pathname;
    if(path==='/api/app/download/qr')return route.fulfill({contentType:'image/png',path:'output/download-page-qr.png'});
    if(path==='/api/app/download/config')return offline?route.fulfill({status:503,body:'unavailable'}):route.fulfill({json:{code:200,data:{settings:{...settings},android:available?version:{},androidAvailable:available}}});
    if(path==='/api/app/download/android.apk'){downloadCount++;return route.fulfill({headers:{'Content-Disposition':'attachment; filename="test.apk"'},contentType:'application/vnd.android.package-archive',body:'fixture APK'});}
    if(path.endsWith('/download-page/save')){lastSaved=route.request().postDataJSON();Object.assign(settings,lastSaved);return route.fulfill({json:{code:200,success:true,data:null}});}
    if(path.endsWith('/download-page'))return route.fulfill({json:{code:200,data:{...settings}}});
    if(path==='/api/blade-smart/admin/releases')return route.fulfill({json:{code:200,data:[{...version,published:true,fileName:'xiaoxing134.apk'}]}});
    return route.fulfill({json:{code:200,data:{}}});
  });
  await page.setViewportSize({width:1672,height:941});
  await page.goto('http://127.0.0.1:2888/download/');
  await page.locator('#version-label').filter({hasText:'1.0.134'}).waitFor();
  await page.screenshot({path:'output/playwright/download-desktop.png'});
  assert(await page.getByRole('button',{name:'iOS',exact:true}).isDisabled(),'closed iOS channel is disabled');
  await page.getByRole('button',{name:'iOS',exact:true}).evaluate(el=>el.click());
  assert(!await page.locator('#download-dialog').evaluate(el=>el.open),'disabled iOS cannot open dialog');
  await page.getByRole('button',{name:'鸿蒙',exact:true}).click();
  assert(await page.getByRole('button',{name:'HarmonyOS 4.x 及更早版本',exact:false}).isVisible(),'HarmonyOS choices explain system versions');
  assert(await page.getByRole('button',{name:'HarmonyOS 5 及以上 / NEXT',exact:false}).isDisabled(),'closed native HarmonyOS choice is disabled');
  await page.getByRole('button',{name:'关闭',exact:true}).click();
  await page.getByRole('button',{name:'扫码下载 / 分享'}).click();
  await page.locator('#qr-image').evaluate(image=>image.decode());
  assert((await page.locator('#page-link').getAttribute('href'))===settings.pageUrl,'QR share link uses configured canonical address');
  await page.screenshot({path:'output/playwright/download-qr.png'});
  await page.getByRole('button',{name:'关闭',exact:true}).click();
  const downloaded=page.waitForEvent('download');
  await page.getByRole('button',{name:'Android',exact:true}).click();
  await downloaded;
  assert(downloadCount===1,'Android directly downloads same-origin APK');
  await page.route('https://testflight.apple.com/**',route=>route.fulfill({body:'TestFlight fixture'}));
  await page.route('https://apps.apple.com/**',route=>route.fulfill({body:'App Store fixture'}));
  await page.route('https://appgallery.huawei.com/**',route=>route.fulfill({body:'AppGallery fixture'}));
  settings.iosEnabled=true;settings.testflightUrl='https://testflight.apple.com/join/AbCd1234';
  await page.reload();await page.locator('#version-label').filter({hasText:'1.0.134'}).waitFor();
  await page.getByRole('button',{name:'iOS',exact:true}).click();await page.waitForURL(settings.testflightUrl);
  assert(page.url()===settings.testflightUrl,'TestFlight mode follows public invite');
  settings.iosMode='appstore';settings.appStoreUrl='https://apps.apple.com/cn/app/id12345678';
  await page.goto('http://127.0.0.1:2888/download/');await page.locator('#version-label').filter({hasText:'1.0.134'}).waitFor();
  await page.getByRole('button',{name:'iOS',exact:true}).click();await page.waitForURL(settings.appStoreUrl);
  assert(page.url()===settings.appStoreUrl,'App Store mode follows official store link');
  settings.harmonyNativeEnabled=true;settings.harmonyStoreUrl='https://appgallery.huawei.com/app/C12345678';
  await page.goto('http://127.0.0.1:2888/download/');await page.locator('#version-label').filter({hasText:'1.0.134'}).waitFor();
  await page.getByRole('button',{name:'鸿蒙',exact:true}).click();
  await page.getByRole('button',{name:'HarmonyOS 5 及以上 / NEXT',exact:false}).click();await page.waitForURL(settings.harmonyStoreUrl);
  assert(page.url()===settings.harmonyStoreUrl,'native HarmonyOS opens configured AppGallery');
  settings.iosEnabled=false;settings.harmonyNativeEnabled=false;settings.harmonyEnabled=false;settings.androidEnabled=false;
  await page.goto('http://127.0.0.1:2888/download/');await page.locator('#version-label').filter({hasText:'1.0.134'}).waitFor();
  for(const name of ['Android','iOS','鸿蒙']){const button=page.getByRole('button',{name,exact:true});assert(await button.isVisible()&&await button.isDisabled(),name+' remains visible but disabled');await button.evaluate(el=>el.click());}
  assert(!await page.locator('#download-dialog').evaluate(el=>el.open)&&downloadCount===1,'closed channels cannot navigate or download');
  await page.screenshot({path:'output/playwright/download-channels-disabled.png'});
  settings.androidEnabled=true;settings.harmonyEnabled=true;
  for(const width of [390,320]){
    await page.setViewportSize({width,height:844});await page.goto('http://127.0.0.1:2888/download/');await page.locator('#version-label').filter({hasText:'1.0.134'}).waitFor();
    assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'no horizontal overflow at '+width+'px');
    await page.screenshot({path:'output/playwright/download-mobile-'+width+'.png',fullPage:true});
  }
  available=false;await page.reload();await page.locator('#version-label').filter({hasText:'持续适配'}).waitFor();
  await page.getByRole('button',{name:'Android',exact:true}).click();assert((await page.locator('#dialog-message').textContent()).includes('暂未发布'),'no published APK gives meaningful feedback');
  offline=true;await page.reload();await page.getByRole('button',{name:'重新加载'}).waitFor();
  assert(await page.locator('#load-error').isVisible(),'network error exposes retry');
  assert(await page.getByRole('button',{name:'Android',exact:true}).isDisabled(),'network failure leaves channels disabled');
  offline=false;available=true;await page.getByRole('button',{name:'重新加载'}).click();await page.locator('#version-label').filter({hasText:'1.0.134'}).waitFor();
  assert(!(await page.locator('#load-error').isVisible()),'retry recovers download config');
  // Mount the actual admin Vue component, replacing only its HTTP transport in this isolated harness.
  await page.route('**/src/axios.js*',route=>route.fulfill({contentType:'text/javascript',body:'export default async function request(c){const r=await fetch(c.url,{method:c.method||"GET",headers:{"Content-Type":"application/json"},body:c.data?JSON.stringify(c.data):undefined});return {data:await r.json()};}'}));
  await page.route('**/__download-admin',route=>route.fulfill({contentType:'text/html',body:'<html><head><link rel="stylesheet" href="/node_modules/element-plus/dist/index.css"></head><body><div id="app"></div><script type="module">import {createApp,h} from "/node_modules/.vite/deps/vue.js";import ElementPlus from "/node_modules/.vite/deps/element-plus.js";import Releases from "/src/views/smartReminder/releases.vue";const app=createApp(Releases);app.component("basic-container",{setup(props,{slots}){return ()=>h("main",{style:{padding:"24px"}},slots.default());}});app.use(ElementPlus);app.mount("#app");</script></body></html>'}));
  await page.setViewportSize({width:1440,height:1050});
  await page.goto('http://127.0.0.1:2888/__download-admin');
  await page.getByRole('button',{name:'保存下载配置'}).waitFor();
  await page.getByPlaceholder('https://你的域名/download/').fill('https://47.100.172.149/download/');
  await page.locator('.el-radio').filter({hasText:'TestFlight 邀请'}).click();
  await page.locator('.el-switch').nth(1).locator('.el-switch__core').click();
  await page.getByPlaceholder('https://testflight.apple.com/join/邀请码').fill('');
  await page.getByRole('button',{name:'保存下载配置'}).click();
  await page.getByText('开启 iOS 下载前，请填写对应的官方链接',{exact:true}).waitFor();
  assert(lastSaved===null,'admin rejects enabled iOS without a link');
  await page.getByPlaceholder('https://testflight.apple.com/join/邀请码').fill('https://testflight.apple.com/join/Changed1');
  await page.getByRole('button',{name:'保存下载配置'}).click();
  await page.getByText('下载配置已保存',{exact:true}).waitFor();
  assert(lastSaved?.testflightUrl.endsWith('Changed1')&&lastSaved.iosMode==='testflight'&&lastSaved.iosEnabled,'real admin form saves correct platform fields');
  await page.locator('.el-switch').nth(0).locator('.el-switch__core').click();
  await page.locator('.el-switch').nth(2).locator('.el-switch__core').click();
  await page.getByRole('button',{name:'保存下载配置'}).click();
  await page.waitForFunction(()=>!document.querySelector('.download-settings .el-loading-mask'));
  await page.waitForTimeout(300);
  assert(lastSaved?.androidEnabled===false&&lastSaved?.harmonyEnabled===false&&lastSaved?.harmonyApkEnabled===true,'admin persists master switches and retains compatibility choice');
  await page.reload();await page.getByRole('button',{name:'保存下载配置'}).waitFor();
  assert(await page.locator('.el-switch').nth(3).getAttribute('class').then(c=>c.includes('is-disabled')),'disabled HarmonyOS master disables child controls after reload');
  await page.screenshot({path:'output/playwright/download-admin.png',fullPage:true});
  console.log('DOWNLOAD PAGE BROWSER CHECKS PASSED');
}
