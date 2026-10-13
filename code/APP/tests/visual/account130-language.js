async page => {
 const requests=[];
 await page.route('**/api/app/reminder/settings/models',route=>route.fulfill({json:{code:200,data:{models:[{id:'1',modelAlias:'Enabled',configType:'LLM'}],llmMode:'SYSTEM',llmConfigId:'999',speechMode:'SYSTEM',language:'zh-cn'}}}));
 await page.route('**/api/app/reminder/settings/language',async route=>{requests.push(route.request().postDataJSON());await route.fulfill({json:{code:200,data:true}})});
 await page.goto('http://localhost:5174/#/pages/settings/index');await page.reload();await page.locator('#xiaoxing-startup').waitFor({state:'hidden'});
 await page.locator('.select-field').first().click();await page.getByText('English',{exact:true}).click();await page.getByText('Settings',{exact:true}).waitFor();
 if(requests.length!==1 || JSON.stringify(requests[0])!==JSON.stringify({language:'en-us'}))throw Error(JSON.stringify(requests));
 await page.screenshot({path:'output/playwright/account130-language.png'});
 return {requests,translated:true};
}