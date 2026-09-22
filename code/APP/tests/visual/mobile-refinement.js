async page => {
 const base=page.url().split('/').slice(0,3).join('/');
 const rows=Array.from({length:22},(_,i)=>({id:String(800+i),messageRole:i%2?'assistant':'user',messageType:'TEXT',content:'第'+i+'条对话：请确认今天的工作安排。',payload:{}}));
 rows.push({id:'long',messageRole:'assistant',messageType:'REMINDER',eventId:'201',content:'请完成今天的会议文稿。',payload:{eventSummary:'请在周二下班前完成会议文稿、整理各项工作安排，并确认明天的会议时间与参会人员。'.repeat(5)}});
 rows.push({id:'thinking',messageRole:'assistant',messageType:'TEXT',content:'已确认你的安排。',payload:{reasoningContent:'确认事件时间和接收人。'}});
 await page.route('**/api/**/chat/messages?*',r=>r.fulfill({json:{code:200,data:rows}}));
 await page.route('**/api/**/chat/sync?*',r=>r.fulfill({json:{code:200,data:{revision:'test',messages:rows}}}));
 await page.goto(base+'/#/pages/chat/index');await page.reload();
 await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});
 await page.getByText('已确认你的安排。',{exact:true}).waitFor();await page.waitForTimeout(2200);
 const related=await page.locator('.related-summary').evaluate(el=>({height:el.clientHeight,full:el.scrollHeight,clamp:getComputedStyle(el).webkitLineClamp}));
 if(related.height>55||related.height<50||related.full<=related.height||related.clamp!=='3')throw Error('Three-line clamp failed '+JSON.stringify(related));
 const composer=await page.locator('.composer').boundingBox();
 if(composer.height>140)throw Error('Empty composer too high '+composer.height);
 const separator=await page.locator('.reply-after-thinking').last().evaluate(el=>({margin:getComputedStyle(el).marginTop,padding:getComputedStyle(el).paddingTop}));
 if(separator.margin!=='4px'||separator.padding!=='6px')throw Error('Thinking separator spacing wrong');
 await page.screenshot({path:'output/playwright/mobile113-chat.png'});
 const scroller=page.locator('#chat-scroll .uni-scroll-view').last();
 await page.locator('#chat-scroll').dispatchEvent('touchstart');
 await scroller.evaluate(el=>el.scrollTop=0);await page.waitForTimeout(600);
 if(await scroller.evaluate(el=>el.scrollTop)>20)throw Error('Manual chat scroll was reset');
 await page.goto(base+'/#/pages/events/index');await page.reload();await page.locator('.filter-label').waitFor();
 const filter=await page.locator('.filter-label').evaluate(el=>getComputedStyle(el).fontWeight);
 await page.goto(base+'/#/pages/me/index');await page.reload();await page.locator('.logout-label').waitFor();
 const logout=await page.locator('.logout-label').evaluate(el=>getComputedStyle(el).fontWeight);
 if(filter!=='700'||logout!=='700')throw Error('Text weight incorrect');
 return {related,composerHeight:composer.height,separator,filter,logout,manualScroll:true};
}
