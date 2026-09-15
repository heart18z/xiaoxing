<template>
  <div ref="shell" :class="['sr-shell',variant && 'skin-'+variant]">
  <el-config-provider :locale="mobileLanguage==='en-us'?elementEn:elementZh">
    <header v-if="variant!=='profile'" class="sr-header">
      <slot name="leading" />
      <button v-if="back" class="sr-back" @click="goBack">‹</button>
      <slot name="title"><div>
        <strong>{{ title }}</strong>
        <small v-if="subtitle">{{ subtitle }}</small>
      </div></slot>
      <slot name="action" />
    </header>
    <main class="sr-main"><slot /></main>
    <nav v-if="!back" class="sr-nav" :style="{'--tab-index':tabIndex}">
	  <router-link class="event-entry" :class="{'tab-pending':pendingTab==='/app/events'}" to="/app/events" :aria-label="mt('事件')" @click="selectTab($event,'/app/events')">
        <span class="event-counter" :class="{compact:activeEventCount>99}" :title="activeEventCount+mt(' 个进行中的事件')" aria-live="polite"><Transition name="count-roll" mode="out-in"><b :key="activeEventCount">{{ activeEventCount }}</b></Transition></span>{{ mt("事件") }}</router-link>
      <router-link class="chat-entry" :class="{'tab-pending':pendingTab==='/app/chat'}" to="/app/chat" :aria-label="mt('对话')" @click="selectTab($event,'/app/chat')">
        <img class="assistant-tab-image" src="/images/ai-assistant-tab.png" alt="" aria-hidden="true" width="34" height="34" />{{ mt("AI助手") }}</router-link>
      <router-link class="me-entry" :class="{'tab-pending':pendingTab==='/app/me'}" to="/app/me" :aria-label="mt('我的')" @click="selectTab($event,'/app/me')">
		<span v-if="pendingFriendCount" class="nav-badge">{{ pendingFriendCount > 99 ? '99+' : pendingFriendCount }}</span>
        <svg class="nav-icon" viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="9" cy="7" r="3.5" /><path d="M2 21v-2a7 7 0 0 1 14 0v2M16 4a3.5 3.5 0 0 1 0 7M22 21v-2a6 6 0 0 0-4-5.65" />
        </svg>{{ mt("我的") }}</router-link>
    </nav>
  </el-config-provider>
  </div>
</template>

<script setup>
import {mt} from './mobileLocale';
import { computed,nextTick,onActivated,onDeactivated,onBeforeUnmount,onMounted,ref } from 'vue';
import {pendingTab,tabPaths} from './mobileNavigation';
import { onBeforeRouteLeave,useRouter } from 'vue-router';
import { silentReminder } from '@/api/smartReminder';
import { setAvatars } from './avatarState';
import {setMobileLanguage,mobileLanguage} from './mobileLocale';
import elementEn from 'element-plus/es/locale/lang/en';
import elementZh from 'element-plus/es/locale/lang/zh-cn';

const props=defineProps({ title: String, subtitle: String, back: Boolean, backTo: [String,Object], variant: String });
const router=useRouter(),activeEventCount=ref(0),pendingFriendCount=ref(0);
const shell=ref();let scrollPositions=[];
// KeepAlive preserves nodes, but browsers can reset nested scrolling when nodes are detached.
onBeforeRouteLeave(()=>{scrollPositions=[...(shell.value?.querySelectorAll('.sr-main,.event-list-scroll,.friend-scroll')||[])].map(el=>[el,el.scrollTop]);});
onActivated(async()=>{await nextTick();if(active&&shell.value?.isConnected)for(const [el,top] of scrollPositions)el.scrollTop=top;});
const tabIndex=computed(()=>Math.max(0,tabPaths.indexOf(pendingTab.value||router.currentRoute.value.path)));
const selectTab=(event,path)=>{if(router.currentRoute.value.path===path){event.preventDefault();return;}pendingTab.value=path;};
let active=true,badgeRevision=0;
const refreshBadge=async()=>{const revision=++badgeRevision;try{const data=(await silentReminder('bootstrap')).data.data||{};if(!active||revision!==badgeRevision)return;if(data.language)setMobileLanguage(data.language);setAvatars(data);activeEventCount.value=Number(data.activeEventCount||0);pendingFriendCount.value=Number(data.pendingFriendRequests||0);}catch{/* Keep the last known badge while offline. */}};
const goBack=()=>props.backTo?router.replace(props.backTo):router.back();
const refreshIfActive=()=>{if(active)void refreshBadge();};
let mountedActivation=false;
onMounted(()=>{refreshBadge();window.addEventListener('smart-reminder:badge-refresh',refreshIfActive);window.addEventListener('smart-reminder:profile-updated',refreshIfActive);});
onActivated(()=>{active=true;if(mountedActivation)void refreshBadge();mountedActivation=true;});onDeactivated(()=>{active=false;badgeRevision++;});
onBeforeUnmount(()=>{active=false;window.removeEventListener('smart-reminder:badge-refresh',refreshIfActive);window.removeEventListener('smart-reminder:profile-updated',refreshIfActive);});
</script>

