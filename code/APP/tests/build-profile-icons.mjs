import {readFileSync,writeFileSync,copyFileSync} from 'node:fs';
import {Resvg} from '@resvg/resvg-js';
const colors={muted:'#959aaf',blue:'#4262f2',purple:'#8b80b4',ink:'#555e68',green:'#36ab90',red:'#f2525b',white:'#ffffff'};
for(const [name,source] of Object.entries({settings:'settings',scan:'scan-line',qr:'qr-code',close:'x',tipInfo:'info',tipSuccess:'circle-check',tipError:'circle-alert'})){
 const original=readFileSync(`node_modules/lucide-static/icons/${source}.svg`,'utf8');
 for(const [tone,color] of Object.entries(colors)){
  const svg=original.replace('stroke="currentColor"',`stroke="${color}"`).replace('stroke-width="2"','stroke-width="1.8"');
  writeFileSync(`static/icons/${name}-${tone}.svg`,svg);
  writeFileSync(`static/icons/${name}-${tone}.png`,new Resvg(svg,{fitTo:{mode:'width',value:132}}).render().asPng());
 }
}
copyFileSync('node_modules/lucide-static/LICENSE','static/icons/LUCIDE-LICENSE.txt');
