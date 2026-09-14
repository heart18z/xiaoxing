async (page) => {
  const errors=[], checks=[];let pending=false,updated=false,signupCount=0,passwordCount=0,eventReads=0;
  let fixtureUser={id:'1',name:'测试用户',account:'fixture',phone:'13800138000',email:'fixture@example.com'},profileWrites=0;
  page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{
    const listeners={};let sequence=0;
    let value=sessionStorage.getItem('fixture-anonymous')==='1'?'{}':JSON.stringify({token:'offline',userInfo:{user_id:'1',user_name:'测试用户',role_name:'app_user'}});
    window.__nativeEmit=(plugin,event,data)=>{for(const item of Object.values(listeners))if(item.plugin===plugin&&item.event===event)item.cb(data);};
    window.CapacitorCustomPlatform={name:'ios'};
    window.Capacitor={
      PluginHeaders:[
        {name:'XiaoxingNative',methods:['readState','writeState','context','openSettings']},
        {name:'XiaoxingSpeech',methods:['start','stop','removeListener']},
        {name:'PushNotifications',methods:['checkPermissions','requestPermissions','register','removeAllDeliveredNotifications','removeListener']},
        {name:'App',methods:['removeListener']},{name:'Keyboard',methods:['removeListener']}
      ].map(p=>({...p,methods:[...p.methods.map(name=>({name,rtype:'promise'})),{name:'addListener',rtype:'callback'}]})),
      nativePromise:async(plugin,method,options)=>{
        if(method==='readState')return{value};
        if(method==='writeState'){value=options.value;return{};}
        if(method==='context')return{bundleId:'com.dfyj.xiaoxing',environment:'production',appVersion:'1.0'};
        if(method==='checkPermissions')return{receive:'granted'};
        if(method==='removeListener'){delete listeners[options.callbackId];return{};}
        if(plugin==='PushNotifications'&&method==='register')setTimeout(()=>window.__nativeEmit(plugin,'registration',{value:'fixture-apns'}),5);
        if(plugin==='XiaoxingSpeech'&&method==='start')window.__speechSession=options.session;
        if(plugin==='XiaoxingSpeech'&&method==='stop')setTimeout(()=>window.__nativeEmit(plugin,'finished',{session:options.session,text:'实时识别文字',cancelled:!!options.cancel,error:''}),5);
        return{};
      },
      nativeCallback:(plugin,method,options,cb)=>{const id=String(++sequence);listeners[id]={plugin,event:options.eventName,cb};return id;}
    };
  });
  const prose='这是一段检查思考内容行宽和高度的测试文字。'.repeat(35);
  const messages=[{id:'11',messageRole:'assistant',messageType:'TEXT',content:'正常回复',isRead:true},{id:'12',messageRole:'assistant',messageType:'TEXT',content:'',reasoningContent:prose,isRead:true}];
  await page.route('**/api/**',async route=>{
    if(route.request().url().includes('/src/')){await route.continue();return;}
    const path='/api/'+route.request().url().split('/api/')[1].split('?')[0];let data={},code=200,msg='';
    if(path.endsWith('/bootstrap'))data={user:fixtureUser,activeEventCount:1,sentActiveEventCount:1,receivedActiveEventCount:0,pendingFriendRequests:pending?1:0};
    else if(path.endsWith('/profile/update')){profileWrites++;const input=route.request().postDataJSON();if('account' in input||'id' in input||'avatar' in input)throw Error('profile unexpectedly sent identity/avatar');fixtureUser={...fixtureUser,...input};data=fixtureUser;}
    else if(path.endsWith('/account/register')){signupCount++;if(signupCount===1){code=400;msg='账号已存在，请更换人员号';}else data=true;}
    else if(path.endsWith('/account/password')){passwordCount++;data=true;}
    else if(path.endsWith('/chat/messages'))data=messages;
    else if(path.endsWith('/chat/sync'))data={revision:'fixture',messages,unreadCount:0};
    else if(path.endsWith('/friends/list'))data=[];
    else if(path.endsWith('/friends/requests'))data={incoming:pending?[{id:'9',name:'新的好友申请',permissionMode:'MUTUAL',requestType:'FRIEND'}]:[],outgoing:[]};
    else if(path.endsWith('/events')){eventReads++;data=[{id:'99',eventNo:'FIXTURE99',eventStatus:'ACTIVE',eventSummary:updated?'静默刷新后的事件':'原事件',recipientCount:1,activeBranchCount:1}];}
    else if(path.includes('/settings/'))data={language:'zh-cn',models:[],llmMode:'SYSTEM',speechMode:'SYSTEM'};
    else if(path.endsWith('/push/register'))data={bindingId:'fixture',userId:'1',backendReady:true};
    await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({code,msg,success:code===200,data})});
  });
  await page.evaluate(()=>sessionStorage.setItem('fixture-anonymous','1'));
  await page.setViewportSize({width:320,height:844});
  await page.goto('http://127.0.0.1:4188/app/login');
  await page.getByRole('button',{name:'没有账号？注册账号'}).click();
  const form=page.locator('.account-form');
  await form.getByLabel('登录账号 *').fill('x'.repeat(50));
  if((await form.getByLabel('登录账号 *').inputValue()).length!==32)throw Error('account maxlength missing');
  await form.getByLabel('登录账号 *').fill('staff_123');
  await form.getByLabel('姓名 *').fill('测试用户');
  if(!(await form.getByLabel('密码 *',{exact:true}).inputValue()))throw Error('default password missing');
  const registrationPassword=form.getByLabel('密码 *',{exact:true}), originalPassword=await registrationPassword.inputValue();
  await form.getByRole('button',{name:'显示密码',exact:true}).click();
  if(await registrationPassword.getAttribute('type')!=='text'||await registrationPassword.inputValue()!==originalPassword)throw Error('password reveal changed content');
  await page.screenshot({path:'output/playwright/interaction-password-eye-320.png'});
  await form.getByRole('button',{name:'隐藏密码',exact:true}).click();
  if(await registrationPassword.getAttribute('type')!=='password')throw Error('password not masked');
  checks.push('registration password eye reveals and masks without changing value');
  await form.getByRole('button',{name:'注册',exact:true}).click();
  await form.getByRole('alert').filter({hasText:'账号已存在'}).waitFor();
  await page.screenshot({path:'output/playwright/upgrade-register-320.png'});
  await form.getByLabel('登录账号 *').fill('staff_456');
  await form.getByRole('button',{name:'注册',exact:true}).click();
  await form.waitFor({state:'hidden'});checks.push('registration bounds, duplicate error, success');
  await page.evaluate(()=>sessionStorage.removeItem('fixture-anonymous'));
  await page.goto('http://127.0.0.1:4188/app/me');
  if(await page.getByRole('button',{name:/重置密码/}).count())throw Error('password still floating on profile');
  await page.getByRole('button',{name:'编辑资料',exact:true}).click();
  const profileForm=page.locator('.profile-edit-form');
  if(await profileForm.getByLabel('手机号（选填）').inputValue()!=='13800138000'||await profileForm.getByLabel('邮箱（选填）').inputValue()!=='fixture@example.com')throw Error('contacts not hydrated');
  await profileForm.getByLabel('姓名 *').fill('更新姓名');
  await profileForm.getByLabel('手机号（选填）').fill('123');await profileForm.getByRole('button',{name:'保存资料'}).click();await profileForm.getByRole('alert').waitFor();
  if(profileWrites!==0)throw Error('invalid phone reached server');
  await profileForm.getByLabel('手机号（选填）').fill('');await profileForm.getByLabel('邮箱（选填）').fill('');
  await page.screenshot({path:'output/playwright/profile-edit-320.png'});
  await profileForm.getByRole('button',{name:'保存资料'}).click();await profileForm.waitFor({state:'hidden'});
  await page.getByRole('heading',{name:'更新姓名'}).waitFor();
  const toast=page.locator('.el-message').filter({hasText:'个人资料已保存'});await toast.waitFor();await page.waitForTimeout(400);
  if((await toast.boundingBox()).y<60)throw Error('toast too close to island');
  await page.screenshot({path:'output/playwright/profile-toast-320.png'});
  if(profileWrites!==1||fixtureUser.phone!==''||fixtureUser.email!=='')throw Error('contact clear not saved');
  checks.push('profile fields hydrate, validate, save and clear; toast below island');
  await page.getByRole('button',{name:'设置',exact:true}).click();
  await page.getByRole('button',{name:/重置密码/}).click();
  await form.getByLabel('原密码 *').fill('old-fixture');await form.getByLabel('新密码 *',{exact:true}).fill('new-fixture');await form.getByLabel('确认新密码 *').fill('mismatch-fixture');
  await form.getByRole('button',{name:'确认修改'}).click();await form.getByRole('alert').filter({hasText:'两次新密码不一致'}).waitFor();
  if(passwordCount!==0)throw Error('mismatch reached backend');
  await page.screenshot({path:'output/playwright/upgrade-password-320.png'});
  await page.getByRole('button',{name:/Close this dialog|关闭此对话框/}).click();
  await page.goto('http://127.0.0.1:4188/app/me');
  await page.getByRole('button',{name:'退出登录',exact:true}).click();
  await page.getByRole('button',{name:'取消',exact:true}).click();
  if(!page.url().endsWith('/app/me'))throw Error('logout cancellation failed');checks.push('password confirmation and logout cancellation');
  await page.getByRole('button',{name:'申请待通过'}).click();pending=true;
  await page.getByText('新的好友申请',{exact:true}).waitFor({timeout:16000});checks.push('friend request arrives without leaving profile');
  await page.screenshot({path:'output/playwright/upgrade-friends-320.png'});
  await page.goto('http://127.0.0.1:4188/app/events');await page.getByRole('heading',{name:'原事件'}).waitFor();updated=true;
  await page.getByRole('heading',{name:'静默刷新后的事件'}).waitFor({timeout:16000});checks.push('event silently refreshes');
  await page.goto('http://127.0.0.1:4188/app/settings');await page.locator('.notification-settings').waitFor();
  await page.getByRole('button',{name:'开启 / 检查通知',exact:true}).click();
  await page.locator('.check-feedback').filter({hasText:'设备已绑定'}).waitFor();
  for(const width of [320,390]){
    await page.setViewportSize({width,height:844});
    const layout=await page.evaluate(()=>{const form=document.querySelector('.settings-form');return{width:innerWidth,scroll:document.documentElement.scrollWidth,form:form.getBoundingClientRect().width,overflow:form.scrollWidth>form.clientWidth+1};});
    if(layout.overflow||layout.scroll>layout.width+1)throw Error('settings overflow '+JSON.stringify(layout));
    await page.screenshot({path:`output/playwright/upgrade-settings-${width}.png`});
  }
  checks.push('settings width 320/390 and notification feedback');
  await page.goto('http://127.0.0.1:4188/app/chat');await page.locator('.thinking-toggle').waitFor();
  if(await page.locator('.thinking-toggle').getAttribute('aria-expanded')!=='true')await page.locator('.thinking-toggle').click();
  const height=await page.locator('.thinking-content').evaluate(el=>el.getBoundingClientRect().height);
  if(height>139)throw Error('thinking panel too tall');
  const input=page.locator('.composer textarea');await input.fill('原有草稿');
  await page.getByRole('button',{name:'语音输入',exact:true}).click();
  await page.waitForFunction(()=>!!window.__speechSession);
  await page.evaluate(()=>window.__nativeEmit('XiaoxingSpeech','result',{session:window.__speechSession,text:'实时'}));
  await page.waitForFunction(()=>document.querySelector('.composer textarea').value==='原有草稿\n实时');
  await page.evaluate(()=>window.__nativeEmit('XiaoxingSpeech','result',{session:window.__speechSession,text:'实时识别文字'}));
  await page.waitForFunction(()=>document.querySelector('.composer textarea').value==='原有草稿\n实时识别文字');
  await page.screenshot({path:'output/playwright/upgrade-voice-390.png'});
  await page.evaluate(()=>window.__nativeEmit('XiaoxingSpeech','level',{session:window.__speechSession,level:85}));
  await page.waitForFunction(()=>[...document.querySelectorAll('.voice-line i')].some(el=>el.getBoundingClientRect().height>15));
  await page.getByRole('button',{name:'确认并转文字',exact:true}).click();await page.locator('.voice-inline').waitFor({state:'hidden'});
  if(await input.inputValue()!=='原有草稿\n实时识别文字')throw Error('transcription appended more than once');
  checks.push('inline voice partials replace, preserve draft, never send automatically');
  await page.getByRole('button',{name:'语音输入',exact:true}).click();
  await page.locator('.voice-inline').getByRole('button',{name:'确认并转文字',exact:true}).waitFor();
  const cancelledSession=await page.evaluate(()=>window.__speechSession);
  await page.locator('.voice-inline').getByRole('button',{name:'取消',exact:true}).click();
  await page.evaluate(session=>window.__nativeEmit('XiaoxingSpeech','result',{session,text:'不应写入的迟到结果'}),cancelledSession);
  if(await input.inputValue()!=='原有草稿\n实时识别文字')throw Error('cancel erased draft or accepted late result');
  checks.push('cancel preserves draft and rejects late speech callbacks');
  await page.evaluate(()=>window.__nativeEmit('Keyboard','keyboardWillShow',{keyboardHeight:300}));
  await page.waitForFunction(()=>Math.abs(document.querySelector('.sr-shell').getBoundingClientRect().height-(innerHeight-300))<2);
  const composer=await page.locator('.composer').boundingBox();
  if(composer.y+composer.height>545)throw Error('composer under keyboard');
  await page.screenshot({path:'output/playwright/upgrade-keyboard-390.png'});
  await page.evaluate(()=>window.__nativeEmit('Keyboard','keyboardWillHide',{}));
  checks.push('keyboard will-show updates layout before did-show');
  await page.goto('http://127.0.0.1:4188/app/settings');
  await page.getByRole('button',{name:/重置密码/}).click();
  await form.getByLabel('原密码 *').fill('old-fixture');await form.getByLabel('新密码 *',{exact:true}).fill('new-fixture');await form.getByLabel('确认新密码 *').fill('new-fixture');
  await form.getByRole('button',{name:'确认修改'}).click();
  await page.waitForURL('**/app/login');
  if(passwordCount!==1)throw Error('password change not submitted exactly once');
  checks.push('successful password change returns to login');
  if(errors.length)throw Error(errors.join('; '));
  return {checks,signupCount,passwordCount,eventReads,thinkingHeight:height,errors,nativeHardware:'Still requires Xcode build and real iPhone verification'};
}
