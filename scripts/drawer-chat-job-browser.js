async page=>{
  const errors=[],checks=[],jobs=new Map();let submissions=0,offline=false;
  page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{
    let value=localStorage.getItem('fixture-vault')||JSON.stringify({token:'fixture',userInfo:{user_id:'1',user_name:'测试用户',role_name:'app_user'}});
    window.__background=false;Object.defineProperty(document,'hidden',{configurable:true,get:()=>window.__background});
    window.CapacitorCustomPlatform={name:'ios'};
    window.Capacitor={PluginHeaders:[{name:'XiaoxingNative',methods:['readState','writeState','context']},{name:'PushNotifications',methods:['checkPermissions','register','removeListener']},{name:'App',methods:['removeListener']},{name:'Keyboard',methods:['removeListener']}].map(p=>({...p,methods:[...p.methods.map(name=>({name,rtype:'promise'})),{name:'addListener',rtype:'callback'}]})),nativeCallback:()=> 'fixture',nativePromise:async(plugin,method,o)=>{if(method==='readState')return{value};if(method==='writeState'){value=o.value;localStorage.setItem('fixture-vault',value);return{};}if(method==='context')return{bundleId:'com.dfyj.xiaoxing',environment:'production'};if(method==='checkPermissions')return{receive:'denied'};return{};}};
  });
  const day=offset=>{const now=new Date(Date.now()+offset*86400000);return new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit'}).format(now);};
  const row=(i,extra={})=>({eventId:String(i),branchId:String(i),summary:'待办事项 '+i,recipientName:'陈颖',creatorName:'李明',eventStatus:'ACTIVE',branchStatus:'ACTIVE',eventTime:day(0)+'T23:00:00',nextEvaluateTime:day(0)+'T22:00:00',...extra});
  const drawerRows=[...Array.from({length:12},(_,i)=>row(i+1)),row(20,{summary:'已经提醒的报告',lastRemindedAt:day(0)+'T09:00:00',nextEvaluateTime:null,eventTime:null}),row(21,{summary:'明日提交合同',eventTime:day(1)+'T10:00:00',nextEvaluateTime:day(1)+'T09:00:00'}),row(22,{summary:'月底项目复盘',eventTime:day(8)+'T18:00:00',nextEvaluateTime:day(8)+'T17:00:00'})];
  const history=()=>[...jobs.values()].filter(j=>j.status==='SUCCEEDED').flatMap((j,i)=>[{id:String(i*2+1),messageRole:'user',messageType:'TEXT',content:j.content,isRead:true},{id:String(i*2+2),messageRole:'assistant',messageType:'TEXT',content:j.result.reply,isRead:true}]);
  await page.route('**/api/**',async route=>{
    const path=route.request().url().split('?')[0];let data={};
    if(path.endsWith('/bootstrap'))data={user:{id:'1',name:'测试用户',account:'fixture'},activeEventCount:12};
    else if(path.endsWith('/events/drawer'))data=route.request().url().includes('received')?[row(30,{summary:'我接收的会议通知'})]:drawerRows;
    else if(path.endsWith('/chat/jobs/submit')){const body=route.request().postDataJSON();if(!jobs.has(body.requestId)){submissions++;jobs.set(body.requestId,{...body,status:'RUNNING',reasoning:'正在核对事件时间'});}data=jobs.get(body.requestId);}
    else if(path.endsWith('/chat/jobs/status')){if(offline){await route.abort();return;}const body=route.request().postDataJSON();data=jobs.get(body.requestId)||{status:'NOT_FOUND'};}
    else if(path.endsWith('/chat/messages'))data=history();
    else if(path.endsWith('/chat/sync'))data={revision:String(submissions),messages:history()};
    else if(path.endsWith('/chat/stop')){const body=route.request().postDataJSON();const job=jobs.get(body.requestId);job.status='CANCELLED';job.message='已停止生成';data={stopped:true};}
    else if(path.endsWith('/event/detail'))data={creator:true,event:{id:'1',event_no:'FIXTURE1',event_summary:'待办事项 1',event_status:'ACTIVE'},branches:[],timeline:[]};
    await route.fulfill({json:{code:200,success:true,data}});
  });
  await page.setViewportSize({width:390,height:844});await page.goto('http://127.0.0.1:4188/app/chat');
  await page.getByRole('button',{name:'查看我的事件'}).click();
  await page.getByRole('heading',{name:'提醒列表'}).waitFor();await page.getByText('待办事项 1',{exact:true}).waitFor();await page.waitForTimeout(400);
  if(!page.url().endsWith('/app/chat'))throw Error('menu navigated instead of opening drawer');
  const region=page.getByRole('region',{name:'分组提醒列表'});
  if(!await region.evaluate(el=>el.scrollHeight>el.clientHeight))throw Error('drawer not scrollable');
  await page.screenshot({path:'output/playwright/reminder-drawer.png'});
  await region.hover();await page.mouse.wheel(0,600);await page.waitForTimeout(250);if(await region.evaluate(el=>el.scrollTop)<=0)throw Error('drawer cannot scroll');
  if(await page.getByRole('tab',{name:'全部',exact:true}).getAttribute('aria-selected')!=='true')throw Error('all is not default');
  await page.getByRole('button',{name:'选择日期范围'}).click();
  await page.locator('input[aria-label="开始日期"]').fill(day(1));await page.locator('input[aria-label="结束日期"]').fill(day(1));
  if(await page.getByText('待办事项 1',{exact:true}).count())throw Error('date filter retained today');
  await page.getByText('明日提交合同',{exact:true}).waitFor();
  await page.getByRole('button',{name:'还原'}).click();await page.getByText('待办事项 1',{exact:true}).waitFor();
  await page.getByRole('tab',{name:'我收到的'}).click();await page.getByText('我接收的会议通知',{exact:true}).waitFor();
  await page.getByRole('tab',{name:'我发起的'}).click();await page.getByRole('button',{name:'查看事件：待办事项 1',exact:true}).click();await page.waitForURL('**/app/event/1?**');
  checks.push('drawer opens without navigation; category list scrolls; sent/received switch; arrow opens detail');
  await page.goto('http://127.0.0.1:4188/app/chat');await page.locator('.assistant-tab-image').waitFor();await page.waitForTimeout(400);
  if(!await page.locator('.assistant-tab-image').evaluate(el=>el.complete&&el.naturalWidth>0))throw Error('provided icon failed to load');
  if(await page.locator('.event-counter b').textContent()!=='12')throw Error('event count lost');
  await page.screenshot({path:'output/playwright/chat-tab-icons.png'});
  await page.locator('.composer textarea').fill('息屏后仍然处理的消息');await page.getByRole('button',{name:'发送',exact:true}).click();
  await page.getByText('正在处理，息屏后也会继续',{exact:true}).waitFor();
  await page.evaluate(()=>{window.__background=true;document.dispatchEvent(new Event('visibilitychange'));});
  offline=true;await page.waitForTimeout(1600);
  const first=[...jobs.values()][0];first.status='SUCCEEDED';first.result={reply:'息屏期间事件已更新'};
  if(await page.locator('.composer textarea').inputValue()!=='')throw Error('background restored sent message to input');
  await page.evaluate(()=>{window.__background=false;document.dispatchEvent(new Event('visibilitychange'));});await page.waitForTimeout(1600);offline=false;
  await page.evaluate(()=>window.dispatchEvent(new Event('online')));
  await page.getByText('息屏期间事件已更新',{exact:true}).waitFor({timeout:10000});if(submissions!==1)throw Error('reconnection duplicated submission');
  await page.getByRole('button',{name:'发送',exact:true}).waitFor();
  checks.push('background and network interruption preserve empty composer; result resumes with one server execution');
  await page.locator('.composer textarea').fill('重进 App 恢复消息');await page.getByRole('button',{name:'发送',exact:true}).click();await page.getByText('正在处理，息屏后也会继续',{exact:true}).waitFor();
  await page.reload();await page.getByText('正在处理，息屏后也会继续',{exact:true}).waitFor();
  const second=[...jobs.values()][1];second.status='SUCCEEDED';second.result={reply:'重新打开后恢复成功'};
  await page.getByText('重新打开后恢复成功',{exact:true}).waitFor({timeout:10000});if(submissions!==2)throw Error('reload resubmitted task');
  await page.getByRole('button',{name:'发送',exact:true}).waitFor();
  await page.locator('.composer textarea').fill('主动停止消息');await page.getByRole('button',{name:'发送',exact:true}).click();await page.getByText('正在处理，息屏后也会继续',{exact:true}).waitFor();
  await page.getByRole('button',{name:'停止生成'}).click();await page.getByRole('button',{name:'发送',exact:true}).waitFor({timeout:10000});
  if(await page.evaluate(()=>Boolean(JSON.parse(localStorage.getItem('fixture-vault')).pendingChat)))throw Error('cancelled task still pending');
  checks.push('reload restores pending request without replay; explicit stop clears pending only after server acknowledgement');
  if(errors.length)throw Error(errors.join('\n'));return{checks,submissions,errors};
}
