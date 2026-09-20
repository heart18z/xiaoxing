import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { Resvg } from '@resvg/resvg-js';
const source = readFileSync('../front/src/page/smart-reminder/UiIcon.vue', 'utf8');
const paths = Object.fromEntries([...source.matchAll(/(\w+):'([^']+)'/g)].map(m => [m[1], m[2]]));
Object.assign(paths, {
  scan:'M8 3H3v5M16 3h5v5M3 16v5h5M21 16v5h-5M6 12h12',
  qr:'M3 3h6v6H3ZM15 3h6v6h-6ZM3 15h6v6H3ZM15 15h2v2h-2ZM21 13v4M13 21h4M21 20v1',
  settings:'M9 3h6l1 3 3 1 2 5-2 5-3 1-1 3H9l-1-3-3-1-2-5 2-5 3-1ZM16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0',

  user:'M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M4 22v-2a8 8 0 0 1 16 0v2',
  lock:'M5 10h14v12H5ZM8 10V6a4 4 0 0 1 8 0v4M12 15v3',
  eye:'M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12ZM15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
  eyeOff:'M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12ZM15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0M3 3l18 18',
  camera:'M3 7h4l2-3h6l2 3h4v14H3ZM16 13a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  close:'M6 6l12 12M6 18L18 6',
  down:'m5 9 7 7 7-7',
  spark:'m10 2 2.5 6.5L19 11l-6.5 2.5L10 20l-2.5-6.5L1 11l6.5-2.5ZM19 15l1.2 3.2L23 19l-2.8 1.2L19 23l-1.2-2.8L15 19l2.8-.8Z',
});
mkdirSync('static/icons', {recursive:true});
writeFileSync('static/icons/input-orb.png', new Resvg('<svg xmlns="http://www.w3.org/2000/svg" width="144" height="144" viewBox="0 0 100 100"><defs><radialGradient id="g" cx="30%" cy="15%" r="85%"><stop stop-color="#fff"/><stop offset=".15" stop-color="#c9a9ff"/><stop offset=".36" stop-color="#9569ed"/><stop offset=".6" stop-color="#a474f9"/><stop offset=".84" stop-color="#35d9ec"/><stop offset="1" stop-color="#7dbbff"/></radialGradient></defs><circle cx="50" cy="50" r="48" fill="url(#g)"/></svg>').render().asPng());
for (const [name,d] of Object.entries(paths)) {
  for (const [tone,color] of Object.entries({muted:'#959aaf',blue:'#4262f2',purple:'#8b80b4',ink:'#555e68',green:'#36ab90',red:'#f2525b',white:'#ffffff'})) {
    const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="${d}"/></svg>`;
    writeFileSync(`static/icons/${name}-${tone}.png`,new Resvg(svg).render().asPng());
  }
}

// Drawer group colors share the original clock vector without recoloring other UI icons.
for (const [tone,color] of Object.entries({violet:'#8655e9',amber:'#e79a29'})) {
  const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="${paths.clock}"/></svg>`;
  writeFileSync(`static/icons/clock-${tone}.png`,new Resvg(svg).render().asPng());
}

// Standard profile icons override the legacy hand-drawn paths.
await import("./build-profile-icons.mjs");
