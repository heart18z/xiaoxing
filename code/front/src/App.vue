<template>
  <div class="route-stage" :class="{'mobile-route-stage':isMobilePage}"><router-view v-slot="{Component}"><Transition :name="isMobilePage?'mobile-route':''" :duration="isMobilePage?180:0" @before-leave="el=>{if(isMobilePage)el.inert=true}" @after-leave="el=>el.inert=false" @before-enter="el=>el.inert=false"><KeepAlive :include="isMobilePage && route.path!=='/app/login' ? ['SmartReminderChat','SmartReminderEvents','SmartReminderMe'] : []" :max="3"><component :is="Component" /></KeepAlive></Transition></router-view></div>
  <transition name="mobile-loader-fade">
    <div v-if="isMobilePage && mobileLoadingVisible" class="mobile-page-loader" role="status" aria-live="polite" aria-label="页面加载中">
      <span class="loading-track"></span><span class="sr-only">{{ mt('正在准备内容') }}</span>
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
.route-stage:not(.mobile-route-stage){height:100%;min-height:0}
.route-stage{min-height:100%;position:relative}.mobile-route-stage{background:#edf4ff;isolation:isolate;min-height:100dvh}.mobile-route-leave-active{position:absolute!important;inset:0;width:100%;pointer-events:none;z-index:0}.mobile-route-enter-active{position:relative;z-index:1}.mobile-route-enter-active .sr-main,.mobile-route-enter-active .sr-header{transition:opacity .18s ease,translate .18s cubic-bezier(.22,1,.36,1)}.mobile-route-enter-from .sr-main,.mobile-route-enter-from .sr-header{opacity:.65;translate:0 7px}.mobile-route-leave-active .sr-main{pointer-events:none}.mobile-route-enter-active .sr-nav,.mobile-route-leave-active .sr-nav{opacity:1}
.smart-mobile-app .el-dialog,.smart-mobile-app .el-message-box{max-width:calc(100vw - 40px);border-radius:22px}
.mobile-sheet-enter-active,.mobile-sheet-leave-active{transition:opacity .22s ease}.mobile-sheet-enter-active .permission-sheet,.mobile-sheet-leave-active .permission-sheet{transition:transform .26s cubic-bezier(.22,1,.36,1)}.mobile-sheet-enter-from,.mobile-sheet-leave-to{opacity:0}.mobile-sheet-enter-from .permission-sheet,.mobile-sheet-leave-to .permission-sheet{transform:translateY(100%)}
.smart-mobile-app button,.smart-mobile-app .sr-nav a{transition:background-color .18s,box-shadow .18s,scale .15s}.smart-mobile-app button:active,.smart-mobile-app .sr-nav a:active{scale:.97}
@media(prefers-reduced-motion:reduce){.mobile-route-enter-active,.mobile-route-leave-active,.mobile-sheet-enter-active,.mobile-sheet-leave-active,.mobile-sheet-enter-active .permission-sheet,.mobile-sheet-leave-active .permission-sheet,.smart-mobile-app button,.smart-mobile-app .sr-nav a{transition:none!important}.smart-mobile-app button:active,.smart-mobile-app .sr-nav a:active{scale:1}}
@media(prefers-reduced-motion:reduce){.mobile-route-enter-active .sr-main,.mobile-route-enter-active .sr-header{transition:none!important}.mobile-route-enter-from .sr-main,.mobile-route-enter-from .sr-header{opacity:1;translate:none}}
html,
body,
#app {
  width: 100%;
  height: 100%;
}
.mobile-page-loader{position:fixed;z-index:9999}.mobile-loader-fade-enter-active,.mobile-loader-fade-leave-active{transition:opacity .15s ease}.mobile-loader-fade-enter-from,.mobile-loader-fade-leave-to{opacity:0}
.mobile-page-loader{inset:auto;top:env(safe-area-inset-top,0px);left:50%;transform:translateX(-50%);width:min(100%,760px);height:3px;display:block;background:#dce5ff60;backdrop-filter:none;overflow:hidden;pointer-events:none}.loading-track{display:block;width:35%;height:100%;border-radius:3px;background:linear-gradient(90deg,#6375ff,#68d7f9);animation:loading-track 1.2s ease-in-out infinite}.sr-only{position:absolute;width:1px;height:1px;overflow:hidden;clip-path:inset(50%)}@keyframes loading-track{from{transform:translateX(-100%)}to{transform:translateX(290%)}}@media(prefers-reduced-motion:reduce){.loading-track{animation:none;width:100%}}
</style>
