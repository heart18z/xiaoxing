async page => {
 const checks=[],errors=[],writes=[];
 page.on('pageerror',e=>errors.push(e.message));
 page.on('request',r=>{if(r.url().endsWith('/profile/update'))writes.push(JSON.parse(r.postData()));});
 const origin='http://localhost:5174';
 const go=async name=>{await page.goto(origin+'/#/pages/'+name+'/index');await page.locator('.frame').waitFor();};
 await page.route('**/api/app/reminder/profile/update',async r=>{
  const data=JSON.parse(r.request().postData());
  if(!data.nickname)throw Error('Missing required nickname');
  await r.fulfill({json:{code:200,data:{}}});
 });
 const people=[{id:'r1',userId:'9101',name:'好友一',account:'contract01',avatar:'/avatars/user/B2.png',permissionMode:'MUTUAL',requestType:'FRIEND',requestMessage:'希望一起安排提醒'}];
 await page.route('**/api/app/reminder/friends/requests',r=>r.fulfill({json:{code:200,data:{incoming:people,outgoing:[{...people[0],id:'r2'}]}}}));
 await page.route('**/api/app/reminder/friends/search**',r=>r.fulfill({json:{code:200,data:[{...people[0],relationStatus:'NONE'}]}}));
 await go('me');await page.locator('.profile-picture').click();await page.locator('.avatar-choice').nth(2).click();await page.locator('.mask').waitFor({state:'hidden'});
 await page.locator('.assistant-card').click();await page.locator('.avatar-choice').nth(1).click();await page.locator('.mask').waitFor({state:'hidden'});
 if(writes.length!==2||!writes[0].avatar||!writes[1].aiAvatar||writes.some(w=>w.nickname!=='陈通'))throw Error('Avatar contract invalid');checks.push('Both avatars include nickname');
 await page.getByText('添加好友',{exact:true}).click();await page.locator('.mask input').fill('contract01');await page.getByText('查找',{exact:true}).click();await page.locator('.search-person image,.search-person uni-image').waitFor();await page.screenshot({path:'output/playwright/contracts-search.png'});checks.push('Search avatar and name');
 await page.locator('.mask .close').click();await page.locator('.segment-item').nth(1).click();await page.locator('.request-card').first().waitFor();if(await page.locator('.request-card uni-image').count()!==2)throw Error('Missing request avatars');await page.screenshot({path:'output/playwright/contracts-requests.png'});checks.push('Incoming and outgoing avatars');
 await go('chat');let status='NOT_FOUND',submits=0;
 await page.route('**/api/app/reminder/chat/jobs/**',r=>{if(r.request().url().includes('/submit')){submits++;status='SUCCEEDED';}return r.fulfill({json:{code:200,data:{status,streamSupported:false,result:{reply:'收到'}}}})});
 const input=page.locator('.compose-input textarea');await input.fill('第一行');await input.press('Shift+Enter');if(submits!==0||!(await input.inputValue()).includes('\n'))throw Error('Shift Enter must insert newline');
 await input.dispatchEvent('keydown',{key:'Enter',code:'Enter',keyCode:229,isComposing:true,bubbles:true});if(submits!==0)throw Error('IME composition submitted');
 await input.fill('测试回车发送');await input.press('Enter');await page.waitForTimeout(1200);if(submits!==1)throw Error('Enter must submit exactly once: '+submits);checks.push('Enter sends once; Shift Enter new line; IME does not submit');
 await page.locator('.toolbar').first().click();await page.locator('.drawer-card').first().waitFor();if(!(await page.locator('.drawer-card').first().innerText()).includes('标书')&&!(await page.locator('.drawer-card').first().innerText()).includes('报名'))throw Error('summary missing');await page.screenshot({path:'output/playwright/contracts-drawer.png'});checks.push('Drawer backend summary field');
 await go('events');await page.waitForTimeout(300);if(await page.locator('.uni-scroll-view-refresher-triggered').count())throw Error('Ordinary loading triggered refresh');await page.screenshot({path:'output/playwright/contracts-events.png'});
 if(errors.length)throw Error(errors.join(';'));return {checks,errors};
}
