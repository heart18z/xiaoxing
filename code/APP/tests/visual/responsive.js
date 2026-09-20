async page=>{
 const errors=[],checks=[];page.on('pageerror',error=>errors.push(error.message));
 for(const width of [320,402,768]){
  await page.setViewportSize({width,height:874});
  for(const name of ['chat','events','event-detail','me','settings']){
   await page.goto('http://localhost:5174/#/pages/'+name+'/index'+(name==='event-detail'?'?id=201':''));
   await page.locator('.frame .header-title,.profile .title').first().waitFor();
   const result=await page.evaluate(()=>({width:innerWidth,scroll:document.documentElement.scrollWidth}));
   if(result.scroll>width+1)throw Error(name+' overflows at '+width);
   checks.push({name,width});
  }
 }
 if(errors.length)throw Error(errors.join(';'));
 return {checks,errors};
}
