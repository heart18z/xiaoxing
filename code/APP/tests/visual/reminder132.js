async page => {
 const assert=(v,m)=>{if(!v)throw Error(m)};
 await page.evaluate(async()=>Promise.all(Array.from(document.images).map(img=>img.decode().catch(()=>{}))));
 assert(await page.locator('.event-card').first().locator('.people-more').innerText()==='+2','six recipients show four avatars and +2');
 await page.screenshot({path:'output/playwright/reminder132-events.png'});
 await page.locator('.dock').getByText('AI助手',{exact:true}).click();
 await page.locator('.toolbar').first().click();
 await page.locator('.drawer-card').first().waitFor();
 await page.addStyleTag({content:'.drawer-sheen{height:230px!important;flex-shrink:1!important}.drawer-safe-top{height:20px!important;flex-shrink:0}'});
 await page.evaluate(async()=>Promise.all(Array.from(document.images).map(img=>img.decode().catch(()=>{}))));
 await page.waitForTimeout(350);
 assert(await page.locator('.group-description').count()===4,'four group descriptions');
 assert(await page.locator('.group-later .drawer-card').count()===0,'empty group has header only');
 assert(!(await page.locator('.drawer').innerText()).includes('暂无提醒'),'no empty row under group header');
 await page.screenshot({path:'output/playwright/reminder132-drawer.png'});
 await page.setViewportSize({width:320,height:740});
 await page.waitForTimeout(100);
 assert(await page.evaluate(()=>document.documentElement.scrollWidth<=320),'no page horizontal overflow');
 assert(await page.locator('.drawer-card').first().evaluate(el=>el.scrollWidth<=el.clientWidth+2),'card fits narrow width');
 await page.screenshot({path:'output/playwright/reminder132-drawer-320.png'});
 const gesture=async(dx,dy)=>page.locator('.drawer .scroll').evaluate((el,{dx,dy})=>{
  const send=(type,x,y)=>{const t=new Touch({identifier:1,target:el,clientX:x,clientY:y,pageX:x,pageY:y});el.dispatchEvent(new TouchEvent(type,{bubbles:true,touches:type==='touchend'?[]:[t],changedTouches:[t]}))};
  send('touchstart',240,400);send('touchmove',240+dx/2,400+dy/2);send('touchend',240+dx,400+dy);
 },{dx,dy});
 await gesture(8,120);assert(await page.locator('.drawer').isVisible(),'vertical scrolling retains drawer');
 await gesture(-150,8);await page.locator('.drawer').waitFor({state:'detached'});
 assert(page.url().includes('/chat/'),'swipe dismiss does not open a card');
 console.log('PASS +2, four groups, empty header, narrow cards, vertical scroll and swipe dismiss');
}
