// Compatibility PNGs for direct image references. UiIcon renders the vector font.
import {mkdirSync,writeFileSync} from 'node:fs';
import {Resvg} from '@resvg/resvg-js';
import {lucideIcons} from '../../front/src/page/smart-reminder/lucideIcons.js';
const colors={muted:'#8e9ab0',blue:'#3267ed',purple:'#8a63d8',ink:'#243550',green:'#19ad88',red:'#ef6675',white:'#ffffff'};
const markup=nodes=>nodes.map(({tag,attrs})=>`<${tag} ${Object.entries(attrs).map(([key,value])=>`${key}="${value}"`).join(' ')} />`).join('');
const svg=(nodes,color)=>`<svg xmlns="http://www.w3.org/2000/svg" width="132" height="132" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${markup(nodes)}</svg>`;
mkdirSync('static/icons',{recursive:true});
writeFileSync('static/icons/input-orb.png', new Resvg('<svg xmlns="http://www.w3.org/2000/svg" width="144" height="144" viewBox="0 0 100 100"><defs><radialGradient id="g" cx="30%" cy="15%" r="85%"><stop stop-color="#fff"/><stop offset=".15" stop-color="#c9a9ff"/><stop offset=".36" stop-color="#9569ed"/><stop offset=".6" stop-color="#a474f9"/><stop offset=".84" stop-color="#35d9ec"/><stop offset="1" stop-color="#7dbbff"/></radialGradient></defs><circle cx="50" cy="50" r="48" fill="url(#g)"/></svg>').render().asPng());

for(const [name,nodes] of Object.entries(lucideIcons))for(const [tone,color] of Object.entries(colors))writeFileSync(`static/icons/${name}-${tone}.png`,new Resvg(svg(nodes,color)).render().asPng());
for(const [tone,color] of Object.entries({violet:'#885bdd',amber:'#d99b37'}))writeFileSync(`static/icons/clock-${tone}.png`,new Resvg(svg(lucideIcons.clock,color)).render().asPng());
await import('./build-profile-icons.mjs');
