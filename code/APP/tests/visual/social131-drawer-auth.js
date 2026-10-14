async page => {
 const assert=(v,m)=>{if(!v)throw Error(m)};
 await page.locator('.dock').getByText('AI助手',{exact:true}).click();
 await page.locator('.toolbar').first().click();
 await page.locator('.drawer-card').first().waitFor();
 await page.evaluate(async()=>Promise.all(Array.from(document.images).map(img=>img.decode().catch(()=>{}))));
 await page.waitForTimeout(350);
 await page.screenshot({path:'output/playwright/social131-drawer.png'});
 const gesture=async(dx,dy)=>page.locator('.drawer .scroll').evaluate((el,{dx,dy})=>{
  const send=(type,x,y)=>{const t=new Touch({identifier:1,target:el,clientX:x,clientY:y,pageX:x,pageY:y});el.dispatchEvent(new TouchEvent(type,{bubbles:true,touches:type==='touchend'?[]:[t],changedTouches:[t]}))};
  send('touchstart',260,450);send('touchmove',260+dx/2,450+dy/2);send('touchend',260+dx,450+dy);
 },{dx,dy});
 await gesture(8,140);assert(await page.locator('.drawer').isVisible(),'vertical scroll keeps drawer open');
 await gesture(-170,8);await page.locator('.drawer').waitFor({state:'detached'});
 assert(page.url().includes('/chat/'),'swipe does not open a card');
 await page.locator('.dock').getByText('事件',{exact:true}).click();
 await page.setViewportSize({width:320,height:740});
 await page.evaluate(async()=>Promise.all(Array.from(document.images).map(img=>img.decode().catch(()=>{}))));
 await page.waitForTimeout(350);
 assert(await page.evaluate(()=>document.documentElement.scrollWidth<=320),'no horizontal overflow');
 await page.screenshot({path:'output/playwright/social131-events-320.png'});
 // Simulate the exact screenshot business error, including HTTP 200.
 await page.route('**/api/**',route=>{
  if(route.request().url().includes('/@fs/') || route.request().url().split('?')[0].endsWith('.uts'))return route.continue();
  return route.fulfill({json:{code:500,msg:'认证信息错误或无效'}});
 });
 await page.locator('.filter').click();
 await page.getByRole('button',{name:'登录',exact:true}).waitFor();
 assert(await page.evaluate(()=>sessionStorage.getItem('xiaoxing:session'))==='','expired credentials removed');
 console.log('PASS drawer vertical/close gestures, narrow viewport, business authentication redirect');
}
