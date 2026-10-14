async page => {
 const assert=(v,m)=>{if(!v)throw Error(m)};
 const swipe=async(selector,x,y,dx,dy)=>page.locator(selector).first().evaluate((el,{x,y,dx,dy})=>{
  const send=(type,px,py)=>{const t=new Touch({identifier:1,target:el,clientX:px,clientY:py,pageX:px,pageY:py});el.dispatchEvent(new TouchEvent(type,{bubbles:true,cancelable:true,touches:type==='touchend'?[]:[t],changedTouches:[t]}))};
  send('touchstart',x,y);send('touchmove',x+dx/2,y+dy/2);send('touchend',x+dx,y+dy);
 },{x,y,dx,dy});
 assert(await page.locator('.event-card').first().locator('.person-avatar').count()===4,'four avatars');
 await page.screenshot({path:'output/playwright/social131-events.png'});
 await swipe('.scroll',280,450,-160,8);
 await page.waitForFunction(()=>document.querySelector('.event-tab.selected')?.textContent.includes('我收到的'));
 await page.waitForFunction(()=>document.querySelector('.people')?.textContent.includes('发起人：陈颖'));
 assert(await page.locator('.event-card').first().locator('.person-avatar').count()===1,'creator avatar');
 await swipe('.scroll',110,450,12,160);
 assert((await page.locator('.event-tab.selected').textContent()).includes('我收到的'),'vertical stays received');
 await swipe('.scroll',80,450,160,8);
 await page.waitForFunction(()=>document.querySelector('.event-tab.selected')?.textContent.includes('我发起的'));
 await page.locator('.dock').getByText('我的',{exact:true}).click();
 await page.getByText('管理',{exact:true}).first().click();
 await page.getByText('好友资料',{exact:true}).waitFor();
 assert((await page.locator('body').textContent()).includes('谢鑫'),'original real name fallback is visible');
 await page.evaluate(async()=>Promise.all(Array.from(document.images).map(img=>img.decode().catch(()=>{}))));
 await page.waitForTimeout(300);
 await page.screenshot({path:'output/playwright/social131-friend.png'});
 await page.locator('.popup-head .close').click();
 await page.locator('.mask').waitFor({state:'detached'});
 console.log('PASS avatars limit, creator, both swipe directions, vertical scroll, friend original name');
}
