async page=>{
  const errors=[],checks=[];let authSuccess=false,suggestions=0,historySlow=false,historyReads=0,streamSends=0;
  page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{
    const listeners={};let sequence=0,value=localStorage.getItem('fixture-native-vault')||'{}';
    window.__nativeEmit=(plugin,event,data)=>{for(const x of Object.values(listeners))if(x.plugin===plugin&&x.event===event)x.cb(data);};
    window.CapacitorCustomPlatform={name:'ios'};
    window.Capacitor={PluginHeaders:[{name:'XiaoxingNative',methods:['readState','writeState','context']},{name:'PushNotifications',methods:['checkPermissions','register','removeListener']},{name:'App',methods:['removeListener']},{name:'Keyboard',methods:['removeListener']}].map(p=>({...p,methods:[...p.methods.map(name=>({name,rtype:'promise'})),{name:'addListener',rtype:'callback'}]})),
      nativeCallback:(plugin,method,o,cb)=>{const id=String(++sequence);listeners[id]={plugin,event:o.eventName,cb};return id;},
      nativePromise:async(plugin,method,o)=>{if(method==='readState')return{value};if(method==='writeState'){value=o.value;localStorage.setItem('fixture-native-vault',value);return{};}if(method==='context')return{bundleId:'com.dfyj.xiaoxing',environment:'production'};if(method==='checkPermissions')return{receive:'denied'};return{};}};
  });
  const committedReply='已处理 1 个事件。\n已更新：明天上午九点召开项目会议。';
  await page.route('**/api/**',async route=>{
    const path=route.request().url().split('?')[0];let data={},json;
    if(path.endsWith('/suggest-account')){suggestions++;if(suggestions===2)await page.waitForTimeout(650);data='20260915'+String(suggestions).padStart(2,'0');}
    else if(path.includes('/oauth/token'))json=authSuccess?{access_token:'offline',refresh_token:'offline-refresh',expires_in:259200,user_id:'1',user_name:'测试用户',role_name:'app_user',tenant_id:'000000'}:{error_description:'账号或密码不正确'};
    else if(path.endsWith('/bootstrap'))data={user:{id:'1',name:'测试用户',account:'2026091503'},activeEventCount:1};
    else if(path.endsWith('/chat/messages')){historyReads++;if(historySlow)await page.waitForTimeout(3000);data=historySlow?[{id:'1',messageRole:'user',messageType:'TEXT',content:'改到明天九点',isRead:true},{id:'2',messageRole:'assistant',messageType:'TEXT',content:committedReply,isRead:true}]:[];}
    else if(path.endsWith('/chat/sync'))data={revision:'test',messages:[]};
    else if(path.endsWith('/chat/jobs/status'))data=streamSends?{status:'SUCCEEDED',result:{reply:committedReply}}:{status:'NOT_FOUND'};
    else if(path.endsWith('/chat/jobs/submit')){streamSends++;historySlow=true;data={status:'SUCCEEDED',result:{reply:committedReply}};}
    else if(path.endsWith('/chat/stream')){
      streamSends++;historySlow=true;
      const body=[{type:'ready'},{type:'reasoning',content:'检查事件时间'},{type:'delta',content:committedReply},{type:'result',data:{reply:committedReply,intent:'update_event'}}].map(x=>'data:'+JSON.stringify(x)+'\n\n').join('');
      await route.fulfill({contentType:'text/event-stream',body});return;
    }
    else if(path.endsWith('/friends/list'))data=[];
    else if(path.endsWith('/friends/requests'))data={incoming:[],outgoing:[]};
    await route.fulfill({json:json||{code:200,success:true,data}});
  });
  await page.setViewportSize({width:390,height:844});await page.goto('http://127.0.0.1:4188/app/login');
  await page.getByRole('button',{name:'没有账号？注册账号'}).click();
  const dialog=page.getByRole('dialog',{name:'创建小醒账号'}),form=dialog.locator('form');await dialog.waitFor();
  await page.waitForTimeout(500);
  if(await form.getByLabel('登录账号 *').inputValue()!=='2026091501')throw Error('default account not filled');
  await page.screenshot({path:'output/playwright/account-polish-register.png'});
  await form.getByLabel('姓名 *').fill('测试用户');
  const email=form.getByLabel('邮箱',{exact:true});await email.focus();
  await page.evaluate(()=>window.__nativeEmit('Keyboard','keyboardWillShow',{keyboardHeight:350}));await page.waitForTimeout(650);
  await email.fill('test@example.com');
  const rect=await email.boundingBox();if(rect.y<0||rect.y+rect.height>844-350)throw Error('email covered by keyboard: '+JSON.stringify(rect));
  await page.screenshot({path:'output/playwright/account-polish-keyboard.png'});
  await page.setViewportSize({width:320,height:740});await page.waitForTimeout(600);
  const small=await email.boundingBox();if(small.y+small.height>740-350||small.x+small.width>320)throw Error('320px email clipped');
  checks.push('generated editable account; compact grid; email visible at 390/320 with simulated native keyboard');
  await page.evaluate(()=>window.__nativeEmit('Keyboard','keyboardWillHide',{}));await dialog.getByRole('button',{name:'关闭此对话框'}).click();
  await dialog.waitFor({state:'hidden'});await page.setViewportSize({width:390,height:844});
  await page.getByRole('button',{name:'没有账号？注册账号'}).click();await dialog.waitFor();await form.getByLabel('登录账号 *').fill('my_account');await page.waitForTimeout(800);
  if(await form.getByLabel('登录账号 *').inputValue()!=='my_account')throw Error('late suggestion overwrote edit');
  await dialog.getByRole('button',{name:'关闭此对话框'}).click();await dialog.waitFor({state:'hidden'});
  await page.getByLabel('账号',{exact:true}).fill('2026091503');await page.getByLabel('密码',{exact:true}).fill('fixture-password');await page.getByRole('button',{name:'登录',exact:true}).click();
  await page.getByText('账号或密码不正确',{exact:true}).waitFor();
  if(await page.locator('.el-message').count()!==1)throw Error('duplicate login tips');
  await page.waitForTimeout(400);
  await page.screenshot({path:'output/playwright/account-polish-login-tip.png'});
  authSuccess=true;await page.getByRole('button',{name:'登录',exact:true}).click();await page.waitForURL('**/app/chat');
  await page.locator('.composer textarea').fill('改到明天九点');await page.getByRole('button',{name:'发送',exact:true}).click();
  await page.getByText('已处理 1 个事件。',{exact:false}).waitFor();
  await page.getByRole('button',{name:'发送',exact:true}).waitFor({timeout:1000});
  if(streamSends!==1)throw Error('mutation retried');
  checks.push('single friendly login tip; committed reply unlocks send before 3-second history response');
  await page.getByRole('link',{name:'我的',exact:true}).click();await page.getByRole('button',{name:'退出登录',exact:true}).click();
  const confirm=page.getByRole('dialog',{name:'退出当前账号？'});await confirm.waitFor();await page.waitForTimeout(350);
  await page.screenshot({path:'output/playwright/account-polish-logout.png'});
  await confirm.getByRole('button',{name:'暂不退出'}).click();await confirm.waitFor({state:'hidden'});if(!page.url().endsWith('/app/me'))throw Error('cancel logged out');
  await page.getByRole('button',{name:'退出登录',exact:true}).click();await confirm.getByRole('button',{name:'退出登录',exact:true}).click();await page.waitForURL('**/app/login');
  await page.reload();if(await page.getByLabel('账号',{exact:true}).inputValue()!=='2026091503')throw Error('last login not remembered');
  if(await page.getByLabel('密码',{exact:true}).inputValue()!=='')throw Error('password persisted');
  checks.push('logout cancel/confirm; remembered account survives reload, password empty');
  if(errors.length)throw Error(errors.join('\n'));return{checks,historyReads,streamSends,errors};
}
