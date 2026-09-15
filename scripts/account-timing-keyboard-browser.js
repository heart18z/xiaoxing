async page=>{
  const errors=[],checks=[];let registrations=0;
  page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{
    const listeners={};let sequence=0;
    let value=sessionStorage.getItem('fixture-auth')==='1'?JSON.stringify({token:'offline',userInfo:{user_id:'1',user_name:'测试用户',role_name:'app_user'}}):'{}';
    window.__nativeEmit=(plugin,event,data)=>{for(const x of Object.values(listeners))if(x.plugin===plugin&&x.event===event)x.cb(data);};
    window.CapacitorCustomPlatform={name:'ios'};
    window.Capacitor={PluginHeaders:[
      {name:'XiaoxingNative',methods:['readState','writeState','context']},
      {name:'PushNotifications',methods:['checkPermissions','requestPermissions','register','removeListener']},
      {name:'App',methods:['removeListener']},{name:'Keyboard',methods:['removeListener']}
    ].map(p=>({...p,methods:[...p.methods.map(name=>({name,rtype:'promise'})),{name:'addListener',rtype:'callback'}]})),
      nativeCallback:(plugin,method,o,cb)=>{const id=String(++sequence);listeners[id]={plugin,event:o.eventName,cb};return id;},
      nativePromise:async(plugin,method,o)=>{
        if(method==='readState')return{value};if(method==='writeState'){value=o.value;return{};}
        if(method==='context')return{bundleId:'com.dfyj.xiaoxing',environment:'production',appVersion:'1.0'};
        if(method==='checkPermissions')return{receive:'denied'};return{};
      }};
  });
  const messages=Array.from({length:25},(_,i)=>({id:String(i+1),messageRole:i%2?'user':'assistant',messageType:'TEXT',content:i===24?'这是最新一条消息，键盘弹出后应保持可见':'这是一条较长的历史对话，用于验证键盘上顶时的滚动位置。'.repeat(3),isRead:true}));
  await page.route('**/api/**',async route=>{
    const path=route.request().url().split('?')[0];let data={},code=200,msg='';
    if(path.endsWith('/bootstrap'))data={user:{id:'1',name:'测试用户',account:'fixture'},activeEventCount:1};
    else if(path.endsWith('/account/register')){
      registrations++;const body=route.request().postDataJSON();
      if(!body.phone&&!body.email)throw Error('empty contacts reached server');
      if(body.phone==='13800138000'){code=400;msg='手机号已被使用，请更换手机号';}
      else if(body.email==='used@example.com'){code=400;msg='邮箱已被使用，请更换邮箱';}
      else data=true;
    }
    else if(path.endsWith('/chat/messages'))data=messages;
    else if(path.endsWith('/chat/sync'))data={revision:'test',messages};
    else if(path.endsWith('/friends/list'))data=[];
    else if(path.endsWith('/friends/requests'))data={incoming:[],outgoing:[]};
    else if(path.endsWith('/event/detail'))data={creator:true,event:{id:'99',creator_user_id:'1',event_no:'FIXTURE99',event_summary:'会议',event_time:null,event_timeVaries:true,event_status:'ACTIVE'},branches:[{id:'2',recipientUserId:'2',name:'陈通',branchStatus:'ACTIVE',taskEventTime:'2030-09-15 13:00:00',latestSummary:'13点开会'},{id:'3',recipientUserId:'3',name:'李明',branchStatus:'ACTIVE',taskEventTime:'2030-09-15 16:00:00',latestSummary:'16点开会'}],timeline:[]};
    await route.fulfill({json:{code,msg,success:code===200,data}});
  });
  await page.setViewportSize({width:390,height:844});await page.goto('http://127.0.0.1:4188/app/login');
  await page.getByRole('button',{name:'没有账号？注册账号'}).click();
  const form=page.locator('.account-form');
  if(await form.getByLabel('登录账号 *').getAttribute('placeholder')!=='账号')throw Error('old account placeholder');
  if(await form.getByLabel('密码 *',{exact:true}).inputValue()!=='admin@123')throw Error('wrong default password');
  await form.getByLabel('登录账号 *').fill('staff_123');await form.getByLabel('姓名 *').fill('测试用户');
  await form.getByRole('button',{name:'注册',exact:true}).click();
  await form.getByRole('alert').filter({hasText:'至少填写一项'}).waitFor();if(registrations)throw Error('empty contact submitted');
  await form.getByLabel('手机号',{exact:true}).fill('13800138000');await form.getByRole('button',{name:'注册',exact:true}).click();
  await form.getByRole('alert').filter({hasText:'手机号已被使用'}).waitFor();
  await form.getByLabel('手机号',{exact:true}).fill('');await form.getByLabel('邮箱',{exact:true}).fill('used@example.com');
  await form.getByRole('button',{name:'注册',exact:true}).click();await form.getByRole('alert').filter({hasText:'邮箱已被使用'}).waitFor();
  await page.screenshot({path:'output/playwright/register-contact-required.png'});
  await form.getByLabel('邮箱',{exact:true}).fill('new@example.com');await form.getByRole('button',{name:'注册',exact:true}).click();await form.waitFor({state:'hidden'});
  checks.push('account placeholder/default password; blank contact blocked; duplicate phone/email feedback; email-only registration');
  await page.evaluate(()=>sessionStorage.setItem('fixture-auth','1'));await page.goto('http://127.0.0.1:4188/app/chat');
  await page.getByText('这是最新一条消息，键盘弹出后应保持可见',{exact:true}).waitFor();
  const distance=()=>page.locator('.messages').evaluate(el=>el.scrollHeight-el.scrollTop-el.clientHeight);
  await page.waitForTimeout(600);if(await distance()>5)throw Error('initial scroll not latest');
  await page.locator('.composer textarea').focus();
  await page.evaluate(()=>window.__nativeEmit('Keyboard','keyboardWillShow',{keyboardHeight:350}));
  await page.waitForTimeout(650);
  if(await distance()>5)throw Error('keyboard shrink lost bottom: '+await distance());
  const box=await page.getByText('这是最新一条消息，键盘弹出后应保持可见',{exact:true}).boundingBox(),composer=await page.locator('.composer').boundingBox();
  if(box.y+box.height>composer.y+2)throw Error('latest message hidden behind composer');
  await page.screenshot({path:'output/playwright/keyboard-latest-visible.png'});
  await page.locator('.messages').evaluate(el=>{el.dispatchEvent(new Event('pointerdown'));el.scrollTop=0;});
  await page.waitForTimeout(150);await page.getByRole('button',{name:'回到最新',exact:true}).waitFor();
  await page.waitForTimeout(500);if(await distance()<100)throw Error('manual history scroll was pulled back');
  await page.evaluate(()=>window.__nativeEmit('Keyboard','keyboardWillHide',{}));await page.waitForTimeout(650);
  if(await distance()<100)throw Error('keyboard hide interrupted manual history reading');
  await page.getByRole('button',{name:'回到最新',exact:true}).click();
  await page.evaluate(()=>window.__nativeEmit('Keyboard','keyboardWillShow',{keyboardHeight:350}));await page.waitForTimeout(650);
  await page.evaluate(()=>window.__nativeEmit('Keyboard','keyboardWillHide',{}));await page.waitForTimeout(650);
  if(await distance()>5)throw Error('keyboard hide lost bottom');
  await page.setViewportSize({width:320,height:740});await page.locator('.composer textarea').focus();
  await page.evaluate(()=>window.__nativeEmit('Keyboard','keyboardWillShow',{keyboardHeight:300}));await page.waitForTimeout(650);
  if(await distance()>5)throw Error('320px keyboard lost bottom');
  checks.push('390/320 keyboard show/hide follows latest; manual history reading remains undisturbed');
  await page.evaluate(()=>window.__nativeEmit('Keyboard','keyboardWillHide',{}));await page.goto('http://127.0.0.1:4188/app/event/99');
  await page.getByText('事件时间：按接收人分别设置',{exact:false}).waitFor();
  if(await page.locator('.branch-event-time').count()!==2)throw Error('per-recipient dates missing');
  await page.screenshot({path:'output/playwright/event-distinct-recipient-times.png'});
  checks.push('creator differing-time label and individual branch dates');
  if(errors.length)throw Error(errors.join('\n'));return{checks,registrations,errors};
}
