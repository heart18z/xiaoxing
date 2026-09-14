<template>
  <section class="alarm-control">
    <b>小醒系统闹铃 · 本机</b>
    <p v-if="active">已设置：{{ timeText(active.timestamp) }}（北京时间）</p>
    <p v-else-if="!alarmState.supported">需要 iOS 26+ 和支持闹铃的新版小醒。普通提醒不受影响。</p>
    <p v-else>按自己的任务时间设置，需单独授权；事件创建不代表闹铃已设置。</p>
    <p class="alarm-note">在小醒内管理，不进入苹果“时钟”列表。其他设备改期或停止后，请打开本机小醒同步；离线时已有闹铃仍按原时间响铃。</p>
    <p v-if="feedback||alarmState.error" role="status">{{ feedback||alarmState.error }}</p>
    <div><button type="button" :disabled="busy||!alarmState.supported" @click="set">{{ busy?'处理中…':active?'更新本机闹铃':'设置本机闹铃' }}</button><button v-if="active" type="button" :disabled="busy" @click="cancel">取消本机闹铃</button></div>
  </section>
</template>
<script setup>
import {computed,onMounted,ref} from 'vue';
import {alarmState,refreshAlarms,setEventAlarm,cancelEventAlarm} from './alarms';
const props=defineProps({detail:{type:Object,required:true}}),busy=ref(false),feedback=ref('');
const active=computed(()=>alarmState.items.find(item=>item.eventId===String(props.detail.event?.id)&&item.active));
const timeText=value=>new Date(value*1000).toLocaleString('zh-CN',{timeZone:'Asia/Shanghai',hour12:false});
async function set(){busy.value=true;feedback.value='';try{await setEventAlarm(props.detail);feedback.value='系统已确认：本机闹铃设置成功';}catch(e){feedback.value=e.message;}finally{busy.value=false;}}
async function cancel(){busy.value=true;feedback.value='';try{await cancelEventAlarm(props.detail.event.id);feedback.value='本机闹铃已取消，提醒事件仍保留';}catch(e){feedback.value=e.message;}finally{busy.value=false;}}
onMounted(()=>refreshAlarms().catch(e=>feedback.value=e.message));
</script>
<style scoped>
.alarm-control{margin-top:18px;padding:14px;border-radius:16px;background:#f0f3ff;color:#546286}.alarm-control b{font-size:14px}.alarm-control p{font-size:12px;line-height:1.6;margin:7px 0;overflow-wrap:anywhere}.alarm-control .alarm-note{color:#8993a8;font-size:11px}.alarm-control>div{display:flex;gap:8px;flex-wrap:wrap}.alarm-control button{border:0;border-radius:10px;background:#566dff;color:white;padding:10px;font-size:12px}.alarm-control button:disabled{opacity:.5}
</style>
