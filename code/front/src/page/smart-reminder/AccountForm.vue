<template>
  <el-dialog v-model="opened" :title="register ? '创建小醒账号' : '重置密码'" class="account-dialog" modal-class="account-overlay" width="min(440px, calc(100vw - 32px))" align-center append-to-body :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy" @closed="reset">
    <form class="account-form" :class="{'registration-grid':register}" @focusin="followFocus" @submit.prevent="submit" :aria-busy="busy">
      <template v-if="register">
        <label>登录账号 *<input v-model.trim="form.account" placeholder="账号" autocomplete="username" minlength="4" maxlength="32" required :disabled="busy" /></label>
        <label>姓名 *<input v-model.trim="form.name" autocomplete="name" maxlength="20" required :disabled="busy" /></label>
        <small class="full-row">{{suggesting?'正在生成默认账号…':suggestionError||'账号已自动生成，可修改。支持4–32位字母、数字、下划线或短横线。'}}</small>
      </template>
      <label v-else>原密码 *<input v-model="form.oldPassword" type="password" autocomplete="current-password" maxlength="128" required :disabled="busy" /></label>
      <div class="password-field full-row"><label :for="passwordId">{{ register ? '密码 *' : '新密码 *' }}</label><div class="password-control"><input :id="passwordId" v-model="form.password" :type="passwordVisible?'text':'password'" autocomplete="new-password" minlength="8" maxlength="64" required :disabled="busy" /><button type="button" class="password-eye" :aria-label="passwordVisible?'隐藏密码':'显示密码'" :aria-pressed="passwordVisible" :disabled="busy" @click="passwordVisible=!passwordVisible"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/><path v-if="!passwordVisible" d="m3 3 18 18"/></svg></button></div></div>
      <small class="full-row">{{ register ? '已预填默认密码，建议修改（8–64位）。' : '请输入8–64位新密码。修改后需重新登录。' }}</small>
      <label v-if="!register">确认新密码 *<input v-model="form.confirmation" type="password" autocomplete="new-password" minlength="8" maxlength="64" required :disabled="busy" /></label>
      <template v-if="register">
        <div class="contact-heading full-row">联系方式 <span>邮箱必填，手机号选填</span></div>
        <label class="full-row">手机号<input v-model.trim="form.phone" placeholder="11位手机号" type="tel" autocomplete="tel" maxlength="11" :disabled="busy" /></label>
        <label class="full-row">邮箱 *<input v-model.trim="form.email" placeholder="常用邮箱" type="email" autocomplete="email" required maxlength="45" :disabled="busy" /></label>
        <small class="full-row">填写的手机号、邮箱不能与其他账号重复。</small>
      </template>
      <p v-if="error" class="account-error full-row" role="alert">{{ error }}</p>
      <button class="account-submit full-row" :disabled="busy || (suggesting && !form.account)">{{ busy ? '正在提交…' : register ? '注册' : '确认修改' }}</button>
    </form>
  </el-dialog>
