// Run after replica-fixture.js. All API responses remain local fixtures.
async page => {
  const base = page.url().split('/').slice(0,3).join('/');
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  const go = async name => {
    await page.goto(base + '/#/pages/' + name + '/index');
    await page.reload();
    await page.locator('.frame').waitFor();
    await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});
  };
  const event = {id:'201',eventNo:'SR20260922001',eventSummary:'今天下班前完成会议文稿，并确认明天的安排',eventStatus:'ACTIVE',creatorName:'陈颖',recipientCount:1,activeBranchCount:1,createTime:'2026-09-22 09:00:00',nextEvaluateTime:'2026-09-22 18:00:00'};
  await page.route('**/api/**/events?*', route => route.fulfill({json:{code:200,data:[
    {...event,latestFact:''}, {...event,id:'202',latestFact:'已完成第一版文稿，等待复核'},
  ]}}));
  await go('events');
  await page.getByText('暂无反馈，等待下一次评估',{exact:true}).waitFor();
  await page.getByText('已完成第一版文稿，等待复核',{exact:true}).waitFor();
  const tabWeight = await page.locator('.tab-text').first().evaluate(el=>getComputedStyle(el).fontWeight);
  if(tabWeight !== '700')throw Error('Event tab labels are too light');
  await page.screenshot({path:'output/playwright/ios109-events.png'});

  await go('me');
  await page.locator('.friend-row').first().waitFor();
  const friendStyles = await page.locator('.friend-row').first().evaluate(el=>['.permission-tag','.manage'].map(sel=>{
    const style=getComputedStyle(el.querySelector(sel));return {background:style.backgroundColor,weight:style.fontWeight};
  }));
  if(friendStyles.some(style=>style.weight!=='700'||style.background!=='rgb(224, 230, 255)'))throw Error('Friend styles do not match');
  await page.screenshot({path:'output/playwright/ios109-friends.png'});

  const messages=Array.from({length:30},(_,i)=>({id:String(100+i),messageRole:i%2?'assistant':'user',messageType:'TEXT',content:'第'+i+'条对话：请帮我确认今天的工作安排，并在合适时间提醒我。',payload:{},createTime:'2026-09-22 09:00:00'}));
  messages.push({id:'140',messageRole:'assistant',messageType:'REMINDER',eventId:'201',content:'11点，记得点外卖。',payload:{eventSummary:'提醒我今天上午十一点订外卖并确认会议室、投影仪以及其他明天会议所需的全部资料'},createTime:'2026-09-22 09:00:00'});
  messages.push({id:'141',messageRole:'assistant',messageType:'TEXT',content:'这是最新一条回复。',payload:{reasoningContent:'已经确认你的安排。'},createTime:'2026-09-22 09:01:00'});
  await page.route('**/api/**/chat/messages?*', async route=>{
    await page.waitForTimeout(650);
    await route.fulfill({json:{code:200,data:messages}});
  });
  await go('chat');
  await page.getByText('这是最新一条回复。',{exact:true}).waitFor();
  await page.waitForTimeout(2300);
  const scroller=page.locator('#chat-scroll .uni-scroll-view').last();
  const bottomGap=await scroller.evaluate(el=>el.scrollHeight-el.scrollTop-el.clientHeight);
  if(bottomGap>30)throw Error('Chat did not settle at latest: '+bottomGap);
  const related=await page.locator('.related').evaluate(el=>{
    const label=el.querySelector('.related-label'),summary=el.querySelector('.related-summary');
    return {labelX:label.getBoundingClientRect().x,summaryX:summary.getBoundingClientRect().x,client:summary.clientWidth,scroll:summary.scrollWidth,ellipsis:getComputedStyle(summary).textOverflow};
  });
  if(related.labelX>=related.summaryX||related.scroll<=related.client||related.ellipsis!=='ellipsis')throw Error('Related event must have a left label and truncated summary');
  const icon=await page.locator('.notice-label .ui-icon').boundingBox();
  if(icon.width!==16||icon.height!==16)throw Error('Reminder icon size incorrect');
  await page.screenshot({path:'output/playwright/ios109-chat.png'});

  // During startup settling, a deliberate touch must allow reading old messages.
  await go('chat');
  await page.getByText('这是最新一条回复。',{exact:true}).waitFor();
  await page.locator('#chat-scroll').dispatchEvent('touchstart');
  await scroller.evaluate(el=>el.scrollTop=0);
  await page.waitForTimeout(2200);
  if(await scroller.evaluate(el=>el.scrollTop)>20)throw Error('Startup scroll fought a user reading history');
  if(errors.length)throw Error(errors.join('\n'));
  return {eventProgress:true,tabWeight,friendStyles,bottomGap,related,manualScrollPreserved:true};
}
