async page => {
  const assert=(v,m)=>{if(!v)throw Error(m)};
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  const image=await page.evaluate(()=>{const c=document.createElement('canvas');c.width=300;c.height=500;const x=c.getContext('2d');x.fillStyle='#dde9ff';x.fillRect(0,0,300,500);x.fillStyle='#3d56bf';x.font='30px sans-serif';x.fillText('Meeting 15:00',15,100);return c.toDataURL('image/png')});
  await page.route('**/api/app/reminder/chat/messages**',route=>route.fulfill({json:{code:200,data:[{id:'picture137',messageRole:'user',messageType:'TEXT',content:'请根据这些图片安排提醒',payload:{fileNames:['a.png','b.png','c.png','d.png','e.png','f.png'],fileIds:['11','12','13','14','15','16']}}]}}));
  await page.route('**/api/app/reminder/files/preview**',route=>route.fulfill({json:{code:200,data:{image}}}));
  const pending=[];
  await page.route('**/api/app/reminder/files/upload',route=>{pending.push(route)});
  await page.goto('http://localhost:5175/#/pages/chat/index');await page.reload();
  await page.locator('.message-images .thumbnail').nth(5).waitFor();
  const boxes=await page.locator('.message-images .attachment-image').evaluateAll(nodes=>nodes.map(n=>{const r=n.getBoundingClientRect();return {x:r.x,y:r.y,width:r.width,height:r.height}}));
  assert(boxes.every(b=>b.width<=104&&b.height<=104),'compact thumbnails');
  assert(boxes[0].y===boxes[1].y&&boxes[2].y>boxes[1].y,'two-column image grid');
  await page.screenshot({path:'output/playwright/polish137-sent-images.png'});
  await page.locator('#chat-compose-input').evaluate((target,data)=>{
    const bytes=Uint8Array.from(atob(data.split(',')[1]),c=>c.charCodeAt(0));
    const dt=new DataTransfer();for(let i=0;i<6;i++)dt.items.add(new File([bytes],'very-long-uploaded-file-name-'+i+'.png',{type:'image/png'}));
    target.dispatchEvent(new ClipboardEvent('paste',{bubbles:true,clipboardData:dt}));
  },image);
  await page.locator('.file-grid .tile').nth(5).waitFor();
  assert(await page.locator('.file-grid .orbit').count()===6,'six loading indicators');
  assert(!(await page.locator('.file-grid').textContent()).includes('very-long'),'no filenames');
  const visible=await page.locator('.file-grid .tile-remove').evaluateAll(nodes=>nodes.every(n=>{const r=n.getBoundingClientRect();return r.left>=0&&r.right<=innerWidth&&r.top>=0&&r.bottom<=innerHeight}));
  assert(visible,'all six remove controls visible');
  await page.screenshot({path:'output/playwright/polish137-compose-images.png'});
  await page.locator('.file-grid .tile-remove').nth(5).click();
  assert(await page.locator('.file-grid .tile').count()===5,'remove pending image');
  for(const [i,route] of pending.entries())await route.fulfill({json:{code:200,data:{id:String(30+i),extractStatus:'SUCCESS'}}});
  await page.waitForFunction(()=>document.querySelectorAll('.file-grid .orbit').length===0);
  assert(await page.locator('.file-grid .tile').count()===5,'late completion never restores removed image');
  await page.setViewportSize({width:320,height:740});
  await page.screenshot({path:'output/playwright/polish137-images-320.png'});
  await page.goto('http://localhost:5175/#/pages/settings/index');
  // Use the public uni storage API rather than relying on its serialized storage format.
  await page.evaluate(()=>uni.setStorageSync('xiaoxing-language-mode','en-us'));
  await page.reload();await page.getByText('Phone notifications',{exact:true}).waitFor();
  assert(await page.getByText('Enable / check notifications',{exact:true}).count()===1,'English notification action');
  assert(await page.getByText('手机通知',{exact:true}).count()===0,'no untranslated notification heading');
  await page.setViewportSize({width:402,height:874});
  await page.waitForTimeout(1800);
  await page.screenshot({path:'output/playwright/polish137-english.png'});
  await page.locator('.select-field').first().click();
  await page.getByText('Follow system',{exact:true}).click();
  assert(await page.evaluate(()=>uni.getStorageSync('xiaoxing-language-mode'))==='system','system mode saved');
  assert(errors.length===0,'page errors: '+errors.join('; '));
  return {images:boxes.length,removedWhileLoading:true,englishNotifications:true,systemMode:true,errors};
}