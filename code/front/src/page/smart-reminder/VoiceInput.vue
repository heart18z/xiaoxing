<template>
  <button type="button" class="voice-button" :disabled="disabled" :aria-label="mt('语音输入')" :title="mt('语音输入')" @click="open"><UiIcon name="microphone" /></button>
  <el-dialog v-model="visible" :title="mt('语音输入')" width="min(92vw,440px)" align-center append-to-body :close-on-click-modal="false" class="voice-input-dialog" @close="cleanup">
    <div class="voice-panel" aria-live="polite">
      <p class="voice-note">{{ mt("识别后填入输入框，确认发送后才交给 AI。") }}</p>
      <p v-if="!secure" class="voice-warning">{{ mt("当前 HTTP 地址无法使用麦克风，请通过 HTTPS 访问，或选择已录制的音频文件。") }}</p>
      <div ref="waveElement" class="voice-wave" :aria-label="mt('实时录音波形')"></div>
      <strong>{{ status==='recording' ? `${mt('正在聆听')} ${seconds}s / 90s` : status==='transcribing' ? mt('正在转为文字…') : status==='opening' ? mt('请允许使用麦克风…') : status==='loading' ? mt('正在准备录音组件…') : mt('说出你想提醒的事') }}</strong>
      <p v-if="error" class="voice-error" role="alert">{{ mt(error) }}</p>
      <div class="voice-controls">
        <button v-if="status==='recording'" type="button" class="sr-button" @click="finish">{{ mt("完成并转文字") }}</button>
        <button v-else-if="secure" type="button" class="sr-button" :disabled="!['idle','error'].includes(status)" @click="start">{{ lastFile?mt('重新录音'):mt('开始录音') }}</button>
        <button v-if="status==='error'&&lastFile" type="button" class="sr-button secondary" @click="recognize(lastFile)">{{ mt("重试识别") }}</button>
        <button type="button" class="voice-cancel" @click="visible=false">{{ mt("取消") }}</button>
      </div>
      <label class="audio-file" :class="{busy:busy}">{{ mt("选择音频文件") }}<input type="file" accept=".wav,.mp3,.m4a,.mp4,.webm,.ogg,.flac,.aac,audio/*" :disabled="busy" @change="chooseFile" /></label>
      <small>{{ mt("录音最长 90 秒；音频文件最大 20MB。录音仅用于转写。") }}</small>
    </div>
  </el-dialog>
