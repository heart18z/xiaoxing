<template>
  <div v-if="visible" class="voice-inline" aria-live="polite">
    <template v-if="status==='recording'">
      <div class="voice-line" aria-hidden="true"><i v-for="(level,n) in waveLevels" :key="n" :style="{height:(3+level*29)+'px',opacity:.35+level*.65}"></i></div>
      <div class="voice-status"><span>正在识别… <time>{{ clockText }}</time></span><button type="button" class="voice-confirm" aria-label="确认并转文字" @click="finish">确认</button><button type="button" class="voice-cancel" @click="cancel">取消</button></div>
    </template>
    <div v-else-if="status==='transcribing'" class="voice-processing" role="status"><span class="transcribe-spinner" aria-hidden="true"></span><div><b>录音已结束，正在转文字</b><small>{{ elapsed>8?'识别服务响应较慢，可取消后重试':'麦克风已关闭，无需继续说话' }}</small></div></div>
    <div v-else class="voice-status"><span>{{ message }}</span></div>
    <p v-if="error" class="voice-error" role="alert">{{ error }}</p>
    <div class="voice-footer"><small>{{ status==='recording'? (native?'文字实时填入草稿，确认后停止采音':'说完点确认后转文字，不会自动发送') : '识别后只填入草稿，不会自动发送' }}</small><button v-if="status!=='recording'" type="button" class="voice-cancel" @click="cancel">取消</button></div>
  </div>
  <button type="button" class="voice-button" :disabled="disabled||(busy&&status!=='recording')" :aria-label="status==='recording'?'结束录音':'语音输入'" :aria-pressed="status==='recording'" @click="status==='recording'?finish():start()"><UiIcon name="microphone" /></button>
