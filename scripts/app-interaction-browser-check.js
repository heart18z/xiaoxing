async page=>{
  const origin='http://127.0.0.1:4188',errors=[],checks=[];let eventReads=0,slow=true,newUser=false;
  page.on('pageerror',e=>errors.push(e.message));
  await page.context().addCookies([{name:'saber-access-token',value:'offline-navigation-fixture',url:origin}]);
  await page.addInitScript(()=>localStorage.setItem('saber-token',JSON.stringify({dataType:'string',content:'offline-navigation-fixture'})));
  const messages=[{id:'1',messageRole:'assistant',messageType:'TEXT',content:'已记录。',reasoningContent:'用于检查折叠后的分隔线。',isRead:true}];
  await page.route('**/api/**',async route=>{
    const url=route.request().url();let data={};
    if(url.includes('/oauth/token')){newUser=true;await route.fulfill({json:{access_token:'offline-second-user',refresh_token:'offline-refresh',user_id:'2',user_name:'第二位测试用户',tenant_id:'000000',role_name:'app_user'}});return;}
    if(url.includes('/bootstrap'))data={user:{id:'1',name:'隔离交互测试',account:'fixture'},activeEventCount:30,sentActiveEventCount:30};
    else if(url.includes('/events?')){eventReads++;if(slow)await page.waitForTimeout(1500);data=Array.from({length:30},(_,i)=>({id:String(i+1),eventNo:'FIXTURE'+i,eventStatus:'ACTIVE',eventSummary:'测试事项 '+i,recipientCount:1,activeBranchCount:1}));}
    else if(url.includes('/chat/messages'))data=newUser?[]:messages;
    else if(url.includes('/chat/sync'))data={revision:'fixture',messages:newUser?[]:messages,unreadCount:0};
    else if(url.includes('/friends/list'))data=[];
    else if(url.includes('/friends/requests'))data={incoming:[],outgoing:[]};
    else if(url.includes('/settings/'))data={language:'zh-cn',models:[],llmMode:'SYSTEM',speechMode:'SYSTEM'};
    await route.fulfill({json:{code:200,success:true,data}});
  });
  await page.setViewportSize({width:390,height:844});await page.goto(origin+'/app/chat');
  const tab=name=>page.locator('.sr-shell:not([inert]):not(.mobile-route-leave-active) .sr-nav').getByRole('link',{name,exact:true});
  await page.locator('.thinking-toggle').waitFor();
  if(await page.locator('.thinking-toggle').getAttribute('aria-expanded')==='true')await page.locator('.thinking-toggle').click();
  const borders=await page.locator('.has-thinking').evaluate(el=>({header:getComputedStyle(el.querySelector('.thinking-toggle')).borderBottomWidth,body:getComputedStyle(el.querySelector('.markdown-content')).borderTopWidth}));
  if(parseFloat(borders.header)!==0||parseFloat(borders.body)<=0||parseFloat(borders.body)>1.1)throw Error('duplicate separator '+JSON.stringify(borders));
  await page.locator('.composer textarea').fill('切换后应该保留的草稿');
  await page.screenshot({path:'output/playwright/interaction-single-separator-390.png'});
  checks.push('collapsed thinking has exactly one separator');
  const eventsTab=tab('事件'),box=await eventsTab.boundingBox();
  await page.mouse.move(box.x+box.width/2,box.y+box.height/2);await page.mouse.down();await page.waitForTimeout(100);
  const pressed=await eventsTab.evaluate(el=>getComputedStyle(el).scale);
  if(Number(pressed)>.99)throw Error('tab has no press response');await page.mouse.up();
  await page.waitForURL('**/app/events');await page.locator('.mobile-page-loader').waitFor();
  const loader=await page.locator('.mobile-page-loader').evaluate(el=>({height:el.getBoundingClientRect().height,pointer:getComputedStyle(el).pointerEvents}));
  if(loader.height>4||loader.pointer!=='none')throw Error('loader blocks page');
  await page.screenshot({path:'output/playwright/interaction-slow-load-390.png'});
  await page.getByRole('heading',{name:'测试事项 0',exact:true}).waitFor();slow=false;
  await page.getByRole('searchbox',{name:'搜索事件',exact:true}).fill('测试事项');
  const scroller=page.locator('.event-list-scroll');await scroller.evaluate(el=>el.scrollTop=600);const scrollTop=await scroller.evaluate(el=>el.scrollTop);
  await tab('我的').click();await page.waitForURL('**/app/me');await page.waitForTimeout(240);
  const before=eventReads;await tab('事件').click();await page.waitForURL('**/app/events');await page.waitForTimeout(300);
  if(await page.getByRole('searchbox',{name:'搜索事件',exact:true}).inputValue()!=='测试事项')throw Error('event filter lost');
  if(Math.abs(await scroller.evaluate(el=>el.scrollTop)-scrollTop)>2)throw Error('event scroll lost');
  if(eventReads<=before)throw Error('event activation did not refresh');
  await tab('对话').click();await page.waitForURL('**/app/chat');await page.waitForTimeout(250);
  if(await page.locator('.composer textarea').inputValue()!=='切换后应该保留的草稿')throw Error('chat draft lost');
  checks.push('press feedback; non-blocking slow loader; tab cache preserves draft, filters and scroll; activation refresh');
  // Sample every frame while rapidly navigating: an opaque shell must always remain.
  await page.evaluate(()=>{window.__navFrames=[];window.__sampling=true;const tick=()=>{if(!window.__sampling)return;window.__navFrames.push([...document.querySelectorAll('.sr-shell')].filter(el=>getComputedStyle(el).display!=='none'&&Number(getComputedStyle(el).opacity)>.1).length);requestAnimationFrame(tick);};tick();});
  for(const name of ['事件','我的','对话','我的','事件','对话']){
    await tab(name).click({force:true});await page.waitForTimeout(50);
  }
  await page.waitForTimeout(300);
  const frames=await page.evaluate(()=>{window.__sampling=false;return window.__navFrames;});
  if(frames.some(count=>count===0))throw Error('blank frame in transition');
  if(await page.locator('.sr-shell').count()!==1||await page.locator('.sr-shell[inert]').count())throw Error('stale outgoing route or inert page');
  checks.push('rapid navigation retains background and removes outgoing inert pages');
  await page.emulateMedia({reducedMotion:'reduce'});
  const reduced=await page.locator('.sr-nav').evaluate(el=>getComputedStyle(el,'::before').transitionDuration);
  if(reduced!=='0s')throw Error('reduced motion ignored');await page.emulateMedia({reducedMotion:'no-preference'});
  await tab('我的').click();await page.waitForURL('**/app/me');await page.waitForTimeout(230);
  await page.getByRole('button',{name:'退出登录',exact:true}).click();await page.getByRole('dialog').getByRole('button',{name:'退出登录',exact:true}).click();await page.waitForURL('**/app/login');await page.waitForTimeout(250);
  if(await page.locator('.sr-shell').count()||await page.locator('.composer').count())throw Error('authenticated page remains after logout');
  await page.getByLabel('账号',{exact:true}).fill('second_fixture');await page.getByLabel('密码',{exact:true}).fill('fake-fixture-password');await page.getByRole('button',{name:'登录',exact:true}).click();await page.waitForURL('**/app/chat');await page.waitForTimeout(250);
  if(await page.locator('.composer textarea').inputValue()!==''||await page.locator('.has-thinking').count())throw Error('previous user cache leaked after login');
  checks.push('reduced motion respected; logout clears cache and next login has no prior draft or messages');
  if(errors.length)throw Error(errors.join(';'));
  return{checks,eventReads,frames:frames.length,pressed,loader,scrollTop,errors};
}
