async page=>{
  const origin='http://127.0.0.1:4189',errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.context().addCookies([{name:'saber-access-token',value:'offline-admin-fixture',url:origin}]);
  await page.addInitScript(()=>{
    localStorage.setItem('saber-token',JSON.stringify({dataType:'string',content:'offline-admin-fixture'}));
    localStorage.setItem('saber-userInfo',JSON.stringify({dataType:'object',content:{userId:'1',user_id:'1',user_name:'布局测试',userName:'布局测试',role_id:'1',role_name:'administrator',dept_id:'1',dept_name:'测试',roleId:'1',roleName:'administrator',deptId:'1',deptName:'测试'}}));
  });
  await page.route('**/api/**',async route=>{
    const url=route.request().url();let data=[];
    if(url.includes('/dashboard'))data={activeEvents:0,activeBranches:0,notificationsToday:0,failedNotifications:0,upcomingEvaluations:[],recentNotifications:[]};
    if(url.includes('/param'))data={};
    if(url.includes('/blade-system/user/info'))data={roleId:'1',roleName:'administrator',deptId:'1',deptName:'测试'};
    await route.fulfill({json:{code:200,success:true,data}});
  });
  await page.setViewportSize({width:1440,height:1000});await page.goto(origin+'/wel/index');await page.locator('.avue-layout').waitFor();await page.waitForTimeout(1200);
  const measure=()=>page.evaluate(()=>Object.fromEntries(['.route-stage','.route-stage > div','.avue-contail','.avue-layout','.avue-main','#avue-view'].map(s=>{const e=document.querySelector(s),b=e.getBoundingClientRect();return[s,{top:b.top,bottom:b.bottom,height:b.height}];})));
  const fixed=await measure();
  for(const s of ['.route-stage','.route-stage > div','.avue-contail','.avue-layout','.avue-main'])if(Math.abs(fixed[s].height-1000)>2)throw Error(s+' does not fill viewport: '+JSON.stringify(fixed));
  await page.screenshot({path:'output/playwright/admin-height-fixed-1440.png'});
  await page.addStyleTag({content:'.route-stage:not(.mobile-route-stage){height:auto!important;min-height:100%!important}'});
  const before=await measure();
  if(before['.avue-layout'].height>=998)throw Error('old-height regression not reproduced');
  await page.screenshot({path:'output/playwright/admin-height-before-1440.png'});
  if(errors.length)throw Error(errors.join(';'));return{fixed,before,errors,checks:['admin height fills viewport','previous auto-height reproduces collapsed percentage chain']};
}
