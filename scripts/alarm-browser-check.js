async page=>{
  const origin='http://127.0.0.1:4188',errors=[],checks=[];
  page.on('pageerror',e=>errors.push(e.message));
  let stopped=false,created=false;
  const detail=()=>({creator:true,event:{id:'99',creator_user_id:'1',event_no:'TEST99',event_summary:'明天开会',event_time:'2030-09-14 16:30:00',event_status:stopped?'STOPPED':'ACTIVE'},branches:[{id:'101',recipientUserId:'1',name:'测试用户',branchStatus:stopped?'STOPPED':'ACTIVE',taskEventTime:'2030-09-14 16:30:00',latestSummary:'自己的开会任务'}],timeline:[]});
  await page.addInitScript(()=>{
    const listeners={};let sequence=0,value=JSON.stringify({token:'offline-alarm-test',userInfo:{user_id:'1',user_name:'测试用户',role_name:'app_user'}});
    window.__alarms=[];window.__alarmCalls=[];window.__alarmMode='ok';window.__alarmOwner='';
    window.CapacitorCustomPlatform={name:'ios'};
    window.Capacitor={PluginHeaders:[
      {name:'XiaoxingNative',methods:['readState','writeState','context']},
      {name:'XiaoxingAlarm',methods:['activate','list','schedule','cancel']},
      {name:'PushNotifications',methods:['checkPermissions','requestPermissions','register','removeAllDeliveredNotifications','removeListener']},
      {name:'App',methods:['removeListener']},{name:'Keyboard',methods:['removeListener']}
    ].map(p=>({...p,methods:[...p.methods.map(name=>({name,rtype:'promise'})),{name:'addListener',rtype:'callback'}]})),
    nativePromise:async(plugin,method,o)=>{
      if(method==='readState')return{value};if(method==='writeState'){value=o.value;return{};}
      if(method==='context')return{bundleId:'com.dfyj.xiaoxing',environment:'production',appVersion:'1.0'};
      if(method==='checkPermissions')return{receive:'denied'};
      if(plugin==='XiaoxingAlarm'){
        window.__alarmCalls.push({method,...o});
        if(method==='activate'){if(window.__alarmOwner!==o.owner){window.__alarms=[];window.__alarmOwner=o.owner;}return{supported:window.__alarmMode!=='unsupported'};}
        if(method==='list')return{supported:true,permission:window.__alarmMode==='denied'?'denied':'authorized',alarms:window.__alarms};
        if(method==='schedule'){
          if(window.__alarmMode==='denied')throw Error('未授权系统闹铃');
          if(window.__alarmMode==='unconfirmed')return{};
          if(o.owner!==window.__alarmOwner)throw Error('wrong account');
          const alarm={...o,id:'fixture-'+o.eventId,active:true};window.__alarms=[...window.__alarms.filter(a=>a.eventId!==o.eventId),alarm];return{id:alarm.id};
        }
        if(method==='cancel'){window.__alarms=window.__alarms.filter(a=>a.eventId!==o.eventId);return{};}
      }return{};
    },nativeCallback:(plugin,method,o,cb)=>{const id=String(++sequence);listeners[id]={plugin,event:o.eventName,cb};return id;}};
  });
  await page.route('**/api/**',async route=>{
    const url=route.request().url();let data={};
    if(url.includes('/bootstrap'))data={user:{id:'1',name:'测试用户',account:'fixture'},activeEventCount:1};
    else if(url.includes('/event/detail'))data=detail();
    else if(url.includes('/candidate/confirm')){created=true;data={eventIds:['99']};}
    else if(url.includes('/event/stop')){stopped=true;data=true;}
    else if(url.includes('/chat/messages'))data=created?[{id:'21',messageRole:'assistant',messageType:'SYSTEM',content:'事件已创建',payload:{eventIds:['99']}}]:[{id:'20',messageRole:'assistant',messageType:'CANDIDATE',content:'请确认本机闹铃',payload:{candidateId:'12',events:[{summary:'2030年开会',eventTime:'2030-09-14 16:30:00',alarmRequested:true,recipients:[{userId:'1',name:'测试用户'}]}]}}];
    else if(url.includes('/chat/sync'))data={revision:'fixture'};
    await route.fulfill({json:{code:200,success:true,data}});
  });
  await page.setViewportSize({width:390,height:844});await page.goto(origin+'/app/event/99');
  const set=page.getByRole('button',{name:'设置本机闹铃',exact:true});await set.waitFor();
  await page.evaluate(()=>window.__alarmMode='denied');await set.click();await page.locator('.alarm-control [role=status]').filter({hasText:'未授权系统闹铃'}).waitFor();
  if(await page.locator('.alarm-time').count())throw Error('denial falsely reported scheduled');checks.push('denial never reports success');
  await page.evaluate(()=>window.__alarmMode='unconfirmed');await set.click();await page.locator('.alarm-control [role=status]').filter({hasText:'系统未确认'}).waitFor();checks.push('missing native acknowledgment rejected');
  await page.evaluate(()=>window.__alarmMode='ok');await set.click();await page.getByText('系统已确认：本机闹铃设置成功',{exact:true}).waitFor();
  const scheduled=await page.evaluate(()=>window.__alarms[0]);if(scheduled.timestamp!==Date.parse('2030-09-14T08:30:00Z')/1000||scheduled.owner!=='1')throw Error('wrong time/account');
  await page.screenshot({path:'output/playwright/alarm-scheduled-390.png'});
  await page.getByRole('button',{name:'更新本机闹铃',exact:true}).click();if(await page.evaluate(()=>window.__alarms.length)!==1)throw Error('duplicate event alarm');
  await page.getByRole('button',{name:'取消本机闹铃',exact:true}).click();await page.getByText('本机闹铃已取消，提醒事件仍保留',{exact:true}).waitFor();checks.push('schedule/update/cancel preserve event and account');
  await page.evaluate(()=>window.__alarmMode='unsupported');await set.click();await page.locator('.alarm-control [role=status]').filter({hasText:'需要 iOS 26'}).waitFor();checks.push('older native device explicitly unsupported');
  await page.evaluate(()=>window.__alarmMode='ok');await page.goto(origin+'/app/chat');
  const checkbox=page.getByRole('checkbox',{name:'同时设置本机系统闹铃（iOS 26+）'});await checkbox.waitFor();if(!await checkbox.isChecked())throw Error('explicit alarm intent not selected');
  await page.getByRole('button',{name:'确认创建',exact:true}).click();await page.getByRole('button',{name:'查看事件 / 设置本机闹铃 ›'}).waitFor();
  if(!created||await page.evaluate(()=>window.__alarms.length)!==1)throw Error('candidate did not schedule');checks.push('confirmed candidate schedules and receipt opens event');
  await page.locator('.el-message').last().waitFor({state:'hidden'});
  await page.getByRole('button',{name:'查看事件 / 设置本机闹铃 ›'}).click();await page.waitForURL('**/app/event/99');
  stopped=true;await page.evaluate(()=>window.dispatchEvent(new Event('smart-reminder:badge-refresh')));
  await page.waitForFunction(()=>window.__alarms.length===0);checks.push('stopped event cancels on local sync');
  stopped=false;await page.reload();await set.click();await page.getByText('系统已确认：本机闹铃设置成功',{exact:true}).waitFor();
  await page.evaluate(()=>window.dispatchEvent(new Event('native:logout')));await page.waitForFunction(()=>window.__alarms.length===0&&window.__alarmOwner==='');checks.push('logout cancels local alarms');
  if(errors.length)throw Error(errors.join(';'));return{checks,errors,native:'mock bridge, not real AlarmKit'};
}
