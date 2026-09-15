<template>
  <div class="login-page">
    <div class="login-canvas">
      <header class="login-brand">
        <img class="brand-icon" src="/avatars/assistant/A3.png" :alt="mt('智能提醒')" width="80" height="80" />
        <span class="brand-spark" aria-hidden="true"><i></i><i></i></span>
        <h1>{{ mt("智能提醒") }}</h1>
        <p>{{ mt("理解你的计划，在合适的时候重新判断") }}</p>
      </header>
      <form class="login-panel" @submit.prevent="login" :aria-busy="loading">
        <label for="login-account">{{ mt("账号") }}</label>
        <div class="input-wrap">
          <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="7" r="4"/><path d="M4 22v-2a8 8 0 0 1 16 0v2"/></svg>
          <input id="login-account" v-model.trim="form.username" autocomplete="username" :placeholder="mt('请输入账号')" :disabled="loading" />
        </div>
        <label for="login-password">{{ mt("密码") }}</label>
        <div class="input-wrap">
          <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="10" width="16" height="12" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4M12 15v3"/></svg>
          <input id="login-password" v-model="form.password" :type="showPassword?'text':'password'" autocomplete="current-password" :placeholder="mt('请输入密码')" :disabled="loading" />
          <button class="password-toggle" type="button" :aria-label="showPassword?mt('隐藏密码'):mt('显示密码')" :aria-pressed="showPassword" @click="showPassword=!showPassword">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/><path v-if="!showPassword" d="m3 3 18 18"/></svg>
          </button>
        </div>
        <button class="forgot-password" type="button" @click="forgotPassword">{{ mt("忘记密码？") }}</button>
        <button class="login-submit" type="submit" :disabled="loading">
          <span v-if="loading" class="submit-spinner" aria-hidden="true"></span>
          {{ loading ? mt('正在登录…') : mt('登录') }}
        </button>
        <button class="register-entry" type="button" :disabled="loading" @click="registerVisible=true">没有账号？注册账号</button>
      </form>
      <AccountForm v-model="registerVisible" register @success="registered" />
      <footer>{{ mt("更智能的提醒") }}<span>·</span>{{ mt("更从容的生活") }}</footer>
    </div>
  </div>
</template>

<script setup>
import {mt} from './mobileLocale';
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { bootstrap } from '@/api/smartReminder';
import AccountForm from './AccountForm.vue';
import {lastLoginAccount,rememberLoginAccount} from './loginMemory';
import {mobileError} from './mobileError.mjs';
const registerVisible = ref(false);
const registered = ({account}) => { form.username=account; form.password=''; ElMessage.success('注册成功，请使用刚设置的密码登录'); };

const router = useRouter(); const store = useStore(); const loading = ref(false), showPassword = ref(false);
const form = reactive({ tenantId:'000000', username:lastLoginAccount(), password:'', type:'account', deptId:'', roleId:'', key:'', code:'', mobile:true });
const forgotPassword = () => ElMessageBox.alert(mt('请联系管理员重置账号密码，重置后即可重新登录。'), mt('忘记密码'), {confirmButtonText:mt('知道了')}).catch(()=>{});
const login = async () => {
  if (loading.value) return;
  ElMessage.closeAll();
  if (!form.username || !form.password) return ElMessage.warning(mt('请输入账号和密码'));
  loading.value = true;
  try {
    await store.dispatch('LoginByUsername', form);
    if (!store.getters.token) throw new Error('登录失败，请检查账号密码');
    rememberLoginAccount(form.username);
    await bootstrap();
    await router.replace('/app/chat');
  } catch (e) { await store.dispatch('FedLogOut'); ElMessage.error({message:mt(mobileError(e,'登录未完成，请稍后重试')),grouping:true}); }
  finally { loading.value = false; }
};
</script>

