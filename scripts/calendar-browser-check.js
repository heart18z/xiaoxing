// Run after calendar-chat-browser.js, opening the drawer and date dialog.
async page=>{
  const dialog=page.getByRole('dialog',{name:'选择提醒日期'});
  const trigger=page.locator('.drawer-date-trigger'),original=await trigger.innerText();
  const confirm=dialog.getByRole('button',{name:'确认筛选',exact:true});
  const box=await confirm.boundingBox();
  if(!box||box.y<0||box.y+box.height>page.viewportSize().height)throw Error('Confirm button hidden on small screens');
  if(await dialog.evaluate(el=>el.scrollWidth>el.clientWidth))throw Error('Calendar overflows');
  await dialog.getByRole('button',{name:'明天',exact:true}).click();
  await dialog.getByRole('button',{name:'取消',exact:true}).click();
  if(await trigger.innerText()!==original)throw Error('Cancel changed active range');
  await trigger.click();await dialog.getByRole('button',{name:'明天',exact:true}).click();await confirm.click();
  await page.getByText('明日提交合同',{exact:true}).waitFor();
  if(await page.getByText('今日整理会议资料',{exact:true}).count())throw Error('Date filter did not apply');
  await page.getByRole('button',{name:'还原',exact:true}).click();await page.getByText('今日整理会议资料',{exact:true}).waitFor();
  return{cancelPreservesRange:true,tomorrowFilters:true,resetRestores:true,confirmVisibleOnSmallScreen:true};
}
