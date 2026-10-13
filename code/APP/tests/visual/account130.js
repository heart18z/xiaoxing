async page => {
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 const login=async()=>{await page.goto('http://localhost:5174/#/pages/login/index');await page.reload();await page.locator('.login-panel').waitFor();await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});};
 await page.evaluate(()=>sessionStorage.clear());await login();
 await page.getByText('没有账号？注册账号',{exact:true}).click();await page.locator('.registration-fields').waitFor();
 if(!page.url().includes('/pages/register/index'))throw Error('registration did not navigate');
 await page.screenshot({path:'output/playwright/account130-register.png'});
 await login();await page.getByText('忘记密码？',{exact:true}).click();await page.locator('.reset-card').waitFor();
 if(!page.url().includes('/pages/reset-password/index'))throw Error('reset did not navigate');
 await page.screenshot({path:'output/playwright/account130-reset.png'});
 await login();await page.getByText('邮箱验证码登录',{exact:true}).click();
 await page.locator('.login-panel input').nth(0).fill('one@example.com');await page.locator('.code-input input').fill('123456');
 const attempts=[];await page.route('**/api/app/account/email-login',async route=>{attempts.push(route.request().headers().confirm||'');await route.fulfill({status:400,json:{error:'need_confirm_login'}})});
 await page.locator('.login-panel .login-submit').click();await page.getByText('继续登录',{exact:true}).waitFor();
 const boxes=await page.locator('.dialog-actions .login-submit').evaluateAll(els=>els.map(e=>({w:e.getBoundingClientRect().width,h:e.getBoundingClientRect().height})));
 if(boxes.length!==2||Math.abs(boxes[0].w-boxes[1].w)>1||boxes[0].h!==boxes[1].h)throw Error(JSON.stringify(boxes));
 await page.screenshot({path:'output/playwright/account130-confirm.png'});await page.getByText('取消',{exact:true}).click();if(attempts.length!==1)throw Error('cancel resubmitted');
 await page.evaluate(()=>sessionStorage.setItem('xiaoxing:session',JSON.stringify({token:'fixture',refresh:'fixture',userId:'9001',expiresAt:1999999999999})));
 await page.goto('http://localhost:5174/#/pages/settings/index');await page.reload();await page.getByText('注销账号',{exact:true}).waitFor();await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});
 await page.getByText('注销账号',{exact:true}).click();await page.getByText('继续注销',{exact:true}).click();await page.getByText('请输入当前密码确认身份',{exact:true}).waitFor();
 await page.screenshot({path:'output/playwright/account130-close.png'});await page.getByText('取消',{exact:true}).click();
 if(errors.length)throw Error(errors.join('\n'));return {boxes,errors};
}