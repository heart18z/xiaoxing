import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';
export const root=fileURLToPath(new URL('../',import.meta.url));
const own=createRequire(new URL('../package.json',import.meta.url));
const previous=createRequire(new URL('../../front/package.json',import.meta.url));
export function dependency(name){try{return own(name)}catch{return previous(name)}}
export async function uts(path){
  const {outputFiles}=await dependency('esbuild').build({entryPoints:[root+path],bundle:true,write:false,platform:'node',format:'esm',loader:{'.uts':'ts'}});
  JSON.parseObject=JSON.parse;
  return import('data:text/javascript;base64,'+Buffer.from(outputFiles[0].text).toString('base64'));
}

globalThis.UTSJSONObject={keys:Object.keys};
