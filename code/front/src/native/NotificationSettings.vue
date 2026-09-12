<template>
  <section v-if="isNative" class="sr-card notification-settings">
    <h2>{{ en ? 'Phone notifications' : '手机通知' }}</h2>
    <p role="status">{{ status }}</p>
    <small>{{ en ? 'Lock-screen previews hide task details. Tap a notification to open its event after signing in.' : '锁屏通知不显示任务详情，点击通知后登录查看对应事件。' }}</small>
    <button type="button" class="sr-button" :disabled="pushState.busy" @click="pushState.permission==='denied'?openNotificationSettings():enableNotifications(true)">{{ pushState.permission==='denied'?(en?'Open iOS Settings':'前往系统设置'):(en?'Enable / check notifications':'开启 / 检查通知') }}</button>
  </section>
</template>
<script setup>
import { computed } from 'vue';
import { isNative } from './runtime';
import { pushState, enableNotifications, openNotificationSettings } from './notifications';
import { mobileLanguage } from '@/page/smart-reminder/mobileLocale';
const en = computed(() => mobileLanguage.value === 'en-us');
const status = computed(() => {
  if (pushState.error) return en.value ? 'Could not complete notification setup. Please retry.' : '通知设置尚未完成，请检查网络后重试。';
  if (pushState.permission === 'denied') return en.value ? 'Notifications are disabled in iOS Settings.' : '系统通知权限已关闭，请前往系统设置开启。';
  if (pushState.registered && !pushState.backendReady) return en.value ? 'Permission granted; server APNs configuration is pending.' : '已获取通知权限，服务器 APNs 配置尚未就绪。';
  if (pushState.registered) return en.value ? 'Device registered. Delivery also depends on network and iOS settings.' : '设备已绑定。实际提醒还受网络、专注模式等系统设置影响。';
  return en.value ? 'Enable notifications to receive event reminders.' : '开启通知后可接收事件提醒。';
});
</script>
<style scoped>.notification-settings h2{font-size:18px;margin:0 0 12px}.notification-settings p{line-height:1.6}.notification-settings small{display:block;line-height:1.7;color:#8491aa}.notification-settings button{margin-top:16px;width:100%}</style>
