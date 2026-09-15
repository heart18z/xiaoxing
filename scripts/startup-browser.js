async page=>{
  let release;const gate=new Promise(resolve=>release=resolve);
  await page.setViewportSize({width:390,height:844});
  await page.route('**/*.js',async route=>{
    if(route.request().url().endsWith('/startup.js')){await route.continue();return;}
    await gate;await route.abort();
  });
  await page.goto('http://127.0.0.1:4188/app/login',{waitUntil:'domcontentloaded',timeout:5000}).catch(()=>{});
  await page.getByRole('status',{name:'应用启动'}).waitFor();
  await page.screenshot({path:'output/playwright/startup-loading.png'});
  await page.getByText('加载比平时稍久，请稍候或重新加载').waitFor({timeout:20000});
  release();await page.getByText('暂时无法打开，请检查网络后重试').waitFor();
  await page.screenshot({path:'output/playwright/startup-failed.png'});
  await page.unroute('**/*.js');
  await page.getByRole('button',{name:'重新加载',exact:true}).click();
  await page.getByRole('button',{name:'登录',exact:true}).waitFor({timeout:30000});
  await page.waitForTimeout(400);
  if(await page.locator('#xiaoxing-startup').count())throw Error('startup cover remained after login rendered');
  return{loading:true,slow:true,failure:true,retry:true,coverRemoved:true};
}
