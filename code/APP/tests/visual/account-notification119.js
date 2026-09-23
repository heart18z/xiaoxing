// Local API fixtures; never creates an account or changes a real password.
async page => {
 await page.unrouteAll({behavior:'ignoreErrors'});
 await page.setViewportSize({width:390,height:844});
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/api/**',async route=>{
  const url=route.request().url();let data={};
  if(url.includes('suggest-account'))data='fixture119';
  if(url.includes('bootstrap'))data={user:{id:'9001',name:'测试用户'},language:'zh-cn'};
  if(url.includes('settings/models'))data={value:{models:[],language:'zh-cn'}};
  await route.fulfill({json:{code:200,data}});
 });
 await page.goto('http://localhost:5174');await page.evaluate(()=>{sessionStorage.clear();localStorage.clear();});
 await page.goto('http://localhost:5174/#/pages/login/index');await page.reload();
 await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});
 await page.getByText('没有账号？注册账号',{exact:true}).click();
 await page.locator('.contact-fields input').last().waitFor();await page.waitForTimeout(300);
 const fields=page.locator('.contact-fields input');const a=await fields.nth(0).boundingBox(),b=await fields.nth(1).boundingBox();
 if(b.y<a.y+a.height||Math.abs(a.x-b.x)>2||a.width<280)throw Error('Contact fields are not full-width rows');
 await page.locator('.form-columns input').nth(1).fill('测试用户');
 await fields.nth(0).fill('13800138000');
 await page.locator('.popup-footer .login-submit').click();
 await page.getByText('请填写邮箱',{exact:true}).waitFor({state:'attached'});
 await page.screenshot({path:'output/playwright/mobile119-registration.png'});
 await page.locator('.mask .close').click();
 await page.evaluate(()=>sessionStorage.setItem('xiaoxing:session',JSON.stringify({token:'fixture',refresh:'fixture',userId:'9001',expiresAt:2000000000000})));
 await page.goto('http://localhost:5174/#/pages/settings/index');await page.reload();
 await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});
 await page.getByText('开启 / 检查通知',{exact:true}).click();
 await page.locator('.top-tip').waitFor();await page.waitForTimeout(150);
 const tip=await page.locator('.top-tip').boundingBox();if(tip.y>120)throw Error('Feedback is not a top tip');
 await page.screenshot({path:'output/playwright/mobile119-notification.png'});
 await page.getByText('重置密码',{exact:true}).click();await page.locator('.mask .panel').waitFor();await page.waitForTimeout(250);
 const panel=await page.locator('.mask .panel').boundingBox(),last=await page.locator('.mask input').last().boundingBox(),button=await page.locator('.popup-footer .login-submit').boundingBox();
 if(panel.height>415||button.y-last.y-last.height>100||button.height<44)throw Error('Password panel is too tall or button too far away');
 await page.screenshot({path:'output/playwright/mobile119-password.png'});
 await page.setViewportSize({width:360,height:540});await page.reload();await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});await page.getByText('重置密码',{exact:true}).click();await page.waitForTimeout(300);
 const small=await page.locator('.popup-footer .login-submit').boundingBox();if(small.y+small.height>532)throw Error('Small screen password action clipped');
 if(errors.length)throw Error(errors.join('\n'));
 return {registrationRows:true,emailRequired:true,topTipY:tip.y,passwordHeight:panel.height,buttonGap:button.y-last.y-last.height,smallScreenButton:small};
}
