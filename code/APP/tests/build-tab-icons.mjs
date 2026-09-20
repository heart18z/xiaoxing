import {readFileSync,writeFileSync} from 'node:fs';
import {Resvg} from '@resvg/resvg-js';
const d=readFileSync('../front/src/page/smart-reminder/UiIcon.vue','utf8').match(/people:'([^']+)'/)[1];
for(const [tone,color] of Object.entries({active:'#5552ed',muted:'#9695b5'})){
 const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="${d}"/></svg>`;
 writeFileSync(`static/icons/tab-people-${tone}.svg`,svg);writeFileSync(`static/icons/tab-people-${tone}.png`,new Resvg(svg).render().asPng());
}