</template>
<script setup>
import { computed, onBeforeUnmount, onDeactivated, ref, watch } from 'vue';
import { Capacitor, registerPlugin } from '@capacitor/core';
import { ElMessage } from 'element-plus';
import Recorder from 'recorder-core';
import 'recorder-core/src/engine/wav.js';
import UiIcon from './UiIcon.vue';
import { transcribeAudio } from '@/api/smartReminder';
const props=defineProps({disabled:Boolean});const emit=defineEmits(['begin','partial','cancel','busy']);
const speech=registerPlugin('XiaoxingSpeech'),native=Capacitor.getPlatform()==='ios';
const visible=ref(false),status=ref('idle'),error=ref(''),inputLevel=ref(0),waveLevels=ref(Array(25).fill(0)),elapsed=ref(0);
const clockText=computed(()=>`${Math.floor(elapsed.value/60)}:${String(elapsed.value%60).padStart(2,'0')}`);
const busy=computed(()=>['opening','recording','transcribing'].includes(status.value));
watch(busy,value=>emit('busy',value),{flush:'sync'});
const message=computed(()=>({opening:'请允许麦克风和语音权限…',recording:native?'正在聆听，文字实时填入…':'正在录音，完成后转文字…',transcribing:'正在整理识别结果…',error:'识别未完成，请重试',idle:'点击麦克风开始语音输入'}[status.value]));
let generation=0,recorder,controller,timer,captureTimer,audioContext,captureStream,listeners=[],nativeSession,pcmPeak=0,uiTimer,phaseStarted=0,lastLevelAt=0;
const setLevel=value=>{if(status.value!=='recording')return;inputLevel.value=Math.max(0,Math.min(100,Number(value)||0));lastLevelAt=Date.now();};
const enterPhase=value=>{status.value=value;phaseStarted=Date.now();elapsed.value=0;clearInterval(uiTimer);uiTimer=setInterval(()=>{elapsed.value=Math.floor((Date.now()-phaseStarted)/1000);if(status.value==='recording'){const level=Date.now()-lastLevelAt<220?inputLevel.value/100:0;waveLevels.value=[...waveLevels.value.slice(1),level];}},80);};
const removeListeners=()=>{for(const listener of listeners)void listener.remove();listeners=[];};
const stopTracks=()=>{captureStream?.getTracks().forEach(track=>track.stop());captureStream=undefined;};
const releaseRecorder=()=>{clearTimeout(captureTimer);stopTracks();const rec=recorder;recorder=undefined;rec?.close();const ctx=audioContext;audioContext=undefined;if(ctx)Recorder.CloseNewCtx(ctx);};
const cleanup=()=>{
  generation++;clearTimeout(timer);clearInterval(uiTimer);controller?.abort();controller=undefined;releaseRecorder();
  const id=nativeSession;nativeSession=undefined;if(id)void speech.stop({session:id,cancel:true}).catch(()=>{});
  removeListeners();status.value='idle';visible.value=false;emit('busy',false);
};
const cancel=()=>{cleanup();emit('cancel');};
const fail=(run,value)=>{if(run!==generation)return;cleanup();ElMessage.error(value);};
const finishNative=event=>{
  if(event.session!==nativeSession)return;
  if(!event.cancelled&&event.text)emit('partial',event.text);
  nativeSession=undefined;clearTimeout(timer);clearInterval(uiTimer);removeListeners();
  if(event.error){fail(generation,event.error);}else{status.value='idle';visible.value=false;}
};
const start=async()=>{
  if(props.disabled||busy.value)return;
  cleanup();visible.value=true;status.value='opening';error.value='';inputLevel.value=0;waveLevels.value=Array(25).fill(0);pcmPeak=0;emit('begin');const run=++generation;
  try{
    if(native){
      const session=String(Date.now())+'-'+run;nativeSession=session;
      for(const [name,callback] of [['level',e=>{if(e.session===nativeSession&&run===generation)setLevel(e.level);}],['result',e=>{if(e.session===nativeSession&&run===generation)emit('partial',e.text);}],['finished',e=>{if(run===generation)finishNative(e);}]]){
        const listener=await speech.addListener(name,callback);if(run!==generation){await listener.remove();return;}listeners.push(listener);
      }
      await speech.start({session});if(run!==generation)return;
      if(nativeSession){enterPhase('recording');timer=setTimeout(finish,60000);}return;
    }
    if(!window.isSecureContext||!navigator.mediaDevices?.getUserMedia)throw new Error('请使用HTTPS访问以使用麦克风');
    // Create/resume within this click, before any await: iOS may otherwise leave the context suspended.
    audioContext=Recorder.GetContext(true);if(!audioContext)throw new Error('浏览器不支持录音，请使用新版小醒或更换浏览器');
    if(audioContext.state==='suspended')void audioContext.resume().catch(()=>{});
    const stream=await navigator.mediaDevices.getUserMedia({audio:{echoCancellation:true,noiseSuppression:true},video:false});
    if(run!==generation){stream.getTracks().forEach(track=>track.stop());return;}captureStream=stream;
    let receivedSamples=false;
    const rec=Recorder({type:'wav',sampleRate:16000,bitRate:16,sourceStream:stream,runningContext:audioContext,onProcess:(buffers,power,duration,rate,first)=>{
      if(run!==generation||status.value!=='recording')return;receivedSamples=true;setLevel(power);
      for(let i=first;i<buffers.length;i++)for(const sample of buffers[i])pcmPeak=Math.max(pcmPeak,Math.abs(sample));
    }});recorder=rec;
    rec.open(()=>{if(run!==generation){rec.close();return;}try{rec.start();enterPhase('recording');timer=setTimeout(finish,90000);captureTimer=setTimeout(()=>{if(!receivedSamples)fail(run,'没有采集到麦克风音频，请检查浏览器麦克风权限或输入设备后重试');},5000);}catch{fail(run,'无法开始录音，请检查权限后重试');}},()=>fail(run,'无法使用麦克风，请检查权限后重试'));
  }catch(e){
    if(run!==generation)return;const id=nativeSession;nativeSession=undefined;if(id)void speech.stop({session:id,cancel:true}).catch(()=>{});
    removeListeners();fail(run,e?.message||'语音服务不可用，请更新App并检查权限');
  }
};
const finish=async()=>{
  if(status.value!=='recording')return;enterPhase('transcribing');clearTimeout(timer);clearTimeout(captureTimer);const run=generation;
  if(nativeSession){try{await speech.stop({session:nativeSession,cancel:false});}catch{fail(run,'无法结束识别，请重试');}return;}
  // Freeze PCM and stop the owned microphone tracks before encoding or waiting on the network.
  recorder?.pause();stopTracks();
  recorder?.stop((blob,duration)=>{if(run!==generation)return;releaseRecorder();if(duration<500){fail(run,'录音太短，请至少说半秒钟');return;}if(pcmPeak<=1){fail(run,'录音中未采集到声音，请检查麦克风是否静音或选错输入设备');return;}void recognize(new File([blob],'recording.wav',{type:'audio/wav'}),run);},()=>fail(run,'录音失败，请重新录音'));
};
const recognize=async(file,run)=>{
  if(file.size===0||file.size>20*1024*1024||!/\.(wav|mp3|m4a|mp4|webm|ogg|flac|aac)$/i.test(file.name)){fail(run,'请选择20MB以内的有效音频文件');return;}
  if(status.value!=='transcribing')enterPhase('transcribing');error.value='';controller=new AbortController();
  try{const res=await transcribeAudio(file,controller.signal);if(run!==generation)return;const text=res.data.data?.text?.trim();if(!text)throw new Error('未识别到语音，请重试');emit('partial',text);clearInterval(uiTimer);status.value='idle';visible.value=false;}
  catch(e){fail(run,e?.response?.data?.msg||e?.message||'识别失败，请检查语音模型配置后重试');}
};
const hide=()=>{if(document.hidden)cleanup();};document.addEventListener('visibilitychange',hide);
onDeactivated(cleanup);onBeforeUnmount(()=>{cleanup();document.removeEventListener('visibilitychange',hide);});
</script>
<style scoped>
.voice-inline{flex:0 0 100%;order:-1;min-width:0;padding:12px 14px 9px;margin:2px 0 6px;border:1px solid #e9edff;border-radius:18px;background:linear-gradient(130deg,#f8faff,#f1f5ff);color:#727e99}.voice-recording-head{display:flex;align-items:center;gap:7px;color:#485678;font-size:13px}.voice-recording-head b{font-weight:600}.voice-recording-head time{margin-left:auto;font-variant-numeric:tabular-nums;color:#8c96af;font-size:12px}.recording-dot{width:7px;height:7px;border-radius:50%;background:#ef7485;box-shadow:0 0 0 3px #fce9ed}.voice-line{display:flex;align-items:center;justify-content:center;gap:4px;height:42px;margin:3px 0;overflow:hidden}.voice-line i{width:3px;flex:0 0 3px;border-radius:4px;background:linear-gradient(#697cff,#a3a5fa);transition:height 80ms linear,opacity 80ms linear}.voice-status{display:flex;gap:10px;align-items:center;font-size:12px}.voice-status span{flex:1;min-width:0}.voice-status .voice-confirm{display:flex;align-items:center;gap:4px;border:0;background:#536aff;color:white;border-radius:11px;padding:9px 12px;font-size:13px;white-space:nowrap}.voice-confirm .ui-icon{width:17px;height:17px}.voice-inline small{display:block;font-size:11px;line-height:1.6;overflow-wrap:anywhere}.voice-footer{display:flex;align-items:center;gap:8px;margin-top:6px}.voice-footer small{flex:1;min-width:0;color:#8f9ab0}.voice-cancel{border:0;background:transparent;color:#8894ad;padding:5px 0 5px 10px;font-size:12px}.voice-processing{display:flex;gap:12px;align-items:center;padding:9px 0}.voice-processing b{font-size:13px;font-weight:600;color:#4d5e80}.voice-processing small{margin-top:5px;color:#8c97ad}.transcribe-spinner{flex:0 0 24px;width:24px;height:24px;border:2px solid #e0e7ff;border-top-color:#657aff;border-radius:50%;animation:transcribe-spin .8s linear infinite}.voice-error{color:#ac4b4b;font-size:12px;line-height:1.5;margin:5px 0;overflow-wrap:anywhere}.audio-file{display:block;color:#5266d7;font-size:12px;margin-top:5px}.audio-file input{display:block;width:100%;font-size:12px!important}.voice-button[aria-pressed=true]{color:#ec627a!important;background:#fff2f5!important}@keyframes transcribe-spin{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.voice-line i{transition:none}.transcribe-spinner{animation:none;border-right-color:#657aff}}
</style>
<style scoped>
.voice-inline{padding:2px 0 4px;margin:0;border:0;border-radius:0;background:transparent}.voice-line{height:32px;margin:0 0 3px;gap:3px}.voice-status{gap:8px;font-size:12px}.voice-status time{font-size:11px;color:#9ba5ba;margin-left:4px}.voice-status .voice-confirm,.voice-inline .voice-cancel{display:inline-flex;align-items:center;justify-content:center;min-width:44px;height:36px;padding:0 9px;border:0;border-radius:10px;background:#f0f2ff;color:#6978dc;font-size:12px;line-height:1;box-sizing:border-box}.voice-footer{margin-top:3px}.voice-processing{padding:5px 0}
</style>
