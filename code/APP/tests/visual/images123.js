async page => {
 await page.unrouteAll();
 await page.setViewportSize({width:402,height:874});
 const image=await page.evaluate(()=>{const c=document.createElement('canvas');c.width=600;c.height=400;const x=c.getContext('2d');x.fillStyle='#fff';x.fillRect(0,0,600,400);x.fillStyle='#142b57';x.font='28px sans-serif';x.fillText('9月24日14:00 去客户现场',20,100);return c.toDataURL('image/jpeg');});
 const requests=[];let failOnce=true;
 await page.route('**/api/**',async route=>{
  const url=route.request().url(),path=url.split('?')[0];let data={};
  if(path.endsWith('/bootstrap'))data={user:{id:'9001',name:'测试',account:'image-test',avatar:'/avatars/user/B1.png'},aiAvatar:'/avatars/assistant/A3.png',activeEventCount:0,sentActiveEventCount:0,receivedActiveEventCount:0,pendingFriendRequests:0,language:'zh-cn'};
  if(path.endsWith('/chat/messages'))data=[{id:'1001',messageRole:'user',messageType:'TEXT',content:'把图片中的工作安排给陈一',payload:{fileNames:['work.jpg','retry.png'],fileIds:['11','12']},createTime:'2026-09-23 15:00:00'}];
  if(path.endsWith('/events')||path.endsWith('/friends/list'))data=[];
  if(path.endsWith('/files/preview')){requests.push(url);if(url.includes('id=12')&&failOnce){failOnce=false;await route.fulfill({status:200,json:{code:400,msg:'测试失败'}});return;}data={image};}
  await route.fulfill({json:{code:200,data},headers:{'access-control-allow-origin':'*'}});
 });
 await page.goto('http://localhost:5174');
 await page.evaluate(()=>sessionStorage.setItem('xiaoxing:session',JSON.stringify({token:'fixture',refresh:'fixture',userId:'9001',expiresAt:1999999999999})));
 await page.goto('http://localhost:5174/#/pages/chat/index');await page.reload();
 await page.locator('.attachment-image .thumbnail').first().waitFor();
 await page.getByText('图片加载失败，点击重试',{exact:true}).click();
 await page.waitForFunction(()=>document.querySelectorAll('.attachment-image .thumbnail').length===2);
 const boxes=await page.locator('.attachment-image').evaluateAll(nodes=>nodes.map(n=>({width:n.getBoundingClientRect().width,height:n.getBoundingClientRect().height})));
 if(boxes.some(b=>b.width!==210||b.height!==156))throw Error('Unstable thumbnail dimensions '+JSON.stringify(boxes));
 await page.screenshot({path:'output/playwright/mobile123-images.png'});
 await page.locator('.attachment-image').first().click();
 await page.waitForTimeout(800);
 if(!requests.some(u=>u.includes('full=true')))throw Error('No full image preview request');
 await page.screenshot({path:'output/playwright/mobile123-image-preview.png'});
 return {thumbnails:boxes.length,previewRequests:requests.length,fullPreview:true};
}
