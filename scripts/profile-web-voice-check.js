async page=>{
  const origin='http://127.0.0.1:4189';let uploads=0,sends=0,rejectUpload=false;const errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.context().addCookies([{name:'saber-access-token',value:'offline-voice-fixture',url:origin}]);
  await page.addInitScript(()=>{
    localStorage.setItem('saber-token',JSON.stringify({dataType:'string',content:'offline-voice-fixture'}));
    navigator.mediaDevices.getUserMedia=async()=>{
      const context=new AudioContext(),osc=context.createOscillator(),gain=context.createGain(),dest=context.createMediaStreamDestination();
      osc.frequency.value=330;gain.gain.value=window.__silentAudio?0:.2;osc.connect(gain);gain.connect(dest);osc.start();await context.resume();
      window.__audioStream=dest.stream;window.__audioContext=context;return dest.stream;
    };
  });
  await page.route('**/api/**',async route=>{
    const url=route.request().url();let data={},code=200,msg='';
    if(url.includes('/bootstrap'))data={user:{id:'1',name:'隔离测试',account:'fixture'},activeEventCount:0};
    else if(url.includes('/audio/transcriptions')){uploads++;await page.waitForTimeout(1200);if(rejectUpload){code=400;msg='语音服务未返回识别文字';}else data={text:'明天下午三点提醒我开会'};}
    else if(url.includes('/chat/stream')||url.includes('/chat/send'))sends++;
    else if(url.includes('/chat/messages'))data=[];
    else if(url.includes('/chat/sync'))data={revision:'fixture',messages:[]};
    await route.fulfill({json:{code,success:code===200,msg,data}});
  });
  await page.setViewportSize({width:390,height:844});await page.goto(origin+'/app/chat');
  const input=page.locator('.composer textarea');await input.fill('原有草稿');
  await page.getByRole('button',{name:'语音输入',exact:true}).click();
  await page.waitForFunction(()=>[...document.querySelectorAll('.voice-line i')].some(e=>parseFloat(e.style.height)>5));
  const confirmBounds=await page.getByRole('button',{name:'确认并转文字',exact:true}).boundingBox(),cancelBounds=await page.locator('.voice-inline .voice-cancel').boundingBox();
  if(Math.abs(confirmBounds.width-cancelBounds.width)>1||confirmBounds.height!==cancelBounds.height)throw Error('confirm/cancel dimensions differ');
  await page.waitForTimeout(900);await page.screenshot({path:'output/playwright/web-voice-signal-390.png'});
  await page.getByRole('button',{name:'确认并转文字',exact:true}).click();
  await page.getByText('麦克风已关闭，无需继续说话',{exact:true}).waitFor();
  if(await page.locator('.voice-line').count())throw Error('recording waveform still visible while transcribing');
  if(!await page.evaluate(()=>window.__audioStream.getTracks().every(t=>t.readyState==='ended')))throw Error('microphone still open during transcription');
  await page.screenshot({path:'output/playwright/interaction-transcribing-390.png'});
  await page.waitForFunction(()=>document.querySelector('.composer textarea').value==='原有草稿\n明天下午三点提醒我开会');
  if(uploads!==1||sends!==0)throw Error('voice unexpectedly auto-sent');
  if(!await page.evaluate(()=>window.__audioStream.getTracks().every(t=>t.readyState==='ended')))throw Error('microphone not released');
  await page.evaluate(async()=>{await window.__audioContext.close();window.__silentAudio=true;});
  await page.getByRole('button',{name:'语音输入',exact:true}).click();await page.getByRole('button',{name:'确认并转文字',exact:true}).waitFor();await page.waitForTimeout(900);
  await page.getByRole('button',{name:'确认并转文字',exact:true}).click();await page.getByRole('alert').filter({hasText:'录音中未采集到声音'}).waitFor();
  if(uploads!==1)throw Error('silent recording uploaded');
  if(!await page.evaluate(()=>window.__audioStream.getTracks().every(t=>t.readyState==='ended')))throw Error('silent microphone not released');
  await page.evaluate(()=>window.__audioContext.close());
  await page.waitForTimeout(3500);
  rejectUpload=true;await page.evaluate(()=>window.__silentAudio=false);
  await page.getByRole('button',{name:'语音输入',exact:true}).click();
  await page.getByRole('button',{name:'确认并转文字',exact:true}).waitFor();await page.waitForTimeout(900);
  await page.getByRole('button',{name:'确认并转文字',exact:true}).click();
  await page.getByRole('alert').filter({hasText:'语音服务未返回识别文字'}).waitFor();
  if(await page.locator('.voice-inline').count()||await page.locator('.audio-file').count())throw Error('error panel or audio upload remains');
  if(await page.locator('.el-message--error').count()!==1)throw Error('missing or duplicate transcription toast');
  if(await input.inputValue()!=='原有草稿\n明天下午三点提醒我开会')throw Error('error erased draft');
  await page.screenshot({path:'output/playwright/web-voice-error-390.png'});
  if(errors.length)throw Error(errors.join(';'));
  return {checks:['real PCM drives waveform; equal confirm/cancel buttons','transcript preserves draft without auto-send','silent recording rejected before upload','mic tracks released','server failure returns composer; one toast; no upload fallback'],uploads,sends,errors};
}
