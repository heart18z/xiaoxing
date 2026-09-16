async page=>{
  const errors=[],checks=[];let searches=0,remarkWrites=0,conversationReads=0;
  let friends=[{id:'2',account:'staff2',name:'陈通',friendRemark:'通哥',phone:'13800138000',email:'friend@example.com',permissionMode:'MUTUAL'},{id:'3',account:'staff3',name:'李明',friendRemark:'',permissionMode:'MUTUAL'}];
  page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{
    let value=JSON.stringify({token:'offline-fixture',userInfo:{user_id:'1',user_name:'测试用户',role_name:'app_user'}});
    window.__alarms=[];window.CapacitorCustomPlatform={name:'ios'};
    window.Capacitor={PluginHeaders:[
      {name:'XiaoxingNative',methods:['readState','writeState','context']},
      {name:'XiaoxingAlarm',methods:['activate','list','schedule','cancel']},
      {name:'PushNotifications',methods:['checkPermissions','requestPermissions','register','removeListener']},
      {name:'App',methods:['removeListener']},{name:'Keyboard',methods:['removeListener']}
    ].map(p=>({...p,methods:[...p.methods.map(name=>({name,rtype:'promise'})),{name:'addListener',rtype:'callback'}]})),
    nativeCallback:()=>String(Math.random()),nativePromise:async(plugin,method,o)=>{
      if(method==='readState')return{value};if(method==='writeState'){value=o.value;return{};}
      if(method==='context')return{bundleId:'com.dfyj.xiaoxing',environment:'production',appVersion:'1.0'};
      if(method==='checkPermissions')return{receive:'denied'};
      if(plugin==='XiaoxingAlarm'){
        if(method==='activate')return{supported:true};
        if(method==='list')return{supported:true,permission:'authorized',alarms:window.__alarms};
        if(method==='schedule'){window.__alarms=[{...o,id:'test-alarm',active:true}];return{id:'test-alarm'};}
        if(method==='cancel')window.__alarms=[];
      }return{};
    }};
  });
  await page.route('**/api/**',async route=>{
    const url={pathname:route.request().url().split('?')[0]};let data={};
    if(url.pathname.endsWith('/bootstrap'))data={user:{id:'1',name:'测试用户',account:'fixture'},activeEventCount:1};
    else if(url.pathname.endsWith('/friends/list'))data=friends;
    else if(url.pathname.endsWith('/friends/requests'))data={incoming:[],outgoing:[]};
    else if(url.pathname.endsWith('/chat/messages'))data=[];
    else if(url.pathname.endsWith('/chat/sync'))data={revision:'1',messages:[]};
    else if(url.pathname.endsWith('/events/drawer'))data=[{eventId:'99',summary:'明天开会',recipientName:'陈通',eventStatus:'ACTIVE',branchStatus:'ACTIVE',eventTime:'2030-09-14T16:30:00'}];
    else if(url.pathname.endsWith('/friends/search')){searches++;data=[{id:'4',name:'新好友',account:'staff4',relationStatus:'NONE'}];}
    else if(url.pathname.endsWith('/friends/remark')){remarkWrites++;const body=route.request().postDataJSON();if(Object.keys(body).sort().join(',')!=='remark,targetUserId')throw Error('unexpected owner or profile write');friends=friends.map(u=>u.id===body.targetUserId?{...u,friendRemark:body.remark}:u);data=true;}
    else if(url.pathname.endsWith('/event/detail'))data={creator:true,event:{id:'99',creator_user_id:'1',event_no:'FIXTURE99',event_summary:'明天开会',event_time:'2030-09-14 16:30:00',event_status:'ACTIVE'},branches:[{id:'101',recipientUserId:'1',name:'测试用户',branchStatus:'ACTIVE',taskEventTime:'2030-09-14 16:30:00',latestSummary:'开会'}],timeline:[]};
    else if(url.pathname.endsWith('/event/conversation')){
      conversationReads++;await page.waitForTimeout(1400);
      data={participant:{displayName:'测试用户'},messages:conversationReads===1?Array.from({length:16},(_,i)=>({id:String(i),messageRole:i%2?'user':'assistant',content:'这是事件的对话记录 '+i,createTime:'2030-09-14 10:00:00'})):[]};
    }
    await route.fulfill({json:{code:200,success:true,data}});
  });
  await page.setViewportSize({width:390,height:844});await page.goto('http://127.0.0.1:4188/app/me');
  await page.getByRole('button',{name:'添加好友',exact:true}).waitFor();
  const filter=page.getByRole('textbox',{name:'查找现有好友'});
  await filter.fill('通哥');await page.waitForTimeout(100);
  if(await page.locator('.friend-list .person').count()!==1||searches!==0)throw Error('local filter invoked discovery or failed');
  await filter.fill('不存在');if(await page.locator('.friend-list .person').count())throw Error('empty filter failed');
  await filter.fill('');checks.push('existing friends filter locally by remark/name/contact');
  const firstFriend=page.locator('.friend-list .person').first();
  if(await firstFriend.getByRole('button').count()!==1)throw Error('friend needs one management action');
  await page.waitForTimeout(400);await page.screenshot({path:'output/playwright/ui-three-friends.png'});
  await firstFriend.getByRole('button',{name:'管理',exact:true}).click();
  await page.getByRole('dialog',{name:'好友资料',exact:true}).getByRole('button',{name:'权限设定',exact:true}).click();
  await page.getByRole('heading',{name:'申请变更权限'}).waitFor();
  if(await page.getByRole('dialog',{name:'好友资料'}).isVisible())throw Error('permissions opened profile');
  await page.locator('.sheet-head button').click();await page.locator('.sheet-mask').waitFor({state:'hidden'});
  await firstFriend.getByRole('button',{name:'管理',exact:true}).click();
  let dialog=page.getByRole('dialog',{name:'好友资料',exact:true});
  await dialog.getByText('friend@example.com',{exact:true}).waitFor();
  await dialog.getByLabel('备注（昵称）').fill('老陈');
  await dialog.getByRole('button',{name:'保存备注',exact:true}).click();await dialog.waitFor({state:'hidden'});
  await page.locator('.friend-list').getByText('老陈',{exact:true}).waitFor();
  if(remarkWrites!==1||friends[0].name!=='陈通')throw Error('remark replaced real name');
  checks.push('private remark saved; original name and basic info preserved');
  await page.getByRole('button',{name:'添加好友',exact:true}).click();
  dialog=page.getByRole('dialog',{name:'添加好友',exact:true});
  await dialog.getByRole('textbox',{name:'搜索新好友'}).fill('new@example.com');
  await dialog.getByRole('button',{name:'查找',exact:true}).click();await dialog.getByText('新好友',{exact:true}).waitFor();
  if(searches!==1)throw Error('discovery not submitted');
  await page.screenshot({path:'output/playwright/friends-add-390.png'});
  await dialog.getByRole('textbox',{name:'搜索新好友'}).fill('next');await page.waitForTimeout(100);
  if(await dialog.getByText('新好友',{exact:true}).count())throw Error('stale discovery results');
  await page.keyboard.press('Escape');await dialog.waitFor({state:'hidden'});checks.push('add modal has dedicated exact-field search and clears stale results');
  await page.setViewportSize({width:320,height:740});
  await page.locator('.friend-list .person').first().getByRole('button',{name:'编辑资料',exact:true}).click();
  await page.getByRole('dialog',{name:'好友资料'}).waitFor();await page.waitForTimeout(350);
  await page.screenshot({path:'output/playwright/friends-detail-320.png'});
  const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1);if(overflow)throw Error('narrow viewport overflow');
  await page.setViewportSize({width:320,height:400});
  dialog=page.getByRole('dialog',{name:'好友资料'});
  await dialog.getByLabel('备注（昵称）').fill('');
  await dialog.getByRole('button',{name:'保存备注',exact:true}).click();await dialog.waitFor({state:'hidden'});
  if(friends[0].friendRemark!==''||friends[0].name!=='陈通')throw Error('clear remark failed');
  checks.push('short viewport scrolls to save; clearing remark restores original name');
  await page.setViewportSize({width:390,height:844});await page.goto('http://127.0.0.1:4188/app/event/99');
  await page.getByRole('button',{name:'设置本机闹铃',exact:true}).click();
  const time=page.locator('.alarm-time');await time.waitFor();
  if(!(await time.innerText()).includes('2030/09/14 16:30'))throw Error('actual alarm date/time missing: '+await time.innerText());
  if(await page.locator('.alarm-note').getAttribute('open')!==null)throw Error('alarm caveats not collapsed');
  await page.screenshot({path:'output/playwright/alarm-compact-390.png'});checks.push('compact alarm reads actual native timestamp and retains expandable caveats');
  await page.getByRole('button',{name:'查看对话',exact:true}).first().click();
  await page.locator('.conversation-frame[aria-busy=true]').waitFor();await page.waitForTimeout(350);
  const before=await page.locator('.event-conversation-dialog').boundingBox();
  await page.locator('.conversation-frame[aria-busy=false]').waitFor();
  const after=await page.locator('.event-conversation-dialog').boundingBox();
  if(Math.abs(before.height-after.height)>1)throw Error('dialog jumps '+before.height+' -> '+after.height);
  const scrolling=await page.locator('.conversation-list').evaluate(el=>el.scrollHeight>el.clientHeight);
  if(!scrolling)throw Error('conversation not scrolling internally');
  await page.screenshot({path:'output/playwright/conversation-stable-390.png'});
  checks.push('delayed dialog content keeps fixed height and scrolls internally');
  await page.goto('http://127.0.0.1:4188/app/event/99');
  await page.getByRole('button',{name:'查看对话',exact:true}).first().click();
  await page.locator('.conversation-frame[aria-busy=true]').waitFor();await page.waitForTimeout(350);
  const emptyBefore=await page.locator('.event-conversation-dialog').boundingBox();
  await page.getByText('该事件暂时没有对话记录',{exact:true}).waitFor();
  const emptyAfter=await page.locator('.event-conversation-dialog').boundingBox();
  if(Math.abs(emptyBefore.height-emptyAfter.height)>1)throw Error('empty conversation changes height');
  checks.push('empty conversation has same stable height');
  await page.setViewportSize({width:390,height:844});await page.goto('http://127.0.0.1:4188/app/chat');
  await page.getByRole('button',{name:'查看我的事件'}).click();await page.getByRole('heading',{name:'提醒列表'}).waitFor();await page.waitForTimeout(400);
  await page.screenshot({path:'output/playwright/ui-three-drawer.png'});
  const tabHeight=await page.getByRole('tab',{name:'全部',exact:true}).evaluate(el=>el.getBoundingClientRect().height);
  if(tabHeight>35)throw Error('drawer tabs are not compact');
  await page.getByRole('button',{name:'选择日期范围'}).click();await page.getByRole('dialog',{name:'选择提醒日期'}).getByRole('button',{name:'未来7天',exact:true}).click();
  await page.setViewportSize({width:320,height:740});await page.waitForTimeout(200);
  if(await page.locator('.reminder-drawer').evaluate(el=>el.scrollWidth>el.clientWidth))throw Error('date row overflows');
  await page.screenshot({path:'output/playwright/ui-three-drawer-date-320.png'});
  await page.getByRole('button',{name:'确认筛选',exact:true}).click();
  await page.getByRole('button',{name:'还原',exact:true}).click();await page.getByRole('button',{name:'选择日期范围'}).waitFor();
  await page.getByRole('button',{name:'关闭提醒列表'}).click();await page.waitForTimeout(400);
  await page.screenshot({path:'output/playwright/ui-three-tab.png'});
  checks.push('single friend management action, smaller drawer tabs, custom calendar/reset/narrow screen');
  if(errors.length)throw Error(errors.join('\n'));
  return {checks,searches,remarkWrites,dialogHeight:after.height,errors};
}
