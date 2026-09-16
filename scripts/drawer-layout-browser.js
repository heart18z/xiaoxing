async page=>{
  const day=offset=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit'}).format(Date.now()+offset*86400000);
  const row=(id,summary,extra={})=>({eventId:id,branchId:id,summary,recipientName:'陈颖',creatorName:'李明',eventStatus:'ACTIVE',branchStatus:'ACTIVE',eventTime:day(0)+'T23:00:00',nextEvaluateTime:day(0)+'T22:00:00',...extra});
  const rows=[row('1','下午2点办理落户补贴'),row('2','晚上8点提交周报'),row('3','下午1点提交报告',{lastRemindedAt:day(0)+'T13:00:00',eventTime:null,nextEvaluateTime:null}),row('4','把合同发给小王',{eventTime:day(1)+'T10:00:00',nextEvaluateTime:day(1)+'T10:00:00'}),row('5','月底完成项目复盘',{eventTime:day(8)+'T18:00:00',nextEvaluateTime:day(8)+'T18:00:00'})];
  await page.route('**/events/drawer*',route=>route.fulfill({json:{code:200,success:true,data:route.request().url().includes('received')?[]:rows}}));
  await page.setViewportSize({width:390,height:844});await page.goto('http://127.0.0.1:4188/app/chat');
  await page.getByRole('button',{name:'查看我的事件'}).click();await page.getByText('下午2点办理落户补贴',{exact:true}).waitFor();await page.waitForTimeout(400);
  await page.screenshot({path:'output/playwright/reminder-drawer-polished.png'});
  await page.setViewportSize({width:320,height:740});await page.waitForTimeout(250);
  const result=await page.locator('.reminder-drawer').evaluate(el=>({width:el.getBoundingClientRect().width,viewport:innerWidth,overflow:el.scrollWidth>el.clientWidth}));
  if(result.overflow||result.width>result.viewport)throw Error('drawer overflows narrow device');
  await page.getByRole('button',{name:'选择日期范围'}).click();
  const calendar=page.getByRole('dialog',{name:'选择提醒日期'});
  await calendar.getByRole('button',{name:'明天',exact:true}).click();
  await page.screenshot({path:'output/playwright/reminder-drawer-narrow.png'});
  await calendar.getByRole('button',{name:'确认筛选',exact:true}).click();
  await page.getByText('把合同发给小王',{exact:true}).waitFor();
  await page.getByRole('button',{name:'还原'}).click();
  return{narrow:result,customCalendar:true};
}
