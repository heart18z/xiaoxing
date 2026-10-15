// Run after social131-fixture.js in an isolated local preview session on port 5175.
async page => {
 const assert=(value,label)=>{if(!value)throw Error(label)};
 await page.locator('.dock').getByText('我的',{exact:true}).click();
 await page.addStyleTag({content:'.top-tip{top:62px!important}'});
 const show=async(text,tone)=>page.evaluate(async({text,tone})=>{
   const tips=await import('/store/tip.uts?import');tips.showTip(text,tone);
 },{text,tone});
 for(const [tone,text] of [['success','备注已保存'],['info','正在为你同步最新提醒'],['error','保存失败，请稍后重试']]){
   await show(text,tone);await page.getByText(text,{exact:true}).waitFor();await page.waitForTimeout(280);
   assert(await page.locator('.tip-halo').getAttribute('class').then(c=>c.includes('halo-'+tone)),'status icon tone '+tone);
   assert(await page.locator('.tip-text').evaluate(el=>getComputedStyle(el).color)==='rgb(25, 44, 77)','readable dark text on light card');
   await page.screenshot({path:'output/playwright/tip136-'+tone+'.png'});
   await page.getByRole('button',{name:'关闭提示',exact:true}).click();
   await page.locator('.top-tip').waitFor({state:'detached'});
 }
 await page.setViewportSize({width:320,height:740});
 await show('下载链接暂时不可用，请稍后重新尝试或联系管理员检查下载配置。','error');
 await page.waitForTimeout(300);
 const box=await page.locator('.top-tip').boundingBox();
 assert(box.x>=0&&box.x+box.width<=320,'long feedback fits narrow screen');
 assert(await page.locator('.tip-close').isVisible(),'close control remains visible with long text');
 await page.screenshot({path:'output/playwright/tip136-narrow.png'});
 await page.locator('.top-tip').waitFor({state:'detached',timeout:7000});
 await page.setViewportSize({width:402,height:874});
 return 'PASS success/info/error visual states, close action, narrow text and automatic dismissal';
}
