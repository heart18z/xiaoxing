// Local mocked APIs only. No live messages or events are created.
async page => {
 await page.unrouteAll({behavior:'ignoreErrors'});await page.setViewportSize({width:390,height:844});
 const errors=[];page.on('pageerror',e=>errors.push(e.stack || e.message));
 const user={id:'9001',name:'测试用户',account:'fixture'};
 let rows=Array.from({length:24},(_,i)=>({id:String(i+1),messageRole:i%2?'assistant':'user',messageType:'TEXT',content:'历史消息 '+i+'，这是用于检查滚动位置的测试内容。'}));
 let started=0,completed=false;
 const reason='只整理当前用户的安排，按时间分别核对会议、报告和客户现场事项。'.repeat(12);
 const reply='你有三个独立事项：9月23日9:53开会，15:30前提交报告，以及9月24日14:00到客户现场。'.repeat(3);
 await page.route('**/api/**',async r=>{
  const path=r.request().url();let data={};
  if(path.includes('/bootstrap'))data={user,aiAvatar:'/avatars/assistant/A3.png',language:'zh-cn'};
  if(path.includes('/chat/messages')||path.includes('/chat/sync'))data=path.includes('/sync')?{messages:rows,revision:'test'}:rows;
  if(path.includes('/jobs/')){
   if(path.includes('/submit'))started=Date.now();
   if(!started)data={status:'NOT_FOUND'};
   else if(Date.now()-started<5000)data={status:'RUNNING',streamSupported:false,reasoning:Date.now()-started>900?reason:''};
   else{data={status:'SUCCEEDED',streamSupported:false,result:{reply,reasoningContent:reason,intent:'chat'}};if(!completed){completed=true;rows=rows.concat([{id:'100',messageRole:'user',messageType:'TEXT',content:'查看我的安排'},{id:'101',messageRole:'assistant',messageType:'TEXT',content:reply,reasoningContent:reason}]);}}
  }
  await r.fulfill({json:{code:200,data}});
 });
 await page.goto('http://localhost:5174');await page.evaluate(()=>{sessionStorage.clear();sessionStorage.setItem('xiaoxing:session',JSON.stringify({token:'fixture',refresh:'fixture',userId:'9001',expiresAt:2000000000000}));});
 await page.goto('http://localhost:5174/#/pages/chat/index');await page.reload();await page.locator('.message').last().waitFor();await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});await page.waitForTimeout(2000);
 const bottom=()=>page.evaluate(()=>{const v=document.querySelector('#chat-scroll').getBoundingClientRect(),end=document.querySelector('#latest').getBoundingClientRect();return {gap:end.bottom-v.bottom,inside:end.top>=v.top};});
 const initial=await bottom();if(!initial.inside||Math.abs(initial.gap)>40)throw Error('Initial scroll '+JSON.stringify(initial));
 await page.locator('.compose-input textarea').fill('查看我的安排');await page.locator('.send').click();
 await page.locator('.reply-progress').waitFor();
 await page.locator('.assistant').last().evaluate(el=>{window.__live=el;window.__samples=[];window.__sampleTimer=setInterval(()=>{const v=document.querySelector('#chat-scroll').getBoundingClientRect(),end=document.querySelector('#latest').getBoundingClientRect();window.__samples.push({gap:end.bottom-v.bottom,top:end.top,time:performance.now()});},40);});
 await page.getByText('正在思考…',{exact:true}).waitFor();await page.waitForTimeout(500);
 await page.screenshot({path:'output/playwright/mobile118-thinking.png'});
 const shine1=await page.locator('.thinking-shimmer').evaluate(el=>getComputedStyle(el).transform);await page.waitForTimeout(100);const shine2=await page.locator('.thinking-shimmer').evaluate(el=>getComputedStyle(el).transform);if(shine1===shine2)throw Error('Shimmer did not move');
 await page.waitForFunction(()=>!document.querySelector('.send-busy'));await page.waitForTimeout(3500);
 const result=await page.evaluate(()=>{clearInterval(window.__sampleTimer);let clipped=0,longest=0;for(const sample of window.__samples){clipped=sample.gap>32?clipped+1:0;longest=Math.max(longest,clipped);}return {sameNode:window.__live===Array.from(document.querySelectorAll('.assistant')).at(-1),longestClippedMs:longest*40,samples:window.__samples.length};});
 const final=await bottom();await page.screenshot({path:'output/playwright/mobile118-reply.png'});
 if(!result.sameNode)throw Error('Live reply component remounted');if(result.longestClippedMs>320)throw Error('Reply hidden below composer '+JSON.stringify(result));if(Math.abs(final.gap)>40)throw Error('Final scroll '+JSON.stringify(final));
 await page.locator('#chat-scroll').evaluate(el=>{el.dispatchEvent(new TouchEvent('touchstart',{bubbles:true,touches:[new Touch({identifier:1,target:el,clientX:100,clientY:200})],changedTouches:[]}));const s=Array.from(el.querySelectorAll('.uni-scroll-view')).find(n=>n.scrollHeight>n.clientHeight);s.scrollTop=0;s.dispatchEvent(new Event('scroll'));});await page.waitForTimeout(400);await page.locator('.latest-link').waitFor();await page.waitForTimeout(600);
 const held=await page.locator('#chat-scroll').evaluate(el=>Array.from(el.querySelectorAll('.uni-scroll-view')).find(n=>n.scrollHeight>n.clientHeight).scrollTop);if(held>5)throw Error('Reader pulled to bottom');
 await page.locator('.latest-link').click();await page.waitForTimeout(400);const resumed=await bottom();if(Math.abs(resumed.gap)>40)throw Error('Return to latest failed');
 if(errors.length)throw Error(JSON.stringify(errors));return {initial,final,resumed,...result,held,errors};
}
