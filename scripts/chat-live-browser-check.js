// Run after the local calendar-chat-browser fixture and a fresh chat-page snapshot.
async page=>{
  let submitted=false;
  const reply='你好，我在。可以直接告诉我你想提醒的事情，我会帮你整理时间和安排。';
  await page.route('**/chat/jobs/status',route=>route.fulfill({json:{code:200,data:submitted?{status:'RUNNING',streamSupported:true}:{status:'NOT_FOUND'}}}));
  await page.route('**/chat/jobs/submit',route=>{submitted=true;return route.fulfill({json:{code:200,data:{status:'QUEUED',streamSupported:true}}});});
  const history=[{id:'1',messageRole:'user',messageType:'TEXT',content:'你好'},{id:'2',messageRole:'assistant',messageType:'TEXT',content:reply}];
  await page.route('**/chat/messages*',route=>route.fulfill({json:{code:200,data:history}}));
  await page.route('**/chat/sync*',route=>route.fulfill({json:{code:200,data:{revision:'1',messages:history}}}));
  await page.emulateMedia({reducedMotion:'no-preference'});
  await page.evaluate(reply=>{
    const originalFetch=window.fetch.bind(window);window.__liveNotices=[];window.__liveReplies=[];window.__watchCount=0;
    new MutationObserver(()=>{
      const banner=document.querySelector('.chat-recovery')?.textContent||'';if(banner)window.__liveNotices.push(banner);
      const text=document.querySelector('.is-streaming .markdown-content')?.textContent||'';if(text)window.__liveReplies.push(text);
    }).observe(document.body,{subtree:true,childList:true,characterData:true});
    window.fetch=(url,options)=>{
      if(!String(url).endsWith('/chat/jobs/watch'))return originalFetch(url,options);
      window.__watchCount++;
      const encoder=new TextEncoder();let closed=false;const timers=[];
      return Promise.resolve(new Response(new ReadableStream({start(controller){
        const emit=event=>{if(!closed)controller.enqueue(encoder.encode('data:'+JSON.stringify(event)+'\n\n'));};
        emit({type:'ready'});
        timers.push(setTimeout(()=>emit({type:'snapshot',reasoning:'正在理解'}),80));
        timers.push(setTimeout(()=>emit({type:'snapshot',reasoning:'正在理解你的需求'}),250));
        timers.push(setTimeout(()=>{emit({type:'result',data:{status:'SUCCEEDED',result:{reply}}});if(!closed){closed=true;controller.close();}},700));
      },cancel(){closed=true;timers.forEach(clearTimeout);}}),{headers:{'Content-Type':'text/event-stream'}}));
    };
  },reply);
  await page.getByRole('textbox',{name:'说说你想提醒的事…'}).fill('你好');
  await page.getByRole('button',{name:'发送',exact:true}).click();
  await page.getByText(reply,{exact:true}).waitFor();
  await page.getByRole('button',{name:'发送',exact:true}).waitFor();
  const state=await page.evaluate(()=>({notices:window.__liveNotices,replies:[...new Set(window.__liveReplies)],watchCount:window.__watchCount}));
  if(state.notices.length)throw Error('Normal conversation showed a banner: '+state.notices.join('|'));
  if(state.replies.length<3)throw Error('Reply still appears in one block');
  if(state.watchCount!==1)throw Error('Expected one read-only observation stream');
  await page.screenshot({path:'output/playwright/chat-live-20260916.png'});
  return{noQueueBanner:true,progressiveReplyFrames:state.replies.length,watchCount:state.watchCount,composerUnlocked:true};
}
