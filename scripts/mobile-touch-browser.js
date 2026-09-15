async page => {
  const errors = [];
  page.on('pageerror', e => errors.push(e.message));
  await page.addInitScript(() => {
    let value = JSON.stringify({token:'offline',userInfo:{user_id:'1',user_name:'测试用户',role_name:'app_user'}});
    window.CapacitorCustomPlatform = {name:'ios'};
    window.Capacitor = {
      PluginHeaders: [
        {name:'XiaoxingNative',methods:['readState','writeState','context']},
        {name:'PushNotifications',methods:['checkPermissions','register','removeListener']},
        {name:'App',methods:['removeListener']}, {name:'Keyboard',methods:['removeListener']}
      ].map(p=>({...p,methods:[...p.methods.map(name=>({name,rtype:'promise'})),{name:'addListener',rtype:'callback'}]})),
      nativeCallback:()=> 'fixture',
      nativePromise:async (plugin,method,o)=> {
        if(method==='readState')return {value};
        if(method==='writeState'){value=o.value;return {};}
        if(method==='context')return {bundleId:'com.dfyj.xiaoxing',environment:'production'};
        if(method==='checkPermissions')return {receive:'denied'};
        return {};
      }
    };
  });
  await page.route('**/api/**',async route=>{
    const path=route.request().url().split('?')[0];let data={};
    if(path.endsWith('/bootstrap'))data={user:{id:'1',name:'测试用户',account:'fixture'},sentActiveEventCount:20,activeEventCount:20};
    else if(path.endsWith('/events'))data=Array.from({length:20},(_,i)=>({id:String(i+1),eventNo:'FIXTURE'+i,eventSummary:'测试事件 '+i,eventStatus:'ACTIVE',recipientCount:1,activeBranchCount:1,createTime:'2026-09-15 13:00:00'}));
    else if(path.endsWith('/friends/list'))data=[];
    else if(path.endsWith('/friends/requests'))data={incoming:[],outgoing:[]};
    await route.fulfill({json:{code:200,success:true,data}});
  });
  await page.setViewportSize({width:390,height:844});
  await page.goto('http://127.0.0.1:4188/app/events');
  await page.getByText('测试事件 0',{exact:true}).waitFor();
  const assertTouch=async selector=>{
    const values=await page.locator(selector).evaluateAll(nodes=>nodes.map(el=>getComputedStyle(el).touchAction));
    if(!values.length||values.some(v=>v!=='manipulation'))throw Error('Missing manipulation: '+selector+' '+values);
  };
  await assertTouch('.sr-shell,.sr-main,.event-list-scroll,.event,.event h3,.event-search input,.type-tabs button');
  await page.getByText('我的事件',{exact:true}).dblclick();
  if(await page.evaluate(()=>visualViewport.scale)!==1)throw Error('Viewport scale changed');
  await page.locator('.event-list-scroll').hover();await page.mouse.wheel(0,450);await page.waitForTimeout(250);
  if(await page.locator('.event-list-scroll').evaluate(el=>el.scrollTop)<=0)throw Error('List scrolling blocked');
  await page.getByRole('searchbox',{name:'搜索事件',exact:true}).fill('测试事件 19');
  if(await page.locator('.event').count()!==1)throw Error('Search broken');
  if(await page.locator('.event-search input').evaluate(el=>parseFloat(getComputedStyle(el).fontSize))<16)throw Error('Input focus zoom risk');
  await page.getByRole('button',{name:'清空搜索'}).click();
  await page.screenshot({path:'output/playwright/mobile-touch-events.png'});
  await page.getByRole('link',{name:'我的',exact:true}).click();
  await page.getByRole('button',{name:'编辑资料',exact:true}).click();
  const dialog=page.getByRole('dialog',{name:'编辑资料'});await dialog.waitFor();
  await assertTouch('.el-overlay,.el-dialog,.profile-edit-form input');
  if(errors.length)throw Error(errors.join('\n'));
  return {checks:['nested targets use manipulation','desktop double click scale unchanged','scroll/search/click work','16px input preserved','body-teleported dialog protected'],errors,note:'Desktop browser checks are not an iOS double-tap gesture test. Real iPhone acceptance still required.'};
}
