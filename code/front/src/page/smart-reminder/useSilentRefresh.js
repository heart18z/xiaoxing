import { onMounted, onActivated, onDeactivated, onBeforeUnmount } from 'vue';

// No overlapping polls; inactive/hidden pages never poll. Failures preserve the last good UI.
export function useSilentRefresh(task, interval=10000) {
  let active=false, timer, running=false, disposed=false, revision=0;
  const stopTimer=()=>{clearTimeout(timer);timer=undefined;};
  const run=async()=>{
    stopTimer();
    if (!active || disposed || document.hidden || running) return;
    running=true; const ticket=revision;
    try { await task(()=>!disposed && active && !document.hidden && ticket===revision); } catch { /* Silent retry on the next tick. */ }
    finally { running=false; schedule(); }
  };
  const schedule=()=>{stopTimer();if(active&&!disposed&&!document.hidden)timer=setTimeout(run,interval);};
  let activatedOnce=false;
  const resume=()=>{active=true;schedule();};
  const pause=()=>{active=false;revision++;stopTimer();};
  const visibility=()=>{revision++;if(document.hidden)stopTimer();else void run();};
  onMounted(()=>{resume();document.addEventListener('visibilitychange',visibility);window.addEventListener('online',run);window.addEventListener('smart-reminder:push-refresh',run);});
  onActivated(()=>{resume();if(activatedOnce)void run();activatedOnce=true;});onDeactivated(pause);
  onBeforeUnmount(()=>{disposed=true;pause();document.removeEventListener('visibilitychange',visibility);window.removeEventListener('online',run);window.removeEventListener('smart-reminder:push-refresh',run);});
}
