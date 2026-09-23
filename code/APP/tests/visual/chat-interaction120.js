// Run after chat-stream-continuity.js on local fixtures. Never contacts the real API.
async page => {
 const panel=page.locator('.thinking-panel').last();
 await panel.locator('.thinking-toggle').evaluate(el=>{el.dispatchEvent(new TouchEvent('touchstart',{bubbles:true,touches:[new Touch({identifier:2,target:el,clientX:100,clientY:300})],changedTouches:[]}));el.dispatchEvent(new TouchEvent('touchend',{bubbles:true,touches:[],changedTouches:[]}));});
 await panel.locator('.thinking-toggle').click();await page.waitForTimeout(700);
 const geometry=await panel.evaluate(el=>{const r=el.getBoundingClientRect(),v=document.querySelector('#chat-scroll').getBoundingClientRect();return {top:r.top,bottom:r.bottom,viewTop:v.top,viewBottom:v.bottom};});
 if(geometry.top<geometry.viewTop-2||geometry.bottom>geometry.viewBottom-8)throw Error('Expanded thinking is obscured '+JSON.stringify(geometry));
 await page.screenshot({path:'output/playwright/mobile120-expanded.png'});
 // Scroll events must not feed scrollTop commands back into the native scroll view.
 const binding=await page.locator('#chat-scroll').evaluate(el=>el.__vueParentComponent.props.scrollTop);
 await page.locator('#chat-scroll').evaluate(el=>{el.dispatchEvent(new TouchEvent('touchstart',{bubbles:true,touches:[new Touch({identifier:3,target:el,clientX:100,clientY:300})],changedTouches:[]}));const s=Array.from(el.querySelectorAll('.uni-scroll-view')).find(n=>n.scrollHeight>n.clientHeight);s.scrollTop=100;s.dispatchEvent(new Event('scroll'));el.dispatchEvent(new TouchEvent('touchend',{bubbles:true,touches:[],changedTouches:[]}));});
 await page.waitForTimeout(600);
 const after=await page.locator('#chat-scroll').evaluate(el=>({binding:el.__vueParentComponent.props.scrollTop,position:Array.from(el.querySelectorAll('.uni-scroll-view')).find(n=>n.scrollHeight>n.clientHeight).scrollTop}));
 if(after.binding!==binding||Math.abs(after.position-100)>3)throw Error('Manual scroll fed back or jumped '+JSON.stringify({binding,after}));
 await page.locator('.latest-link').click();await page.waitForTimeout(650);
 const heights=[];
 for(const height of [300,300,0]){
  await page.locator('.compose-input').evaluate((el,height)=>{let node=el.__vueParentComponent;while(node&&!node.vnode.props?.onKeyboardheightchange)node=node.parent;if(!node)throw Error('Keyboard handler missing');node.vnode.props.onKeyboardheightchange({detail:{height}});},height);
  await page.waitForTimeout(650);
  heights.push(await page.locator('#chat-scroll').evaluate(el=>{const v=el.getBoundingClientRect(),end=document.querySelector('#latest').getBoundingClientRect();return {height:v.height,gap:end.bottom-v.bottom};}));
 }
 if(heights.some(h=>Math.abs(h.gap)>40))throw Error('Keyboard lost bottom '+JSON.stringify(heights));
 const history=Array.from({length:30},(_,i)=>({id:String(400+i),messageRole:i%2?'assistant':'user',messageType:'TEXT',content:'历史消息 '+i,reasoningContent:i%2?'历史思考内容。'.repeat(60):''}));
 await page.route('**/api/**/chat/messages**',r=>r.fulfill({json:{code:200,data:history}}));
 await page.route('**/api/**/chat/sync**',r=>r.fulfill({json:{code:200,data:{messages:history,revision:'history'}}}));
 await page.reload();await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});await page.waitForTimeout(1600);
 const old=page.locator('.thinking-panel').first();
 await old.locator('.thinking-toggle').evaluate(el=>{el.dispatchEvent(new TouchEvent('touchstart',{bubbles:true,touches:[new Touch({identifier:4,target:el,clientX:100,clientY:300})],changedTouches:[]}));el.dispatchEvent(new TouchEvent('touchend',{bubbles:true,touches:[],changedTouches:[]}));});
 await old.locator('.thinking-toggle').click();await page.waitForTimeout(700);
 const historical=await old.evaluate(el=>{const r=el.getBoundingClientRect(),v=document.querySelector('#chat-scroll').getBoundingClientRect(),end=document.querySelector('#latest').getBoundingClientRect();return {top:r.top,bottom:r.bottom,viewTop:v.top,viewBottom:v.bottom,distanceFromLatest:end.bottom-v.bottom};});
 if(historical.top<historical.viewTop-2||historical.bottom>historical.viewBottom-8||historical.distanceFromLatest<500)throw Error('Historical expansion jumped to latest or stayed hidden '+JSON.stringify(historical));
 return {expandedPanel:geometry,manualScroll:after,keyboard:heights,historical};
}
