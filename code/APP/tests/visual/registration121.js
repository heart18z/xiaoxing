async page => {
 let registrations=0,logins=0;
 await page.route('**/api/app/account/check-contact',r=>r.fulfill({json:{code:200,data:{available:true}}}));
 await page.route('**/api/app/account/register',async route=>{registrations++;await route.fulfill({json:{code:200,data:true}})});
 await page.route('**/api/blade-auth/oauth/token**',async route=>{logins++;await route.fulfill({json:{access_token:'local-fixture',refresh_token:'local-fixture',user_id:'9001',role_name:'app_user',expires_in:3600}})});
 await page.getByText('没有账号？注册账号',{exact:true}).click();
 const form=page.locator('.account-panel');await form.waitFor();
 const inputs=form.locator('input');await inputs.nth(0).fill('fixture_user');await inputs.nth(1).fill('测试注册');await inputs.nth(2).fill('FixturePassword123');await inputs.nth(4).fill('fixture@example.com');await inputs.nth(5).fill('123456');
 await form.getByRole('button',{name:'注册',exact:true}).click();
 await page.waitForURL('**/pages/chat/index');
 if(registrations!==1||logins!==1)throw Error(JSON.stringify({registrations,logins}));
 return {registrations,logins,destination:page.url()};
}
