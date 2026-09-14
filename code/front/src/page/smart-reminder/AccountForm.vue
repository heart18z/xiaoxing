<template>
  <el-dialog v-model="opened" :title="register ? '注册账号' : '重置密码'" width="min(420px, calc(100vw - 32px))" align-center append-to-body :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy" @closed="reset">
    <form class="account-form" @submit.prevent="submit" :aria-busy="busy">
      <template v-if="register">
        <label>登录账号 *<input v-model.trim="form.account" placeholder="人员号" autocomplete="username" minlength="4" maxlength="32" required :disabled="busy" /></label>
        <small>4–32位字母、数字、下划线或短横线，首位为字母或数字。</small>
        <label>姓名 *<input v-model.trim="form.name" autocomplete="name" maxlength="20" required :disabled="busy" /></label>
      </template>
      <label v-else>原密码 *<input v-model="form.oldPassword" type="password" autocomplete="current-password" maxlength="128" required :disabled="busy" /></label>
      <div class="password-field"><label :for="passwordId">{{ register ? '密码 *' : '新密码 *' }}</label><div class="password-control"><input :id="passwordId" v-model="form.password" :type="passwordVisible?'text':'password'" autocomplete="new-password" minlength="8" maxlength="64" required :disabled="busy" /><button type="button" class="password-eye" :aria-label="passwordVisible?'隐藏密码':'显示密码'" :aria-pressed="passwordVisible" :disabled="busy" @click="passwordVisible=!passwordVisible"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/><path v-if="!passwordVisible" d="m3 3 18 18"/></svg></button></div></div>
      <small>{{ register ? '已预填默认密码，建议改为自己的密码（8–64位）。' : '请输入8–64位新密码。修改后需重新登录。' }}</small>
      <label v-if="!register">确认新密码 *<input v-model="form.confirmation" type="password" autocomplete="new-password" minlength="8" maxlength="64" required :disabled="busy" /></label>
      <template v-if="register">
        <label>手机号（选填）<input v-model.trim="form.phone" type="tel" autocomplete="tel" maxlength="11" :disabled="busy" /></label>
        <label>邮箱（选填）<input v-model.trim="form.email" type="email" autocomplete="email" maxlength="45" :disabled="busy" /></label>
      </template>
      <p v-if="error" class="account-error" role="alert">{{ error }}</p>
      <button class="account-submit" :disabled="busy">{{ busy ? '正在提交…' : register ? '注册' : '确认修改' }}</button>
    </form>
  </el-dialog>
</template>
<script setup>
import { computed, reactive, ref, watch, useId } from 'vue';
import { registerAccount, changeOwnPassword } from '@/api/smartReminder';
import { DEFAULT_PASSWORD, registrationError, passwordError } from './accountRules.mjs';
const props = defineProps({ modelValue: Boolean, register: Boolean });
const emit = defineEmits(['update:modelValue', 'success']);
const opened = computed({ get: () => props.modelValue, set: value => { if (!busy.value) emit('update:modelValue', value); } });
const form = reactive({}), busy = ref(false), error = ref('');
const passwordVisible=ref(false),passwordId=useId();
watch(()=>props.modelValue,()=>{passwordVisible.value=false;});
const reset = () => { Object.assign(form, { account:'', name:'', password:props.register ? DEFAULT_PASSWORD : '', phone:'', email:'', oldPassword:'', confirmation:'' }); error.value=''; };
reset(); watch(() => props.modelValue, value => { if (value) reset(); });
const submit = async () => {
  if (busy.value) return;
  error.value = props.register ? registrationError(form) : passwordError(form);
  if (error.value) return;
  busy.value = true;
  try {
    if (props.register) await registerAccount({ account:form.account, name:form.name, password:form.password, phone:form.phone, email:form.email });
    else await changeOwnPassword({ oldPassword:form.oldPassword, password:form.password, confirmation:form.confirmation });
    emit('success', props.register ? { account:form.account } : undefined);
    emit('update:modelValue', false);
    reset();
  } catch (e) { error.value = e?.response?.data?.msg || e?.message || '提交失败，请重试'; }
  finally { busy.value = false; }
};
</script>
<style scoped>
.password-field{display:grid;gap:6px}.password-control{position:relative}.account-form .password-control input{padding-right:48px}.password-eye{position:absolute;right:1px;top:1px;bottom:1px;width:44px;display:grid;place-items:center;border:0;background:transparent;color:#7788ab;border-radius:11px;cursor:pointer}.password-eye svg{width:20px;height:20px;fill:none;stroke:currentColor;stroke-width:1.6;stroke-linecap:round}.password-eye:focus-visible{outline:2px solid #647bff;outline-offset:-3px}.password-eye[aria-pressed=true]{color:#4264ff;background:#edf1ff80}
.account-form{display:grid;gap:12px;max-height:72dvh;overflow-y:auto;padding:2px;box-sizing:border-box}.account-form label{display:grid;gap:6px;font-size:14px;color:#354362}.account-form input{box-sizing:border-box;width:100%;min-width:0;min-height:44px;padding:10px 12px;border:1px solid #dce3f2;border-radius:12px;font-size:16px;background:#f8faff}.account-form small{font-size:12px;color:#8190a8;line-height:1.5}.account-error{margin:0;color:#b84444;font-size:13px;overflow-wrap:anywhere}.account-submit{border:0;border-radius:12px;background:#4264ff;color:white;min-height:46px;font-size:16px}.account-submit:disabled{opacity:.55}
</style>
