<template>
  <div :class="['sr-shell',variant && 'skin-'+variant]">
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
    <nav v-if="!back" class="sr-nav">
	  <router-link class="event-entry" to="/app/events" :aria-label="mt('事件')">
        <span class="event-counter" :class="{compact:activeEventCount>99}" :title="activeEventCount+mt(' 个进行中的事件')" aria-live="polite"><Transition name="count-roll" mode="out-in"><b :key="activeEventCount">{{ activeEventCount }}</b></Transition></span>{{ mt("事件") }}</router-link>
      <router-link class="chat-entry" to="/app/chat" :aria-label="mt('对话')">
        <span class="assistant-spark" aria-hidden="true"></span>{{ mt("AI助手") }}</router-link>
      <router-link class="me-entry" to="/app/me" :aria-label="mt('我的')">
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
import { onActivated, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { bootstrap } from '@/api/smartReminder';
import { setAvatars } from './avatarState';
import {setMobileLanguage,mobileLanguage} from './mobileLocale';
import elementEn from 'element-plus/es/locale/lang/en';
import elementZh from 'element-plus/es/locale/lang/zh-cn';

const props=defineProps({ title: String, subtitle: String, back: Boolean, backTo: [String,Object], variant: String });
const router=useRouter(),activeEventCount=ref(0),pendingFriendCount=ref(0);
const refreshBadge=async()=>{try{const data=(await bootstrap()).data.data||{};if(data.language)setMobileLanguage(data.language);setAvatars(data);activeEventCount.value=Number(data.activeEventCount||0);pendingFriendCount.value=Number(data.pendingFriendRequests||0);}catch(e){activeEventCount.value=0;pendingFriendCount.value=0;}};
const goBack=()=>props.backTo?router.replace(props.backTo):router.back();
onMounted(()=>{refreshBadge();window.addEventListener('smart-reminder:badge-refresh',refreshBadge);window.addEventListener('smart-reminder:profile-updated',refreshBadge);});
onActivated(refreshBadge);
onBeforeUnmount(()=>{window.removeEventListener('smart-reminder:badge-refresh',refreshBadge);window.removeEventListener('smart-reminder:profile-updated',refreshBadge);});
</script>

<style src="./mobile.css"></style>
<style src="./mobile-skins.css"></style>
<style scoped>
.event-counter{display:flex;align-items:center;justify-content:center;width:31px;height:29px;margin:0 auto 3px;border:1.5px solid currentColor;border-radius:9px;background:linear-gradient(150deg,#fff,#edf2ff);overflow:hidden;font-variant-numeric:tabular-nums}.event-counter b{font-size:21px;font-weight:750;line-height:1}.event-counter.compact{width:40px}.event-counter.compact b{font-size:17px}.count-roll-enter-active,.count-roll-leave-active{transition:opacity .2s,transform .2s}.count-roll-enter-from{opacity:0;transform:translateY(12px)}.count-roll-leave-to{opacity:0;transform:translateY(-12px)}@media(prefers-reduced-motion:reduce){.count-roll-enter-active,.count-roll-leave-active{transition:none}}
</style>