<style src="./mobile.css"></style>
<style src="./mobile-skins.css"></style>
<style scoped>
.sr-nav{isolation:isolate}.sr-nav:before{content:'';position:absolute;z-index:0;top:8px;left:calc(var(--tab-index,0)*100%/3 + 9px);width:calc(100%/3 - 18px);height:48px;border-radius:17px;background:linear-gradient(135deg,#edf0ffb3,#e5edff66);transition:left .23s cubic-bezier(.22,1,.36,1);pointer-events:none}.sr-nav a{position:relative;z-index:1;touch-action:manipulation;-webkit-tap-highlight-color:transparent;border-radius:18px;transition:background-color .15s,color .15s,scale .15s!important}.sr-nav a:active{background:#dee6ffb8;scale:.93!important}.sr-nav a:focus-visible{outline:2px solid #7185ff;outline-offset:-5px}.sr-nav a.tab-pending{color:#4965ef}.sr-nav a.router-link-active .nav-icon,.sr-nav a.router-link-active .assistant-spark{animation:tab-arrive .24s cubic-bezier(.22,1,.36,1)}@keyframes tab-arrive{from{transform:translateY(3px) scale(.92)}to{transform:translateY(0) scale(1)}}@media(prefers-reduced-motion:reduce){.sr-nav:before,.sr-nav a{transition:none!important}.sr-nav a:active{scale:1!important}.sr-nav a.router-link-active .nav-icon,.sr-nav a.router-link-active .assistant-spark{animation:none}}
.event-counter{display:flex;align-items:center;justify-content:center;width:31px;height:29px;margin:0 auto 3px;border:1.5px solid currentColor;border-radius:9px;background:linear-gradient(150deg,#fff,#edf2ff);overflow:hidden;font-variant-numeric:tabular-nums}.event-counter b{font-size:21px;font-weight:750;line-height:1}.event-counter.compact{width:40px}.event-counter.compact b{font-size:17px}.count-roll-enter-active,.count-roll-leave-active{transition:opacity .2s,transform .2s}.count-roll-enter-from{opacity:0;transform:translateY(12px)}.count-roll-leave-to{opacity:0;transform:translateY(-12px)}@media(prefers-reduced-motion:reduce){.count-roll-enter-active,.count-roll-leave-active{transition:none}}
.sr-nav:before{left:calc(var(--tab-index,0)*100%/3 + 100%/6);transform:translateX(-50%);top:8px;width:76px;max-width:calc(100%/3 - 20px);height:49px;box-sizing:border-box;border:1px solid #ffffffd9;border-radius:21px;background:radial-gradient(ellipse at 50% 5%,#c6e7ff8c,transparent 75%),linear-gradient(155deg,#ecf5ffa6,#eeedff80);box-shadow:inset 0 1px 0 #fff,0 3px 12px #7890d510}
.sr-nav:after{content:'';position:absolute;top:5px;left:calc(var(--tab-index,0)*100%/3 + 100%/6);transform:translateX(-50%);width:18px;height:3px;border-radius:4px;background:linear-gradient(90deg,#71c5f7,#8882f5);opacity:.8;pointer-events:none;transition:left .23s cubic-bezier(.22,1,.36,1)}
.sr-nav a:active{background:#edf3ff80}
@media(prefers-reduced-motion:reduce){.sr-nav:after{transition:none}}
</style>
