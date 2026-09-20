import fs from 'node:fs/promises';
import path from 'node:path';
import prettier from 'prettier';
const folders=['core','api','services','store','config','components','pages'];
async function walk(dir){const result=[];for(const e of await fs.readdir(dir,{withFileTypes:true})){const p=path.join(dir,e.name);if(e.isDirectory())result.push(...await walk(p));else if(/\.(uts|uvue)$/.test(p))result.push(p)}return result}
const files=['App.uvue','main.uts',...(await Promise.all(folders.map(walk))).flat()];
for(const file of files){let text=await fs.readFile(file,'utf8');const vue=file.endsWith('.uvue');if(vue)text=text.replace('lang="uts"','lang="ts"');text=await prettier.format(text,{parser:vue?'vue':'typescript',singleQuote:true,semi:true,htmlWhitespaceSensitivity:"ignore",printWidth:100});if(vue)text=text.replace('lang="ts"','lang="uts"');await fs.writeFile(file,text)}
