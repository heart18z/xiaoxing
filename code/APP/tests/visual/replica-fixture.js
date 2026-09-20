async page => {
 await page.unrouteAll();
 await page.setViewportSize({width:402,height:874});
 const user={id:'9001',name:'陈通',account:'2024032702',avatar:'/avatars/user/B1.png'};
 const friends=['谢鑫','包诗琴','吴淑真','嘻嘻','李莹莹','洪建滨','陈颖','小林','小吴','小陈'].map((name,i)=>({id:String(9100+i),name,account:['2023063001','2024040101','2023062101','2023061217','2023071802'][i]||'202306300'+i,avatar:i===0?'':'/avatars/user/B'+(i+1)+'.png',permissionMode:'MUTUAL',friendRemark:''}));
 const summary='提醒我9月17日早上10点审核第一版标书';
 const event={id:'201',eventNo:'SR202609161633307718401',eventSummary:summary,eventStatus:'ACTIVE',creatorName:'陈通',recipientCount:1,activeBranchCount:1,latestFact:'已阅读提醒',createTime:'2026-09-16 16:33:30',nextEvaluateTime:'2026-09-17 18:00:00'};
 const messages=[{id:'101',messageRole:'user',messageType:'TEXT',content:'提醒我明天早上10点审核第一版标书',createTime:'2026-09-16 16:31:11',payload:{}},{id:'102',messageRole:'assistant',messageType:'TEXT',content:'好的，明天（9月17日）早上10点会提醒你审核第一版标书。',createTime:'2026-09-16 16:31:25',payload:{reasoningContent:'用户希望在明天上午10点审核第一版标书。我会先确认接收人和任务时间，再创建智能提醒。'}},{id:'103',messageRole:'assistant',messageType:'CANDIDATE_CONFIRMED',content:'',payload:{candidateId:'c1',events:[{summary:'提醒我2026-09-17早上10点审核第一版标书',timeDescription:'明天早上10点',firstEvaluateTime:'2026-09-17 09:00:00',recipients:[{name:'陈通'}]}]}},{id:'104',messageRole:'assistant',messageType:'TEXT',content:'已创建 1 个智能提醒事件。',createTime:'2026-09-16 16:33:30',payload:{}}];
 messages[1].messageType='CANDIDATE_CONFIRMED'; messages[1].payload={...messages[1].payload,...messages[2].payload}; messages.splice(2,1);
 const timeline=[['NOTIFICATION_READ','已阅读提醒','陈通','14:26:13'],['NEXT_EVALUATION_PLANNED','下次评估：2026-09-17 18:00:00；本次决策：ASK_RECIPIENT','AI','14:02:00'],['ASK_RECIPIENT','上午10点安排的第一版标书审核完成了吗？如已完成或不再需要提醒，请告诉我。','AI','14:02:00'],['NEXT_EVALUATION_PLANNED','下次评估：2026-09-17 14:00:00；本次决策：DEFER','AI','12:00:17'],['NOTIFICATION_READ','已阅读提醒','陈通','10:31:17'],['NEXT_EVALUATION_PLANNED','下次评估：2026-09-17 12:00:00；本次决策：ASK_RECIPIENT','AI','10:30:54'],['ASK_RECIPIENT','标书审核是否已经完成？','AI','10:30:54']].map((r,i)=>({id:String(i),branchId:'b1',nodeType:r[0],content:r[1],actorName:r[2],aiAction:r[2]==='AI',createTime:'2026-09-17 '+r[3]}));
 const models=['GLM-5.3-Flash','qwen3.8-flash','deepseek-v4.1-flash','glm-5.2'].map((modelAlias,i)=>({id:String(i+1),modelAlias,configType:'LLM',systemDefault:i===0}));
 const writes=[];
 await page.route('**/api/**',async route=>{
  const path=route.request().url().split('?')[0];let data={};
  if(path.endsWith('/login-page-param'))data={chekcode:'false'};
  if(path.endsWith('/bootstrap'))data={user,aiAvatar:'/avatars/assistant/A3.png',activeEventCount:3,sentActiveEventCount:2,receivedActiveEventCount:1,pendingFriendRequests:0,language:'zh-cn'};
  if(path.endsWith('/chat/messages'))data=messages;
  if(path.endsWith('/events'))data=[event,{...event,id:'202',eventNo:'SR202609161037443543170',eventSummary:'提醒陈颖每日记得归档优化',eventStatus:'STOPPED',activeBranchCount:0,latestFact:'用户在事件详情手动停止',createTime:'2026-09-16 10:37:44',nextEvaluateTime:''}].filter(e=>!route.request().url().includes('ACTIVE')||e.eventStatus==='ACTIVE');
  if(path.endsWith('/event/detail'))data={creator:true,event:{id:'201',event_no:event.eventNo,event_summary:summary,event_status:'ACTIVE',event_time:'2026-09-17 10:00:00',creatorName:'陈通',creator_user_id:'9001'},branches:[{id:'b1',recipientUserId:'9001',name:'陈通',avatar:user.avatar,latestSummary:'9月17日早上10点审核第一版标书',branchStatus:'ACTIVE',taskEventTime:'2026-09-17 10:00:00',currentFact:'收到',nextEvaluateTime:'2026-09-17 18:00:00'}],timeline};
  if(path.endsWith('/event/conversation'))data={participant:{displayName:'陈通',avatar:user.avatar},aiAvatar:'/avatars/assistant/A3.png',messages:[...messages.filter(m=>m.id!=='103'),{id:'105',messageRole:'assistant',content:'陈通为你创建了智能提醒：9月17日早上10点审核第一版标书。AI会根据事件进展在合适时间提醒你。',createTime:'2026-09-16 16:33:30'},{id:'106',messageRole:'assistant',content:'9月17日早上10点审核第一版标书。',createTime:'2026-09-17 09:02:25'},{id:'107',messageRole:'assistant',content:'现在10点了，该审核第一版标书了。',createTime:'2026-09-17 10:01:30'},{id:'108',messageRole:'user',content:'收到',createTime:'2026-09-17 10:03:43'}]};
  if(path.endsWith('/events/drawer'))data=[{eventId:'201',branchId:'b1',eventSummary:summary,summary:'9月17日早上10点审核第一版标书',eventStatus:'ACTIVE',branchStatus:'ACTIVE',recipientName:'陈通',nextEvaluateTime:'2026-09-17 18:00:00',lastRemindedAt:'2026-09-17 10:01:00'},{eventId:'203',branchId:'b3',summary:'9月17日18:00前完成工商旅游项目投标报名；9月21日15:00提交材料',eventStatus:'ACTIVE',branchStatus:'ACTIVE',recipientName:'洪建滨、陈颖',nextEvaluateTime:'2026-09-17 17:00:00'},{eventId:'204',branchId:'b4',summary:'明早8:45到厦大网络中心找庄主任',eventStatus:'ACTIVE',branchStatus:'ACTIVE',recipientName:'陈通',nextEvaluateTime:'2026-09-18 08:45:00'}];
  if(path.endsWith('/friends/list'))data=friends;
  if(path.endsWith('/friends/requests'))data={incoming:[],outgoing:[]};
  if(path.endsWith('/settings/models'))data={models,llmMode:'SYSTEM',llmConfigId:'1',speechMode:'SYSTEM',language:'zh-cn'};
  if(path.endsWith('/suggest-account'))data='2026091702';
  if(/\/(save|request|remark|clear|stop|reply|remove)$/.test(path))writes.push({path,body:route.request().postData()});
  await route.fulfill({json:{code:200,data},headers:{'access-control-allow-origin':'*'}});
 });
 await page.goto('http://localhost:5174');
 await page.evaluate(()=>sessionStorage.setItem('xiaoxing:session',JSON.stringify({token:'local-fixture',refresh:'local-fixture',userId:'9001',expiresAt:1999999999999})));
 await page.goto('http://localhost:5174/#/pages/events/index');
 await page.reload();
 await page.locator('.event-card').first().waitFor();
 await page.locator('.filter').click();
 await page.addStyleTag({content:'.frame>uni-view:nth-child(2){height:62px!important;flex-shrink:0}.dock{height:102px!important;padding-bottom:34px!important}.brand{margin-top:62px!important}.drawer>uni-view:first-child{height:34px!important;flex-shrink:0}'});
 await page.screenshot({path:'output/playwright/replica-events.png'});
}
