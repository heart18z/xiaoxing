<template>
  <router-view v-slot="{Component}"><Transition :name="isMobilePage?'mobile-route':''" mode="out-in"><KeepAlive :include="isMobilePage && route.path!=='/app/login' ? ['SmartReminderChat'] : []"><component :is="Component" /></KeepAlive></Transition></router-view>
  <transition name="mobile-loader-fade">
    <div v-if="isMobilePage && mobileLoadingVisible" class="mobile-page-loader" role="status" aria-live="polite" aria-label="页面加载中">
      <div class="loader-mark">
        <span class="loader-orbit"></span>
        <b>AI</b>
      </div>
      <strong>{{ mt('正在准备内容') }}</strong>
      <span class="loader-dots"><i></i><i></i><i></i></span>
    </div>
  </transition>
</template>

<script setup>
import { computed,watch } from 'vue';
import { useRoute } from 'vue-router';
import { mobileLoadingVisible } from '@/page/smart-reminder/mobileLoading';
import {mt} from '@/page/smart-reminder/mobileLocale';

const route = useRoute();
const isMobilePage = computed(() => route.path.startsWith('/app'));
watch(isMobilePage,value=>document.body.classList.toggle('smart-mobile-app',value),{immediate:true});
watch(()=>route.path==='/app/chat',value=>{
  document.documentElement.classList.toggle('smart-chat-page',value);
  document.body.classList.toggle('smart-chat-page',value);
},{immediate:true});
watch(()=>['/app/events','/app/me'].includes(route.path),value=>{
  document.documentElement.classList.toggle('smart-list-page',value);
  document.body.classList.toggle('smart-list-page',value);
},{immediate:true});
</script>

<style>
.mobile-route-enter-active,.mobile-route-leave-active{transition:opacity .16s ease}.mobile-route-enter-from,.mobile-route-leave-to{opacity:0}
.smart-mobile-app .el-dialog,.smart-mobile-app .el-message-box{max-width:calc(100vw - 40px);border-radius:22px}
.mobile-sheet-enter-active,.mobile-sheet-leave-active{transition:opacity .22s ease}.mobile-sheet-enter-active .permission-sheet,.mobile-sheet-leave-active .permission-sheet{transition:transform .26s cubic-bezier(.22,1,.36,1)}.mobile-sheet-enter-from,.mobile-sheet-leave-to{opacity:0}.mobile-sheet-enter-from .permission-sheet,.mobile-sheet-leave-to .permission-sheet{transform:translateY(100%)}
.smart-mobile-app button,.smart-mobile-app .sr-nav a{transition:background-color .18s,box-shadow .18s,scale .15s}.smart-mobile-app button:active,.smart-mobile-app .sr-nav a:active{scale:.97}
@media(prefers-reduced-motion:reduce){.mobile-route-enter-active,.mobile-route-leave-active,.mobile-sheet-enter-active,.mobile-sheet-leave-active,.mobile-sheet-enter-active .permission-sheet,.mobile-sheet-leave-active .permission-sheet,.smart-mobile-app button,.smart-mobile-app .sr-nav a{transition:none!important}.smart-mobile-app button:active,.smart-mobile-app .sr-nav a:active{scale:1}}
html,
body,
#app {
  width: 100%;
  height: 100%;
}
.mobile-page-loader{position:fixed;z-index:9999;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:11px;background:rgba(239,247,255,.92);backdrop-filter:blur(12px);color:#233052;font-family:"PingFang SC","Microsoft YaHei",sans-serif}.loader-mark{position:relative;width:74px;height:74px;display:grid;place-items:center}.loader-mark b{position:relative;z-index:2;width:48px;height:48px;display:grid;place-items:center;border-radius:17px;background:linear-gradient(145deg,#278cff,#526cff);color:#fff;font-size:17px;box-shadow:0 12px 28px rgba(49,91,255,.28)}.loader-orbit{position:absolute;inset:0;border:2px solid rgba(49,91,255,.15);border-top-color:#315bff;border-right-color:#45c8dd;border-radius:50%;animation:mobile-loader-spin 1s linear infinite}.mobile-page-loader>strong{font-size:15px;letter-spacing:.04em}.loader-dots{display:flex;gap:5px;height:8px}.loader-dots i{width:6px;height:6px;border-radius:50%;background:#6b83e9;animation:mobile-loader-bounce .8s ease-in-out infinite alternate}.loader-dots i:nth-child(2){animation-delay:.16s}.loader-dots i:nth-child(3){animation-delay:.32s}.mobile-loader-fade-enter-active,.mobile-loader-fade-leave-active{transition:opacity .2s ease}.mobile-loader-fade-enter-from,.mobile-loader-fade-leave-to{opacity:0}@keyframes mobile-loader-spin{to{transform:rotate(360deg)}}@keyframes mobile-loader-bounce{to{opacity:.3;transform:translateY(-4px)}}
</style>