<style scoped>
.login-page,.login-page *{box-sizing:border-box}
.register-entry{display:block;margin:18px auto 0;padding:8px;border:0;background:none;color:#4264e9;font-size:14px}
.login-page{min-height:100vh;min-height:100dvh;background:#eef3ff;font-family:"PingFang SC","Microsoft YaHei",sans-serif;color:#222945}
.login-canvas{container-type:inline-size;position:relative;width:100%;max-width:560px;min-height:100vh;min-height:100dvh;margin:auto;padding-bottom:max(30px,env(safe-area-inset-bottom));background:#f8fbff url('/images/login-background.png') top center/100% auto no-repeat;overflow:hidden}
.login-brand{position:relative;height:73cqw;padding:12.4cqw 10cqw 0;color:white}
.brand-icon{width:20cqw;height:20cqw;border-radius:4cqw;display:block;box-shadow:0 5px 15px #2356ef35}
.brand-spark{position:absolute;left:32cqw;top:13cqw;width:4.5cqw;height:5cqw;pointer-events:none}
.brand-spark i{position:absolute;display:block;width:1.25cqw;height:2.7cqw;border-radius:999px;background:#bfd2ff;opacity:.78;transform:rotate(42deg)}
.brand-spark i:first-child{left:.3cqw;top:0}
.brand-spark i:last-child{left:1.8cqw;top:2.2cqw;transform:rotate(76deg)}
.login-brand h1{margin:2.4cqw 0 1.5cqw;font-size:7.7cqw;font-weight:750;letter-spacing:.04em;line-height:1.3}
.login-brand p{font-size:2.65cqw;white-space:nowrap;margin:0;line-height:1.8}
.login-panel{position:relative;margin:0 6.5%;padding:7.3cqw 5.2cqw 6.3cqw;background:rgba(255,255,255,.94);border:1px solid #ffffffb8;border-radius:5.4cqw;box-shadow:0 20px 36px #3863bd24}
.login-panel label{display:block;font-size:max(14px,3.2cqw);font-weight:600;margin:0 0 1.8cqw}
.input-wrap{display:flex;align-items:center;gap:3.2cqw;height:10cqw;min-height:44px;padding:0 3.5cqw;border:1px solid #dedfeb;border-radius:2.5cqw;color:#797fac;background:#ffffff45;transition:border-color .2s,box-shadow .2s}
.input-wrap:focus-within{border-color:#5687ff;box-shadow:0 0 0 3px #517eff12}
.input-wrap svg{flex-shrink:0;width:4cqw;height:4cqw;min-width:19px;min-height:19px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}
.input-wrap input{width:100%;min-width:0;border:0;background:transparent;outline:none;color:#252e4c;font-size:max(16px,3.3cqw);height:100%;padding:0}
.input-wrap input::placeholder{color:#8185a4}
.input-wrap+label{margin-top:4.5cqw}
.password-toggle{border:0;background:transparent;color:#7980aa;display:grid;place-items:center;min-width:36px;min-height:44px;padding:0;cursor:pointer}
.forgot-password{display:block;margin:2.3cqw 0 4.3cqw auto;border:0;background:transparent;color:#1c60e9;font-size:max(12px,2.9cqw);min-height:32px;padding:0;cursor:pointer}
.login-submit{display:flex;align-items:center;justify-content:center;gap:4.5cqw;width:100%;height:11.7cqw;min-height:48px;border:0;border-radius:2.7cqw;background:linear-gradient(120deg,#66b6f8,#2864fc 68%,#6375ff);box-shadow:0 12px 25px #386cff30;color:#fff;font-size:max(17px,4cqw);font-weight:650;cursor:pointer}
.login-submit:disabled{opacity:.7;cursor:wait}
.hint{display:flex;align-items:center;justify-content:center;gap:2cqw;color:#8189ad;font-size:max(10px,2.45cqw);line-height:1.8;text-align:center;margin:5.1cqw 0 0}
.hint:before,.hint:after{content:'';height:1px;background:#d9dfea;flex:1;min-width:8px}
.login-canvas footer{color:#7c85ad;text-align:center;font-size:2.5cqw;letter-spacing:.2em;padding-top:11cqw;white-space:nowrap}
.login-canvas footer span{margin:0 2cqw}
.submit-spinner{width:18px;height:18px;border:2px solid #ffffff65;border-top-color:white;border-radius:50%;animation:login-spin .8s linear infinite}
button:focus-visible{outline:3px solid #8aa6ff;outline-offset:3px}
@keyframes login-spin{to{transform:rotate(360deg)}}
@media(prefers-reduced-motion:reduce){.submit-spinner{animation:none}}
</style>
