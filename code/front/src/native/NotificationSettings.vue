<template>
  <section v-if="isNative" class="sr-card notification-settings">
    <h2>{{ en ? 'Phone notifications' : '手机通知' }}</h2>
    <p role="status">{{ status }}</p>
    <small>{{ en ? 'Notifications show your reminder content. Control lock-screen previews in iOS Settings. Tap to view the event.' : '通知将显示你的提醒内容，可在 iOS 设置中控制锁屏预览。点击通知查看对应事件。' }}</small>
    <button type="button" class="sr-button" :disabled="checking||pushState.busy" @click="check">{{ checking ? (en?'Checking…':'正在检查…') : pushState.permission==='denied'?(en?'Open iOS Settings':'前往系统设置'):(en?'Enable / check notifications':'开启 / 检查通知') }}</button>
    <p v-if="feedback" class="check-feedback" role="status" aria-live="polite">{{ feedback }}</p>
  </section>
</template>
<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue';
import { isNative } from './runtime';
import { pushState, enableNotifications, openNotificationSettings } from './notifications';
import { mobileLanguage } from '@/page/smart-reminder/mobileLocale';
const en = computed(() => mobileLanguage.value === 'en-us');
const checking=ref(false),feedback=ref('');let timeout,disposed=false;
const complete=()=>{clearTimeout(timeout);checking.value=false;feedback.value=(en.value?'Checked: ':'检查结果：')+status.value;};
watch(()=>[pushState.registered,pushState.error,pushState.backendReady],()=>{if(checking.value&&!pushState.busy&&(pushState.registered||pushState.error))complete();});
const check=async()=>{
  if(checking.value)return;
  if(pushState.permission==='denied'){await openNotificationSettings();feedback.value=en.value?'Return here after enabling notifications.':'开启系统通知权限后，返回这里再检查。';return;}
  checking.value=true;feedback.value=en.value?'Checking permission and device binding…':'正在检查通知权限和设备绑定…';
  await enableNotifications(true);
  if(disposed)return;
  if(pushState.registered||pushState.error||pushState.permission!=='granted')complete();
  else timeout=setTimeout(()=>{checking.value=false;feedback.value=en.value?'Registration timed out. Please retry.':'设备绑定暂未完成，请检查网络后重试。';},15000);
};
onBeforeUnmount(()=>{disposed=true;clearTimeout(timeout);});
const status = computed(() => {
  if (pushState.error) return en.value ? 'Could not complete notification setup. Please retry.' : '通知设置尚未完成，请检查网络后重试。';
  if (pushState.permission === 'denied') return en.value ? 'Notifications are disabled in iOS Settings.' : '系统通知权限已关闭，请前往系统设置开启。';
  if (pushState.registered && !pushState.backendReady) return en.value ? 'Permission granted; server APNs configuration is pending.' : '已获取通知权限，服务器 APNs 配置尚未就绪。';
  if (pushState.registered) return en.value ? 'Device registered. Delivery also depends on network and iOS settings.' : '设备已绑定。实际提醒还受网络、专注模式等系统设置影响。';
  return en.value ? 'Enable notifications to receive event reminders.' : '开启通知后可接收事件提醒。';
});
</script>
<style scoped>.notification-settings h2{font-size:18px;margin:0 0 12px}.notification-settings p{line-height:1.6}.notification-settings small{display:block;line-height:1.7;color:#8491aa}.notification-settings button{margin-top:16px;width:100%}</style>
