// Run only after replica-fixture.js in an isolated local browser session.
async page => {
 const origin='http://localhost:5174',checks=[],errors=[];
 if(!page.url().startsWith(origin))throw Error('Local fixture session required');
 page.on('pageerror',e=>errors.push(e.message));
 const safe='.frame>uni-view:nth-child(2){height:62px!important;flex-shrink:0}.dock{height:102px!important;padding-bottom:34px!important}.brand{margin-top:62px!important}';
 const shot=async name=>{await page.waitForLoadState('networkidle');await page.waitForTimeout(250);return page.screenshot({path:'output/playwright/refinement-'+name+'.png'});};
 let rows=[{id:'r1',messageRole:'user',messageType:'TEXT',content:'你好'},{id:'r2',messageRole:'assistant',messageType:'TEXT',content:'你好，我可以帮你创建、修改或停止提醒，也可以记录进度。',reasoningContent:'正在理解你的消息，确认你需要的是日常对话还是新的提醒安排。\n我会结合已有事件和反馈，帮助你管理接下来的计划。'.repeat(5)}];
 await page.route('**/api/app/reminder/chat/messages**',r=>r.fulfill({json:{code:200,data:rows}}));
 const go=async()=>{await page.goto(origin+'/#/pages/chat/index');await page.reload();await page.locator('.message').first().waitFor();await page.addStyleTag({content:safe});await page.locator('.message-avatar').evaluateAll(async images=>{await Promise.all(images.flatMap(e=>Array.from(e.querySelectorAll('img'))).map(img=>img.decode().catch(()=>{})))});await page.waitForTimeout(150);};
 await go();await page.locator('.thinking-toggle').click();await page.locator('.thinking-content').waitFor();
 if(await page.locator('.mask').count())throw Error('Thinking opened a modal');
 const height=await page.locator('.thinking-content').evaluate(e=>e.getBoundingClientRect().height);
 if(height<30||height>140)throw Error('Invalid thinking scroll height '+height);
 await shot('thinking-open');await page.locator('.thinking-toggle').click();
 if(await page.locator('.thinking-content').count())throw Error('Thinking did not collapse');
 checks.push('Inline thinking expands, scrolls and collapses');await shot('thinking-closed');
 const kinds=['REMINDER','FEEDBACK','QUESTION','CONFLICT','EVENT_ASSIGNED','SYSTEM'];
 for(const kind of kinds){
  rows=[{id:kind,messageRole:'assistant',messageType:kind,eventId:'201',createTime:'2026-09-18 13:17:27',content:kind==='REMINDER'?'现在10点了，该审核第一版标书了。':'第一版标书审核已完成，请确认后续安排。',payload:{fact:'审核完成，已提交修改意见。',actor:'陈通',eventSummary:'9月18日上午10点审核第一版标书',eventIds:kind==='SYSTEM'?['201']:[]}}];
  await go();await shot(kind.toLowerCase());
  if(kind==='FEEDBACK')await page.getByText('反馈人：陈通',{exact:true}).waitFor();
 }
 const event={summary:'审核第一版标书',eventTime:'2026-09-19 10:00:00',timeDescription:'明天上午10点',firstEvaluateTime:'2026-09-19 09:00:00',recipients:[{name:'陈通'}]};
 for(const kind of ['CANDIDATE','CANDIDATE_CONFIRMED','CANDIDATE_CANCELLED','CANDIDATE_EXPIRED','macro','conflict']){
  const item={...event,...(kind==='macro'?{eventTime:'',timeDescription:'长期跟进项目投标'}:{}),...(kind==='conflict'?{scheduleConflicts:[{eventId:'202'}]}:{})};
  rows=[{id:kind,messageRole:'assistant',messageType:kind==='macro'||kind==='conflict'?'CANDIDATE':kind,content:'我已整理好提醒安排，请核对。',payload:{candidateId:'c1',events:[item]}}];
  await go();await shot(kind.toLowerCase());
  if(kind==='conflict')await page.getByText('仍要安排',{exact:true}).waitFor();
  if(kind==='macro')await page.getByText('确认开始跟进',{exact:true}).waitFor();
 }
 checks.push('Six message categories and six candidate states');
 await page.evaluate(()=>sessionStorage.clear());
 await page.goto(origin+'/#/pages/login/index');await page.reload();await page.locator('.login-panel').waitFor();await page.addStyleTag({content:safe});
 if(await page.locator('.brand-spark').count()!==2)throw Error('Missing brand decoration');
 await shot('login');await page.getByText('忘记密码？',{exact:true}).click();await page.getByText('请联系管理员重置密码，然后使用新密码登录。',{exact:true}).waitFor();await shot('forgot');await page.getByText('知道了',{exact:true}).click();
 const requests=[];
 await page.route('**/api/blade-auth/oauth/token**',async route=>{
  const headers=route.request().headers();requests.push(headers.confirm||'');
  await route.fulfill({status:headers.confirm==='true'?200:401,json:headers.confirm==='true'?{access_token:'local-fixture',refresh_token:'local-fixture',user_id:'9001',role_name:'app_user',expires_in:3600}:{error:'need_confirm_login',error_description:'账号已在其他地址登录，是否继续？'}});
 });
 await page.locator('.login-panel input').nth(0).fill('fixture-user');await page.locator('.login-panel input').nth(1).fill('fixture-password');await page.locator('.login-panel .primary').click();
 await page.getByText('账号已在其他设备登录，继续将退出其他设备。',{exact:true}).waitFor();await shot('confirm-login');await page.getByText('取消',{exact:true}).click();
 if(requests.length!==1)throw Error('Cancel retried login');
 await page.locator('.login-panel .primary').click();await page.getByText('继续登录',{exact:true}).click();await page.waitForURL('**/pages/chat/index');
 if(JSON.stringify(requests)!==JSON.stringify(['','','true']))throw Error('Unexpected confirmation headers');
 checks.push('Cancel preserves existing login; continue sends confirm header once');
 if(errors.length)throw Error(JSON.stringify(errors));return {checks,errors};
}
