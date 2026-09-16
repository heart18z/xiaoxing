// Local-only Playwright fixture. No live user data or mutation requests.
async page=>{
  await page.unrouteAll();
  await page.addInitScript(()=>{
    let value=JSON.stringify({token:'fixture',userInfo:{user_id:'1',user_name:'测试用户',role_name:'app_user'}});
    window.CapacitorCustomPlatform={name:'ios'};
    window.Capacitor={PluginHeaders:[{name:'XiaoxingNative',methods:['readState','writeState','context']},{name:'PushNotifications',methods:['checkPermissions','register','removeListener']},{name:'App',methods:['removeListener']},{name:'Keyboard',methods:['removeListener']}].map(p=>({...p,methods:[...p.methods.map(name=>({name,rtype:'promise'})),{name:'addListener',rtype:'callback'}]})),nativeCallback:()=> 'fixture',nativePromise:async(plugin,method,o)=>{if(method==='readState')return{value};if(method==='writeState'){value=o.value;return{};}if(method==='context')return{bundleId:'com.dfyj.xiaoxing',environment:'production'};if(method==='checkPermissions')return{receive:'denied'};return{};}};
  });
  const day=offset=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit'}).format(Date.now()+offset*86400000);
  const jobs=new Map();let polls=0;
  await page.route('**/api/**',async route=>{
    if(route.request().url().includes('/src/'))return route.continue();
    const path=route.request().url().split('?')[0];let data={};
    if(path.endsWith('/bootstrap'))data={user:{id:'1',name:'测试用户',account:'fixture'},activeEventCount:2};
    else if(path.endsWith('/events/drawer'))data=route.request().url().includes('received')?[]:[0,1].map((n)=>({eventId:String(n+1),branchId:String(n+1),summary:n?'明日提交合同':'今日整理会议资料',recipientName:'自己',creatorName:'测试用户',eventStatus:'ACTIVE',branchStatus:'ACTIVE',nextEvaluateTime:day(n)+'T23:00:00'}));
    else if(path.endsWith('/chat/jobs/submit')){const body=route.request().postDataJSON();jobs.set(body.requestId,{status:'RUNNING'});data={status:'RUNNING'};polls=0;}
    else if(path.endsWith('/chat/jobs/status')){const body=route.request().postDataJSON();data=jobs.get(body.requestId)||{status:'NOT_FOUND'};if(data.status==='RUNNING'&&++polls>=4)data={status:'SUCCEEDED',result:{reply:'你好，提醒事项已经记录。'}};}
    else if(path.endsWith('/chat/messages'))data=[];
    else if(path.endsWith('/chat/sync'))data={revision:'0',messages:[]};
    await route.fulfill({json:{code:200,success:true,data}});
  });
  await page.setViewportSize({width:390,height:844});
  await page.goto('http://127.0.0.1:4188/native.html');
}
