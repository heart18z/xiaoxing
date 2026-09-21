async page => {
 const errors=[];page.on('pageerror', e=>errors.push(e.message));
 const go=async name=>{await page.goto('http://localhost:5174/#/pages/'+name+'/index'+(name==='event-detail'?'?id=201':''));await page.reload();await page.locator('.frame').waitFor();await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});await page.waitForTimeout(150);};
 await go('me');
 const card=page.locator('.friend-card');
 const before=await card.boundingBox(), searchBefore=await page.locator('.friend-search').boundingBox();
 const firstBefore=await page.locator('.friend-row').first().boundingBox();
 await page.locator('.friend-scroll .uni-scroll-view').last().evaluate(el=>el.scrollTop=el.scrollHeight);
 await page.waitForTimeout(200);
 const after=await card.boundingBox(), firstAfter=await page.locator('.friend-row').first().boundingBox(), searchAfter=await page.locator('.friend-search').boundingBox();
 if(Math.abs(before.y-after.y)>1||Math.abs(before.height-after.height)>1||Math.abs(searchBefore.y-searchAfter.y)>1||firstBefore.y-firstAfter.y<100)throw Error('Friend card/search must stay still while rows scroll');
 await page.screenshot({path:'output/playwright/round2-friends.png'});
 const last=await page.locator('.friend-row').last().boundingBox();
 if(last.y<after.y||last.y+last.height>after.y+after.height+2)throw Error('Last friend is not visible at scroll bottom');
 await page.locator('.friend-input input').fill('谢鑫');await page.waitForTimeout(250);
 if(await page.locator('.friend-row').count()!==1)throw Error('Filtered friend list is incorrect');
 const filtered=await page.locator('.friend-row').first().boundingBox();
 if(filtered.y<after.y||filtered.y>after.y+40)throw Error('Search kept stale scroll position');
 await page.locator('.friend-input input').fill('');await page.waitForTimeout(200);

 await go('settings');
 const nextCard=page.locator('.model-card').first();const y=(await nextCard.boundingBox()).y;
 await page.locator('.select-field').first().click();await page.waitForTimeout(250);
 if(Math.abs((await nextCard.boundingBox()).y-y)>1)throw Error('Dropdown changed document layout');
 await page.getByText('English',{exact:true}).click();await page.waitForTimeout(250);
 if(!(await page.locator('.select-field').first().innerText()).includes('English'))throw Error('Second language option cannot be selected');
 await page.locator('.select-field').first().click();await page.waitForTimeout(250);await page.locator('.option').first().click();
 await go('event-detail');await page.getByText('查看对话',{exact:true}).first().click();await page.waitForTimeout(300);
 const bubbles=await page.locator('.dialog-assistant-bubble').evaluateAll(es=>es.map(el=>({width:el.getBoundingClientRect().width,row:el.parentElement.getBoundingClientRect().width})));
 if(!bubbles.length||bubbles.some(b=>b.width/b.row<.75))throw Error('Assistant event bubbles are too narrow');
 await page.screenshot({path:'output/playwright/round2-conversation.png'});
 return {errors,friendCardStationary:true,languageOverlayAndSelection:true,assistantBubbles:bubbles};
}