</template>
<script setup>
import {mt} from './mobileLocale';
import {ref,computed,nextTick,onBeforeUnmount,onDeactivated,watch} from 'vue';
import UiIcon from './UiIcon.vue';
import {transcribeAudio} from '@/api/smartReminder';
import {ElMessage} from 'element-plus';
defineProps({disabled:Boolean});const emit=defineEmits(['text','busy']);
const visible=ref(false),status=ref('idle'),seconds=ref(0),error=ref(''),waveElement=ref(),secure=window.isSecureContext&&!!navigator.mediaDevices?.getUserMedia;
const busy=computed(()=>['loading','opening','recording','transcribing'].includes(status.value));watch(busy,v=>emit('busy',v));
const lastFile=ref(null);
let Recorder,recorder,wave,controller,generation=0,limitTimer;
const release=()=>{clearTimeout(limitTimer);limitTimer=null;if(wave?.timer)clearInterval(wave.timer);wave=null;const rec=recorder;recorder=null;rec?.close();};
const cleanup=()=>{generation++;controller?.abort();controller=null;release();lastFile.value=null;status.value='idle';emit('busy',false);};
const open=async()=>{
  if(!secure){ElMessage.warning(mt('当前 HTTP 环境不支持语音输入，请使用 HTTPS 地址'));return;}
  visible.value=true;error.value='';seconds.value=0;lastFile.value=null;
  if(!secure)return;
  const run=++generation;status.value='loading';
  try{Recorder=(await import('recorder-core')).default;await import('recorder-core/src/engine/wav.js');await import('recorder-core/src/extensions/waveview.js');if(run===generation&&visible.value)status.value='idle';}
  catch{if(run===generation){error.value='录音组件加载失败，请重试或选择音频文件';status.value='error';}}
};
const start=async()=>{
  if(!Recorder||!secure||busy.value)return;
  release();error.value='';seconds.value=0;status.value='opening';lastFile.value=null;const run=++generation;
  await nextTick();
  const rec=Recorder({type:'wav',sampleRate:16000,bitRate:16,onProcess:(buffers,power,duration,sampleRate)=>{
    if(run!==generation||status.value!=='recording')return;seconds.value=Math.floor(duration/1000);
    wave?.input(buffers[buffers.length-1],power,sampleRate);
  }});recorder=rec;
  rec.open(()=>{
    if(run!==generation||!visible.value){rec.close();return;}
    try{wave=Recorder.WaveView({elem:waveElement.value,lineWidth:2,keep:false,fps:20});rec.start();status.value='recording';limitTimer=setTimeout(finish,90000);}
    catch{release();error.value='无法开始录音，请检查设备或选择音频文件';status.value='error';}
  },()=>{if(run===generation){release();error.value='无法使用麦克风，请检查权限和设备，或选择音频文件';status.value='error';}});
};
const finish=()=>{
  if(status.value!=='recording'||!recorder)return;
  status.value='transcribing';clearTimeout(limitTimer);const run=generation,rec=recorder;
  rec.stop((blob,duration)=>{
    if(run!==generation)return;release();
    if(duration<500){error.value='录音太短，请至少说半秒钟';status.value='error';return;}
    recognize(new File([blob],'recording.wav',{type:'audio/wav'}));
  },()=>{if(run===generation){release();error.value='录音失败，请重录';status.value='error';}});
};
const recognize=async file=>{
  if(file.size>20*1024*1024||!file.size){error.value='请选择 20MB 以内的有效音频文件';status.value='error';return;}
  if(!/\.(wav|mp3|m4a|mp4|webm|ogg|flac|aac)$/i.test(file.name)){error.value='不支持此音频格式';status.value='error';return;}
  lastFile.value=file;status.value='transcribing';error.value='';const run=++generation;controller=new AbortController();
  try{const response=await transcribeAudio(file,controller.signal);if(run!==generation||!visible.value)return;const text=response.data.data?.text?.trim();if(!text)throw Error('未识别到语音，请重试');emit('text',text);visible.value=false;}
  catch(e){if(run===generation){error.value=e?.response?.data?.msg||e?.message||'识别失败，请重试';status.value='error';}}
};
const chooseFile=e=>{const file=e.target.files?.[0];e.target.value='';if(file)recognize(file);};
const visibilityChanged=()=>{if(document.hidden&&visible.value){visible.value=false;cleanup();}};
document.addEventListener('visibilitychange',visibilityChanged);
onDeactivated(()=>{visible.value=false;cleanup();});onBeforeUnmount(()=>{cleanup();document.removeEventListener('visibilitychange',visibilityChanged);});
</script>
<style scoped>
:global(.voice-input-dialog.el-dialog){padding:20px;border-radius:24px}:global(.voice-input-dialog .el-dialog__header){padding:0 25px 12px 0;margin:0}:global(.voice-input-dialog .el-dialog__body){padding:0}
.voice-panel{text-align:center;color:#34405f}.voice-note{font-size:12px;color:#8a93aa;margin:5px 0}.voice-wave{height:88px;width:100%;margin:10px 0;background:linear-gradient(180deg,#faf9ff,#f1f8ff);border-radius:15px;overflow:hidden}.voice-panel strong{display:block;font-size:16px}.voice-panel small{display:block;margin-top:10px;font-size:11px;color:#8b94aa;line-height:1.5}.voice-warning,.voice-error{font-size:12px;line-height:1.6;color:#ad685a;background:#fff4ee;padding:10px;border-radius:12px}.voice-controls{display:flex;justify-content:center;gap:9px;margin:20px 0 12px}.voice-cancel{border:0;border-radius:12px;padding:10px 16px;background:#f0f2f8;color:#6b7891}.audio-file{display:inline-block;position:relative;color:#526be1;font-size:13px;cursor:pointer;padding:8px}.audio-file input{position:absolute;inset:0;width:100%;opacity:0;cursor:pointer}.audio-file.busy{opacity:.5;pointer-events:none}
</style>
