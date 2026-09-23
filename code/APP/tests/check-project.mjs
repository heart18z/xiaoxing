import fs from 'node:fs';
import path from 'node:path';
import {dependency,root} from './loader.mjs';
const {parse,compileScript,compileTemplate}=dependency('@vue/compiler-sfc');
const {transform}=dependency('esbuild');
const failures=[];let pages=0,modules=0;
function walk(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>['node_modules','unpackage','.hbuilderx'].includes(e.name)?[]:e.isDirectory()?walk(path.join(dir,e.name)):[path.join(dir,e.name)])}
for(const file of walk(root)){
  const relative=path.relative(root,file).replaceAll('\\','/');
  if(file.endsWith('.json')){try{JSON.parse(fs.readFileSync(file,'utf8'))}catch(e){failures.push(relative+': '+e.message)}}
  if(file.endsWith('.uvue')){
    pages++;const source=fs.readFileSync(file,'utf8'),{descriptor,errors}=parse(source,{filename:file});
    failures.push(...errors.map(e=>relative+': '+e));
    try{
      if(descriptor.scriptSetup)descriptor.scriptSetup.lang='ts';
      if(descriptor.script)descriptor.script.lang='ts';
      const script=compileScript(descriptor,{id:relative,fs:{fileExists:fs.existsSync,readFile:f=>fs.readFileSync(f,'utf8')}});
      await transform(script.content,{loader:'ts'});
      if(descriptor.template){const result=compileTemplate({id:relative,filename:file,source:descriptor.template.content,compilerOptions:{isTS:true,bindingMetadata:script.bindings,expressionPlugins:['typescript']}});failures.push(...result.errors.map(e=>relative+': '+e));}
    }catch(e){failures.push(relative+': '+e.message)}
    if(/<(?:div|span|button-foo|el-\w+|web-view)\b/.test(source))failures.push(relative+': web-only component');
  }
  // UTS permits this export-function annotation, while TypeScript/esbuild does not.
  // The HBuilderX build and check-ios-generated.mjs validate its native semantics.
  if(file.endsWith('.uts')){modules++;try{await transform(fs.readFileSync(file,'utf8').replace(/^\s*@UTSJS\.keepAlive\s*$/gm,''),{loader:'ts'})}catch(e){failures.push(relative+': '+e.message)}}
}
for(const page of JSON.parse(fs.readFileSync(root+'pages.json','utf8')).pages)if(!fs.existsSync(root+page.path+'.uvue'))failures.push('Missing page '+page.path);
if(failures.length){console.error(failures.join('\n'));process.exitCode=1}else console.log(`PASS: ${pages} uvue components/pages, ${modules} UTS modules, JSON and routes. This checks Vue/TypeScript syntax, not the HBuilderX native compiler.`);
