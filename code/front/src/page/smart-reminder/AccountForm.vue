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
        <label class="full-row">手机号<input v-model.trim="form.phone" @blur="checkContact('phone')" placeholder="11位手机号" type="tel" autocomplete="tel" maxlength="11" :disabled="busy || sending" /><small aria-live="polite">{{ hints.phone }}</small></label>
        <label class="full-row">邮箱 *<input v-model.trim="form.email" @blur="checkContact('email')" placeholder="常用邮箱" type="email" autocomplete="email" required maxlength="45" :disabled="busy || sending" /><small aria-live="polite">{{ hints.email }}</small></label>
        <label class="full-row">邮箱验证码 *<div class="email-code"><input v-model="form.emailCode" inputmode="numeric" autocomplete="one-time-code" placeholder="6位验证码" pattern="[0-9]{6}" maxlength="6" required :disabled="busy"/><button type="button" :disabled="sending || cooldown > 0 || busy" @click="sendCode">{{sending ? "发送中…" : cooldown > 0 ? cooldown + "s" : "获取验证码"}}</button></div><small>验证码10分钟内有效，验证通过后才能注册</small></label>
        <small class="full-row">填写的手机号、邮箱不能与其他账号重复。</small>
      </template>
      <p v-if="error" class="account-error full-row" role="alert">{{ error }}</p>
      <button class="account-submit full-row" :disabled="busy || sending || (suggesting && !form.account)">{{ busy ? '正在提交…' : register ? '注册' : '确认修改' }}</button>
    </form>
  </el-dialog>
</template>
<script setup>
import { computed, reactive, ref, watch, useId, nextTick, onBeforeUnmount } from 'vue';
import { registerAccount, changeOwnPassword, suggestAccount, checkRegistrationContact, sendRegistrationCode } from '@/api/smartReminder';
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

const hints=reactive({email:'',phone:''}),sending=ref(false),cooldown=ref(0);
const checkIds={email:0,phone:0},checkTimers={email:0,phone:0};let codeTimer=0,sendTicket=0;
function stopChecks(){for(const field of ['email','phone']){clearTimeout(checkTimers[field]);checkIds[field]++;}clearInterval(codeTimer);cooldown.value=0;sending.value=false;sendTicket++;}
async function checkContact(field){
  clearTimeout(checkTimers[field]);const id=++checkIds[field],value=(form[field]||'').trim().toLowerCase();
  if(!props.modelValue||!props.register||!value)return field==='phone';
  if(!(field==='email'?/^[^\s@]+@[^\s@]+\.[^\s@]+$/:/^1[3-9]\d{9}$/).test(value)){hints[field]=field==='email'?'请输入正确的邮箱':'请输入正确的11位手机号';return false;}
  hints[field]='正在检查…';
  try{const {data}=await checkRegistrationContact({field,value});if(id!==checkIds[field])return false;hints[field]=data.data.available?(field==='email'?'邮箱可以使用':'手机号可以使用'):(field==='email'?'邮箱已被使用，请更换邮箱':'手机号已被使用，请更换手机号');return data.data.available;}
  catch(e){if(id===checkIds[field])hints[field]=mobileError(e,'检查失败，请重试');return false;}
}
async function sendCode(){
  if(sending.value||busy.value||cooldown.value>0)return;
  sending.value=true;error.value='';const ticket=++sendTicket;
  try{if(!await checkContact('email'))return;const target=form.email.trim().toLowerCase();await sendRegistrationCode(target);if(ticket!==sendTicket)return;
    cooldown.value=60;clearInterval(codeTimer);codeTimer=setInterval(()=>{if(--cooldown.value<=0)clearInterval(codeTimer);},1000);
    if(target===form.email.trim().toLowerCase())hints.email='验证码已发送，请检查邮箱或垃圾邮件';
  }catch(e){if(ticket===sendTicket)error.value=mobileError(e,'发送失败，请重试');}finally{if(ticket===sendTicket)sending.value=false;}
}
for(const field of ['email','phone'])watch(()=>form[field],()=>{clearTimeout(checkTimers[field]);checkIds[field]++;hints[field]='';if(field==='email')form.emailCode='';if(props.modelValue&&props.register)checkTimers[field]=setTimeout(()=>checkContact(field),500);});

const reset = () => { stopChecks();form.emailCode='';hints.email='';hints.phone=''; Object.assign(form, { account:'', name:'', password:props.register ? DEFAULT_PASSWORD : '', phone:'', email:'', oldPassword:'', confirmation:'' }); error.value=''; };
reset(); watch(() => props.modelValue, async value => {
  const ticket=++suggestionTicket;stopFollowing();suggesting.value=false;
  if(!value){stopChecks();return;}
  reset();suggestionError.value='';
  window.visualViewport?.addEventListener('resize',followFocus);window.addEventListener('native:keyboard-layout',followFocus);await nextTick();followFocus();
  if(!props.register)return;
  suggesting.value=true;
  try { const response=await suggestAccount();if(ticket===suggestionTicket&&!form.account)form.account=response.data.data; }
  catch { if(ticket===suggestionTicket)suggestionError.value='默认账号暂未生成，可手动填写4–32位账号。'; }
  finally { if(ticket===suggestionTicket)suggesting.value=false; }
});
onBeforeUnmount(()=>{suggestionTicket++;stopFollowing();stopChecks();});
const submit = async () => {
  if (busy.value || sending.value) return;
  error.value = props.register ? registrationError(form) : passwordError(form);
  if (!error.value && props.register && !/^[0-9]{6}$/.test(form.emailCode||'')) error.value='请输入6位邮箱验证码';
  if (error.value) return;
  busy.value = true;
  try {
    if (props.register) {
      if (!await checkContact('email') || !await checkContact('phone')) { error.value='请检查邮箱和手机号的提示';return; }
      await registerAccount({ account:form.account, name:form.name, password:form.password, phone:form.phone, email:form.email, emailCode:form.emailCode });
    }
    else await changeOwnPassword({ oldPassword:form.oldPassword, password:form.password, confirmation:form.confirmation });
    emit('success', props.register ? { account:form.account, password:form.password } : undefined);
    emit('update:modelValue', false);
    reset();
  } catch (e) { error.value = mobileError(e,'提交失败，请重试'); }
  finally { busy.value = false; }
};
</script>
<style scoped>
.email-code{display:flex;gap:8px}.email-code input{min-width:0;flex:1}.email-code button{flex:none;border:0;border-radius:12px;padding:0 14px;background:#eef0ff;color:#4c5cff;font-weight:600}.email-code button:disabled{opacity:.55}

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
