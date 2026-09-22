// Run after replica-fixture.js; all responses are local fixtures.
async page => {
  const base=page.url().split('/').slice(0,3).join('/');
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  let launches=0;
  await page.route('**/api/**/oauth/token?*',route=>{launches++;return route.fulfill({json:{code:500,msg:'令牌刷新错误或无效'}})});
  await page.route('**/api/**/friends/list',route=>route.fulfill({status:401,json:{code:401,msg:'expired'}}));
  await page.goto(base+'/#/pages/me/index');await page.reload();
  await page.getByText('没有账号？注册账号',{exact:true}).waitFor();
  if(launches!==1)throw Error('Invalid refresh should redirect once');
  const results=[];
  for(const height of [740,540]){
    await page.setViewportSize({width:360,height});
    await page.reload();
    await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});
    await page.getByText('没有账号？注册账号',{exact:true}).click();
    const button=page.locator('.popup-footer .primary');
    await button.waitFor();await page.waitForTimeout(250);
    const before=await button.boundingBox();
    if(before.height<44||before.y+before.height>height-8)throw Error('Registration button is clipped');
    const scroll=page.locator('.account-panel .popup-scroll .uni-scroll-view').last();
    await scroll.evaluate(el=>el.scrollTop=el.scrollHeight);
    await page.waitForTimeout(100);
    const after=await button.boundingBox();
    if(Math.abs(after.y-before.y)>1)throw Error('Registration action moved with form');
    await button.click();
    await page.getByText('账号须为4–32位字母、数字、下划线或短横线，首位为字母或数字，并填写姓名',{exact:true}).waitFor({state:'attached'});
    results.push({height,buttonHeight:after.height,bottom:after.y+after.height});
    await page.screenshot({path:'output/playwright/android110-register-'+height+'.png'});
  }
  if(errors.length)throw Error(errors.join('\n'));
  return {expiredTokenRedirect:true,results};
}
