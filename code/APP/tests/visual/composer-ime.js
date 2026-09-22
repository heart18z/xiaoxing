// Run after replica-fixture.js; all requests use local fixtures.
async page => {
  const base=page.url().split('/').slice(0,3).join('/');
  let refreshes=0;
  const submitted=[];
  await page.route('**/api/**/chat/messages?*',route=>route.fulfill({json:{code:200,data:[{id:'refresh-'+(++refreshes),messageRole:'assistant',messageType:'TEXT',content:'后台刷新 '+refreshes,payload:{}}]}}));
  await page.route('**/api/**/chat/sync?*',route=>route.fulfill({json:{code:200,data:{revision:String(++refreshes),messages:[{id:'refresh-'+refreshes,messageRole:'assistant',messageType:'TEXT',content:'后台刷新 '+refreshes,payload:{}}]}}}));
  await page.route('**/api/**/chat/jobs/status',route=>route.fulfill({json:{code:200,data:{status:'NOT_FOUND'}}}));
  await page.route('**/api/**/chat/jobs/submit',route=>{
    submitted.push(JSON.parse(route.request().postData()).content);
    return route.fulfill({json:{code:200,data:{status:'SUCCEEDED',result:{reply:'已收到'}}}});
  });
  await page.goto(base+'/#/pages/chat/index');await page.reload();
  await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});
  const input=page.locator('#chat-compose-input textarea');
  await input.fill('王处汇报');
  await input.evaluate(el=>{
    window.imeOriginalInput=el;
    el.dispatchEvent(new CompositionEvent('compositionstart',{bubbles:true,data:''}));
    el.value='王处汇报ding zai';
    el.dispatchEvent(new CompositionEvent('compositionupdate',{bubbles:true,data:'ding zai'}));
    el.dispatchEvent(new InputEvent('input',{bubbles:true,data:'ding zai',isComposing:true,inputType:'insertCompositionText'}));
    el.dispatchEvent(new KeyboardEvent('keydown',{bubbles:true,key:'Enter',isComposing:true,keyCode:229}));
  });
  await page.waitForTimeout(7200);
  if(refreshes<2)throw Error('Background refresh did not run');
  if(await input.inputValue()!=='王处汇报ding zai')throw Error('Uncommitted Pinyin was overwritten');
  if(!await input.evaluate(el=>el===window.imeOriginalInput))throw Error('Input was remounted');
  if(submitted.length)throw Error('IME confirmation sent a message');
  await input.evaluate(el=>{
    el.value='王处汇报定在';
    el.dispatchEvent(new CompositionEvent('compositionend',{bubbles:true,data:'定在'}));
    el.dispatchEvent(new InputEvent('input',{bubbles:true,data:'定在',isComposing:false}));
  });
  await page.locator('[aria-label="发送"]').click();
  await page.waitForTimeout(700);
  if(await input.inputValue()!=='')throw Error('First send did not clear');
  await input.fill('第二条消息');
  await page.locator('[aria-label="发送"]').click();
  await page.waitForTimeout(700);
  if(await input.inputValue()!=='')throw Error('Repeated send did not clear');
  if(JSON.stringify(submitted)!==JSON.stringify(['王处汇报定在','第二条消息']))throw Error('Wrong submitted content '+JSON.stringify(submitted));
  return {refreshes,submitted,inputPreserved:true,repeatedClear:true};
}
