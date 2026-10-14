async page => {
 const assert=(v,m)=>{if(!v)throw Error(m)};
 await page.evaluate(async()=>Promise.all(Array.from(document.images).map(img=>img.decode().catch(()=>{}))));
 assert(await page.locator('.event-card').first().locator('.people-more').innerText()==='+2','remaining recipients retained');
 await page.screenshot({path:'output/playwright/reminder133-events.png'});
 await page.setViewportSize({width:320,height:740});
 assert(await page.locator('.people').first().evaluate(el=>el.scrollWidth<=el.clientWidth+2),'avatar row fits narrow cards');
 await page.screenshot({path:'output/playwright/reminder133-events-320.png'});
 await page.setViewportSize({width:402,height:874});
 await page.locator('.dock').getByText('AI助手',{exact:true}).click();
 const open=async()=>{
  await page.locator('.toolbar').first().click();
  await page.locator('.drawer-card').first().waitFor();
  await page.evaluate(async()=>Promise.all(Array.from(document.images).map(img=>img.decode().catch(()=>{}))));
  await page.waitForTimeout(350);
 };
 await open();
 const box=await page.locator('.drawer').boundingBox();
 assert(box.width<370&&box.width>350,'drawer leaves visible outside area');
 assert(await page.locator('.drawer-bubble').count()===3,'three decorative bubbles');
 await page.screenshot({path:'output/playwright/reminder133-drawer.png'});
 await page.mouse.click(390,440);
 await page.locator('.drawer').waitFor({state:'detached'});
 assert(page.url().includes('/chat/'),'outside tap closes without activating underlying page');
 await page.setViewportSize({width:320,height:740});
 await open();
 assert(await page.evaluate(()=>document.documentElement.scrollWidth<=320),'no horizontal page overflow');
 assert(await page.locator('.drawer-tabs').evaluate(el=>el.scrollWidth<=el.clientWidth+2),'all filters fit narrow drawer');
 assert(await page.locator('.drawer-card').first().evaluate(el=>el.scrollWidth<=el.clientWidth+2),'card fits narrow drawer');
 await page.screenshot({path:'output/playwright/reminder133-drawer-320.png'});
 const gesture=async(dx,dy)=>page.locator('.drawer .scroll').evaluate((el,{dx,dy})=>{
  const send=(type,x,y)=>{const t=new Touch({identifier:1,target:el,clientX:x,clientY:y,pageX:x,pageY:y});el.dispatchEvent(new TouchEvent(type,{bubbles:true,touches:type==='touchend'?[]:[t],changedTouches:[t]}))};
  send('touchstart',230,400);send('touchmove',230+dx/2,400+dy/2);send('touchend',230+dx,400+dy);
 },{dx,dy});
 await gesture(8,120);assert(await page.locator('.drawer').isVisible(),'vertical scrolling retains drawer');
 await gesture(-150,8);await page.locator('.drawer').waitFor({state:'detached'});
 assert(page.url().includes('/chat/'),'swipe does not open a card');
 console.log('PASS circular avatars, overflow count, outside tap, narrow drawer filters/cards, swipe dismiss');
}
