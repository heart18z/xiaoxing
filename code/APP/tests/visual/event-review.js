async page => {
  const checks=[],errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto('http://localhost:5174/#/pages/events/index');await page.locator('.event-card').first().waitFor();
  const phases=await page.locator('uni-scroll-view.scroll').evaluate(async el=>{
    const start=performance.now(),out=[];el.__vueParentComponent.proxy.$emit('refresherrefresh');
    for(let i=0;i<32;i++){await new Promise(r=>setTimeout(r,50));out.push({time:performance.now()-start,text:document.querySelector('.refresh-label').textContent});}return out;
  });
  const complete=phases.find(x=>x.text==='事件已更新')?.time;
  if(!complete||complete<800||!phases.some(x=>x.text==='下拉刷新，松开更新'))throw Error('Refresh phase duration failed '+JSON.stringify(phases));
  checks.push('刷新动画至少850ms并显示完成状态400ms');
  const branches=[1,2,3].map(n=>({id:'b'+n,recipientUserId:String(n),name:'接收人'+n,branchStatus:'ACTIVE',latestSummary:'审核合同',currentFact:'进展'+n}));
  const timeline=[{id:'1',nodeType:'EVENT_UPDATED',content:'全局更新'},{id:'2',branchId:'b2',nodeType:'EVENT_UPDATED',content:'其他分支更新'},...['10:00:01','10:00:15'].map((time,i)=>({id:String(i+3),branchId:'b1',nodeType:'NOTIFICATION_READ',content:'已阅读提醒',actorUserId:'1',createTime:'2026-09-20 '+time})),{id:'5',branchId:'b1',nodeType:'ASK_RECIPIENT',content:'审核完成了吗？'}];
  await page.route('**/api/app/reminder/event/detail**',r=>r.fulfill({json:{code:200,data:{creator:true,event:{id:'201',event_no:'TEST',latest_summary:'',event_summary:'概述'.repeat(60),event_status:'ACTIVE'},branches,timeline}}}));
  await page.locator('.event-card').first().click();await page.getByText('展开完整概述',{exact:true}).waitFor();
  if(await page.locator('.progress-copy').count()!==2)throw Error('Progress default should show two');
  await page.getByText('查看全部进展',{exact:true}).click();if(await page.locator('.progress-copy').count()!==3)throw Error('Progress expand failed');
  await page.getByText('展开完整概述',{exact:true}).click();if(await page.locator('.overview-collapsed').count())throw Error('Overview did not expand');
  await page.locator('.timeline-tab').filter({hasText:'接收人1'}).click();
  if(await page.locator('.node-copy').filter({hasText:'其他分支更新'}).count())throw Error('Wrong branch shown');
  if(await page.locator('.node-copy').filter({hasText:'已阅读提醒'}).count()!==1)throw Error('Read receipts not deduplicated');
  await page.locator('.node-copy').filter({hasText:'全局更新'}).waitFor();checks.push('概述回退展开、进展展开、时间轴分支筛选与已读去重');
  await page.route('**/api/app/reminder/chat/messages**',r=>r.fulfill({json:{code:200,data:[{id:'notice-review',messageRole:'assistant',messageType:'QUESTION',content:'审核完成了吗？',eventId:'201',payload:{eventSummary:'审核合同'}}]}}));
  await page.route('**/api/app/reminder/chat/sync**',r=>r.fulfill({json:{code:200,data:{revision:'review'}}}));
  await page.goto('http://localhost:5174/#/pages/chat/index');await page.locator('.notice').filter({hasText:'审核完成了吗？'}).waitFor();checks.push('AI评估QUESTION呈现为异常提醒卡片');
  await page.screenshot({path:'output/playwright/event-review-notice.png'});
  if(errors.length)throw Error(errors.join(';'));
  return {checks,refreshCompleteMs:complete,errors};
}
