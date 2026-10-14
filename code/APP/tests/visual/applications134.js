async page => {
 const assert=(v,m)=>{if(!v)throw Error(m)};
 const rows=[
  {id:'501',userId:'9100',name:'涞涞',avatar:'/avatars/user/B3.png',direction:'incoming',requestStatus:'PENDING',requestType:'FRIEND',permissionMode:'MUTUAL'},
  {id:'502',userId:'9101',name:'陈通',avatar:'/avatars/user/B4.png',direction:'outgoing',requestStatus:'PENDING',requestType:'FRIEND',permissionMode:'MUTUAL'},
  {id:'503',userId:'9102',name:'小王',avatar:'/avatars/user/B6.png',direction:'outgoing',requestStatus:'ACCEPTED',requestType:'FRIEND',friendActive:true},
  {id:'504',userId:'9103',name:'小李',avatar:'/avatars/user/B2.png',direction:'incoming',requestStatus:'ACCEPTED',requestType:'FRIEND',friendActive:true},
 ];
 const writes=[];
 await page.route('**/api/**',async route=>{
  const path=route.request().url().split('?')[0];let data;
  if(path.endsWith('/friends/requests')){
   const priority=r=>r.requestStatus==='PENDING'?(r.direction==='incoming'?0:1):2;
   data={incoming:rows.filter(r=>r.direction==='incoming'&&r.requestStatus==='PENDING'),outgoing:rows.filter(r=>r.direction==='outgoing'&&r.requestStatus==='PENDING'),items:[...rows].sort((a,b)=>priority(a)-priority(b))};
  }else if(path.endsWith('/friends/reply')){
   const request=route.request().postDataJSON();writes.push(request);
   const row=rows.find(r=>r.id===request.requestId);assert(row.direction==='incoming'&&row.requestStatus==='PENDING','only pending incoming requests actionable');
   row.requestStatus=request.accept?'ACCEPTED':'REJECTED';row.friendActive=request.accept;data=true;
  }else if(path.includes('/avatars/'))throw Error('cancelled avatar library API must not be called');
  else return route.fallback();
  await route.fulfill({json:{code:200,data}});
 });
 await page.locator('.dock').getByText('我的',{exact:true}).click();
 await page.getByText('好友申请 1',{exact:true}).click();
 await page.locator('.application-row').first().waitFor();
 assert(await page.locator('.application-row').count()===4,'incoming/outgoing/accepted share one list');
 assert(await page.locator('.application-status').getByText('已添加',{exact:true}).count()===2,'both accepted directions display added');
 assert(await page.getByText('等待对方通过',{exact:true}).count()===1,'outgoing pending display');
 await page.evaluate(async()=>Promise.all(Array.from(document.images).map(img=>img.decode().catch(()=>{}))));
 await page.waitForTimeout(250);await page.screenshot({path:'output/playwright/applications134-requests.png'});
 await page.setViewportSize({width:320,height:740});
 assert(await page.evaluate(()=>document.documentElement.scrollWidth<=320),'no page overflow at 320');
 assert(await page.locator('.application-row').first().evaluate(el=>el.scrollWidth<=el.clientWidth+2),'action buttons and person fit narrow row');
 await page.screenshot({path:'output/playwright/applications134-320.png'});
 await page.setViewportSize({width:402,height:874});
 await page.locator('.application-row').filter({hasText:'涞涞'}).getByText('接受',{exact:true}).click();
 await page.waitForFunction(()=>document.querySelectorAll('.application-accept').length===0);
 assert(writes[0].requestId==='501'&&writes[0].accept===true,'accept targets exact incoming id');
 assert((await page.locator('.application-row').filter({hasText:'涞涞'}).innerText()).includes('已添加'),'accepted request retained in history');
 assert((await page.locator('.segment-item.selected').innerText()).trim()==='好友申请','handled request decrements pending badge');
 rows.push({id:'505',name:'权限请求',avatar:'/avatars/user/B5.png',direction:'incoming',requestStatus:'PENDING',requestType:'PERMISSION',permissionMode:'I_CAN_REMIND'});
 await page.locator('.dock').getByText('AI助手',{exact:true}).click();await page.locator('.dock').getByText('我的',{exact:true}).click();
 await page.getByText('好友申请 1',{exact:true}).click();
 const permission=page.locator('.application-row').filter({hasText:'权限请求'});
 await permission.getByText('对方可提醒我',{exact:true}).waitFor();
 await permission.getByText('拒绝',{exact:true}).click();
 await page.waitForFunction(()=>document.querySelectorAll('.application-reject').length===0);
 assert(writes[1].requestId==='505'&&writes[1].accept===false,'reject preserves correct id and permission direction');
 assert((await permission.innerText()).includes('已拒绝'),'declined request retained');
 await page.locator('.profile-picture').click();await page.getByText('设置我的头像',{exact:true}).waitFor();
 assert(await page.locator('.remove-avatar').count()===0&&!(await page.locator('.popup-body').innerText()).includes('我上传的头像'),'avatar enhancement reverted');
 await page.locator('.popup-head .close').click();await page.locator('.mask').waitFor({state:'detached'});
 console.log('PASS unified history, states, exact accept/reject, pending count, permissions, narrow layout, avatar rollback');
}
