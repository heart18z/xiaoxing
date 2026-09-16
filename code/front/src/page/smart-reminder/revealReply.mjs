// Only animate committed replies: never imply a reminder was saved before commit.
// Bound the animation so long answers cannot keep the composer locked for seconds.
export function revealReply(text,onText,{signal,instant=false}={}){
  const characters=Array.from(String(text||''));
  if(signal?.aborted)return Promise.resolve();
  if(instant||characters.length<2){onText(characters.join(''));return Promise.resolve();}
  return new Promise(resolve=>{
    let timer,index=0;
    const step=Math.max(1,Math.ceil(characters.length/40));
    const stop=()=>{clearTimeout(timer);signal?.removeEventListener('abort',stop);resolve();};
    const tick=()=>{if(signal?.aborted){stop();return;}index=Math.min(characters.length,index+step);onText(characters.slice(0,index).join(''));if(index===characters.length)stop();else timer=setTimeout(tick,16);};
    signal?.addEventListener('abort',stop,{once:true});tick();
  });
}
