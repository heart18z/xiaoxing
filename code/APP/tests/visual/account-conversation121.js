async page => {
 await page.evaluate(()=>sessionStorage.setItem('xiaoxing:session',JSON.stringify({token:'local-fixture',refresh:'local-fixture',userId:'9001',expiresAt:1999999999999})));
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto('http://localhost:5174/#/pages/event-detail/index?id=201');await page.reload();
 await page.getByText('查看对话',{exact:true}).first().click();
 await page.getByText('现在10点了，该审核第一版标书了。',{exact:true}).waitFor();
 const nested=await page.locator('.conversation-panel .popup-scroll').count();if(nested)throw Error('nested scroll owner');
 const panel=await page.locator('.conversation-panel').boundingBox();if(panel.y+panel.height>874)throw Error('popup overflow');
 await page.screenshot({path:'output/playwright/mobile121-conversation.png'});
 await page.locator('.conversation-panel .close').click();
 await page.getByText('查看对话',{exact:true}).first().click();await page.locator('.conversation-panel .close').click();
 await page.waitForTimeout(350);if(await page.locator('.conversation-panel').count())throw Error('closed popup remounted');
 await page.goto('http://localhost:5174/#/pages/chat/index');await page.reload();await page.locator('.orb').waitFor();await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});
 const orb=await page.locator('.orb').evaluate(e=>({width:e.getBoundingClientRect().width,height:e.getBoundingClientRect().height,radius:getComputedStyle(e).borderRadius,shrink:getComputedStyle(e).flexShrink}));
 if(orb.width!==24||orb.height!==24||orb.radius!=='0px'||orb.shrink!=='0')throw Error(JSON.stringify(orb));
 await page.screenshot({path:'output/playwright/mobile121-orb.png'});
 await page.evaluate(()=>sessionStorage.clear());await page.goto('http://localhost:5174/#/pages/login/index');await page.reload();await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});
 await page.getByText('没有账号？注册账号',{exact:true}).click();
 await page.locator('.account-panel').waitFor();
 return {panel,orb,errors};
}
