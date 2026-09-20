async page => {
  const rows=Array.from({length:25},(_,i)=>({id:String(i),messageRole:i%2?'assistant':'user',messageType:'TEXT',content:'消息 '+i+'：请提醒我审核合同。\n下一行内容。',payload:{}}));
  rows.push({id:'markdown',messageRole:'assistant',messageType:'TEXT',content:'# 标题\n\n- **重点**\n\n| 项目 | 时间 |\n| --- | --- |\n| 合同 | 明天 |\n\n```js\nconst value = 1;\n```\n<img src=x onerror="window.markdownUnsafe=true">',payload:{}});
  await page.route('**/api/app/reminder/chat/messages**',r=>r.fulfill({json:{code:200,data:rows}}));
  await page.route('**/api/app/reminder/chat/sync**',r=>r.fulfill({json:{code:200,data:{revision:'history',messages:rows}}}));
  await page.goto('http://localhost:5174/#/pages/events/index');
  await page.goto('http://localhost:5174/#/pages/chat/index');
  await page.locator('.message').last().waitFor();await page.waitForTimeout(600);
  const result=await page.evaluate(()=>{
    const container=document.querySelector('#chat-scroll .uni-scroll-view');
    const root=document.querySelector('.messages');
    return {gap:container.scrollHeight-container.scrollTop-container.clientHeight,heading:!!root.querySelector('h1'),table:!!root.querySelector('table'),code:!!root.querySelector('pre'),unsafe:!!window.markdownUnsafe};
  });
  if(result.gap>80)throw Error('Re-enter did not scroll to latest: '+JSON.stringify(result));
  if(!result.heading||!result.table||!result.code||result.unsafe)throw Error('Markdown parity/sanitization failed '+JSON.stringify(result));
  return result;
}