</template>
<script setup>
import { computed, reactive, ref, watch, useId, nextTick, onBeforeUnmount } from 'vue';
import { registerAccount, changeOwnPassword, suggestAccount } from '@/api/smartReminder';
import {mobileError} from './mobileError.mjs';
import { DEFAULT_PASSWORD, registrationError, passwordError } from './accountRules.mjs';
const props = defineProps({ modelValue: Boolean, register: Boolean });
const emit = defineEmits(['update:modelValue', 'success']);
const opened = computed({ get: () => props.modelValue, set: value => { if (!busy.value) emit('update:modelValue', value); } });
const form = reactive({}), busy = ref(false), error = ref('');
const passwordVisible=ref(false),passwordId=useId();
const suggesting=ref(false),suggestionError=ref('');let suggestionTicket=0,focusFrame=0,focusUntil=0;
const followFocus=()=>{
  cancelAnimationFrame(focusFrame);focusUntil=performance.now()+450;
  const update=()=>{
    if(!props.modelValue)return;
    const keyboard=parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--native-keyboard-height'))||0;
    const height=Math.min(window.visualViewport?.height||innerHeight,innerHeight-keyboard);
    document.documentElement.style.setProperty('--account-viewport-height',height+'px');
    const input=document.activeElement,body=input?.closest('.account-dialog .el-dialog__body');
    if(body){const b=body.getBoundingClientRect(),r=input.getBoundingClientRect();if(r.bottom>b.bottom-16)body.scrollTop+=r.bottom-b.bottom+16;else if(r.top<b.top+8)body.scrollTop-=b.top+8-r.top;}
    if(performance.now()<focusUntil)focusFrame=requestAnimationFrame(update);
  };focusFrame=requestAnimationFrame(update);
};
const stopFollowing=()=>{cancelAnimationFrame(focusFrame);document.documentElement.style.removeProperty('--account-viewport-height');window.visualViewport?.removeEventListener('resize',followFocus);window.removeEventListener('native:keyboard-layout',followFocus);};
watch(()=>props.modelValue,()=>{passwordVisible.value=false;});
const reset = () => { Object.assign(form, { account:'', name:'', password:props.register ? DEFAULT_PASSWORD : '', phone:'', email:'', oldPassword:'', confirmation:'' }); error.value=''; };
reset(); watch(() => props.modelValue, async value => {
  const ticket=++suggestionTicket;stopFollowing();suggesting.value=false;
  if(!value)return;
  reset();suggestionError.value='';
  window.visualViewport?.addEventListener('resize',followFocus);window.addEventListener('native:keyboard-layout',followFocus);await nextTick();followFocus();
  if(!props.register)return;
  suggesting.value=true;
  try { const response=await suggestAccount();if(ticket===suggestionTicket&&!form.account)form.account=response.data.data; }
  catch { if(ticket===suggestionTicket)suggestionError.value='默认账号暂未生成，可手动填写4–32位账号。'; }
  finally { if(ticket===suggestionTicket)suggesting.value=false; }
});
onBeforeUnmount(()=>{suggestionTicket++;stopFollowing();});
const submit = async () => {
  if (busy.value) return;
  error.value = props.register ? registrationError(form) : passwordError(form);
  if (error.value) return;
  busy.value = true;
  try {
    if (props.register) await registerAccount({ account:form.account, name:form.name, password:form.password, phone:form.phone, email:form.email });
    else await changeOwnPassword({ oldPassword:form.oldPassword, password:form.password, confirmation:form.confirmation });
    emit('success', props.register ? { account:form.account, password:form.password } : undefined);
    emit('update:modelValue', false);
    reset();
  } catch (e) { error.value = mobileError(e,'提交失败，请重试'); }
  finally { busy.value = false; }
};
</script>
<style scoped>
.password-field{display:grid;gap:6px}.password-control{position:relative}.account-form .password-control input{padding-right:48px}.password-eye{position:absolute;right:1px;top:1px;bottom:1px;width:44px;display:grid;place-items:center;border:0;background:transparent;color:#7788ab;border-radius:11px;cursor:pointer}.password-eye svg{width:20px;height:20px;fill:none;stroke:currentColor;stroke-width:1.6;stroke-linecap:round}.password-eye:focus-visible{outline:2px solid #647bff;outline-offset:-3px}.password-eye[aria-pressed=true]{color:#4264ff;background:#edf1ff80}
.account-form{display:grid;gap:12px;max-height:72dvh;overflow-y:auto;padding:2px;box-sizing:border-box}.account-form label{display:grid;gap:6px;font-size:14px;color:#354362}.account-form input{box-sizing:border-box;width:100%;min-width:0;min-height:44px;padding:10px 12px;border:1px solid #dce3f2;border-radius:12px;font-size:16px;background:#f8faff}.account-form small{font-size:12px;color:#8190a8;line-height:1.5}.account-error{margin:0;color:#b84444;font-size:13px;overflow-wrap:anywhere}.account-submit{border:0;border-radius:12px;background:#4264ff;color:white;min-height:46px;font-size:16px}.account-submit:disabled{opacity:.55}
</style>
<style>
.account-overlay .el-overlay-dialog{height:var(--account-viewport-height,calc(100dvh - var(--native-keyboard-height,0px)));padding:max(12px,env(safe-area-inset-top)) 0 12px;box-sizing:border-box}
.account-dialog.el-dialog{display:flex;flex-direction:column;margin:auto;padding:22px;border:1px solid #fff;border-radius:26px;max-height:calc(var(--account-viewport-height,100dvh) - max(24px,env(safe-area-inset-top)) - 24px)!important;overflow:hidden!important;box-shadow:0 22px 70px #203c6b30}
.account-dialog .el-dialog__header{flex:none;padding:0 26px 16px 0;margin:0}
.account-dialog .el-dialog__title{font-size:20px;font-weight:650;color:#263451}
.account-dialog .el-dialog__body{min-height:0;overflow-y:auto;overscroll-behavior:contain;scroll-padding:16px;padding:0 2px}
.account-dialog .account-form{max-height:none;overflow:visible;gap:10px;padding:2px 0}
.account-dialog .registration-grid{grid-template-columns:minmax(0,1fr) minmax(0,1fr)}
.account-dialog .full-row{grid-column:1/-1}
.account-dialog label{min-width:0}
.account-dialog input:focus{outline:0;border-color:#7187ff;box-shadow:0 0 0 3px #5269ff12;background:#fff}
.account-dialog .contact-heading{margin-top:6px;padding-top:12px;border-top:1px solid #edf0f7;color:#354362;font-size:14px;font-weight:600}
.account-dialog .contact-heading span{display:block;margin-top:4px;font-size:12px;color:#8390a8;font-weight:400}
.account-dialog .account-submit{margin-top:6px;background:linear-gradient(120deg,#507dff,#6662ff);box-shadow:0 6px 16px #506bff20;font-weight:600}
@media(max-width:350px){.account-dialog.el-dialog{padding:18px}.account-dialog .registration-grid{column-gap:8px}.account-dialog input{padding:10px 8px}}
</style>
