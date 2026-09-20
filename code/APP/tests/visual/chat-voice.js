async page => {
  let finishUpload;
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.route('**/api/app/reminder/audio/transcriptions',async route=>{
    await new Promise(resolve=>finishUpload=resolve);
    await route.fulfill({json:{code:200,data:{text:'识别的语音'}}});
  });
  await page.evaluate(()=>{
    window.voiceStopped=0;
    Object.defineProperty(navigator.mediaDevices,'getUserMedia',{configurable:true,value:async()=>({getTracks:()=>[{stop:()=>window.voiceStopped++}]})});
    window.MediaRecorder=class {
      static isTypeSupported(){return true;}
      constructor(){this.state='inactive';this.mimeType='audio/webm';}
      start(){this.state='recording';}
      stop(){this.state='inactive';this.ondataavailable?.({data:new Blob(['audio'])});queueMicrotask(()=>this.onstop?.());}
    };
  });
  await page.locator('.compose-input textarea').fill('原有草稿');
  await page.locator('.compose-action').nth(1).click();
  await page.getByText('正在录音，说完后点击“完成”',{exact:true}).waitFor();
  await page.waitForTimeout(600);
  await page.getByText('完成',{exact:true}).click();
  await page.getByText('正在转写，请稍候…',{exact:true}).waitFor();
  if(!await page.evaluate(()=>window.voiceStopped>0))throw Error('Microphone not released before upload');
  await page.getByText('取消',{exact:true}).click();finishUpload();
  await page.waitForTimeout(500);
  if(await page.locator('.compose-input textarea').inputValue()!=='原有草稿')throw Error('Cancelled transcription overwrote draft');
  await page.locator('.compose-action').nth(1).click();await page.waitForTimeout(600);await page.getByText('完成',{exact:true}).click();
  await page.getByText('正在转写，请稍候…',{exact:true}).waitFor();finishUpload();
  await page.waitForFunction(()=>document.querySelector('.compose-input textarea').value.includes('识别的语音'));
  if(await page.locator('.compose-input textarea').inputValue()!=='原有草稿\n识别的语音')throw Error('Speech lost existing draft');
  if(await page.locator('.send-busy').count())throw Error('Speech sent automatically');
  if(errors.length)throw Error(errors.join(';'));
  return {checks:['录音结束释放麦克风','转写取消后迟到结果不覆盖草稿','语音只追加草稿不自动发送'],errors};
}
