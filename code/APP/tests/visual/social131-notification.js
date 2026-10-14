async page => {
 await page.locator('.dock').getByText('AI助手',{exact:true}).click();
 await page.getByText('查看好友申请 ›',{exact:true}).click();
 await page.waitForFunction(()=>Array.from(document.querySelectorAll('.segment-item.selected')).some(e=>e.textContent.includes('好友申请')));
 await page.getByText('我的好友 10',{exact:true}).click();
 const result=await page.evaluate(async()=>{
  const url=performance.getEntriesByType('resource').map(e=>e.name).find(n=>n.includes('/store/navigation.uts'));
  if(!url)throw Error('navigation module not loaded');
  const navigation=await import(url);navigation.openFriendRequests();
  return true;
 });
 await page.waitForFunction(()=>Array.from(document.querySelectorAll('.segment-item.selected')).some(e=>e.textContent.includes('好友申请')));
 console.log('PASS friend notification destination from chat and while already on profile');
}
