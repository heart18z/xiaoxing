<template>
  <basic-container>
    <h2>注册邮箱验证</h2>
    <p>用户必须输入有效邮箱验证码才能注册。修改后即时生效，无需重新部署。</p>
    <el-form v-loading="loading" label-width="135px" class="mail-config" @submit.prevent="save">
      <el-form-item label="SMTP 服务器"><el-input v-model.trim="form.host" placeholder="mail.sinoaopt.com（不含 https://）" maxlength="253" /></el-form-item>
      <el-form-item label="端口"><el-input-number v-model="form.port" :min="1" :max="65535" /></el-form-item>
      <el-form-item label="连接加密"><el-select v-model="form.security"><el-option label="STARTTLS（通常为587）" value="STARTTLS"/><el-option label="SSL/TLS（通常为465）" value="SSL"/></el-select></el-form-item>
      <el-form-item label="发信账号"><el-input v-model.trim="form.username" autocomplete="off" placeholder="fmemo@sinoaopt.com" maxlength="45"/></el-form-item>
      <el-form-item label="密码 / 应用密码"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" maxlength="512" :placeholder="passwordConfigured?'已配置，留空保留原密码':'请输入发信邮箱的 SMTP 密码'"/></el-form-item>
      <el-form-item label="发件人邮箱"><el-input v-model.trim="form.fromAddress" maxlength="45"/></el-form-item>
      <el-form-item label="发件人名称"><el-input v-model.trim="form.fromName" maxlength="60"/></el-form-item>
      <el-form-item label="启用验证码发送"><el-switch v-model="form.enabled"/></el-form-item>
      <el-alert title="未启用时不能发送验证码，新用户无法注册；已有用户登录不受影响。建议先保存并发送测试邮件，确认收到后再启用。" type="info" :closable="false" show-icon/>
      <el-form-item class="actions"><el-button type="primary" :loading="saving" :disabled="testing" native-type="submit">保存配置</el-button></el-form-item>
      <el-divider/>
      <el-form-item label="测试收件邮箱"><el-input v-model.trim="testEmail" type="email" placeholder="填写你可以查收的邮箱" maxlength="45"/></el-form-item>
      <el-form-item><el-button :loading="testing" :disabled="saving" @click="test">用已保存配置发送测试邮件</el-button></el-form-item>
      <p class="mail-note">默认验证码有效期10分钟，每个邮箱60秒可重发、每小时最多5封，连续输错5次需重新获取。密码加密保存，不会回显。</p>
    </el-form>
  </basic-container>
</template>
<script setup>
import {reactive,ref,onMounted} from 'vue';
import {ElMessage} from 'element-plus';
import request from '@/axios';
const form=reactive({host:'mail.sinoaopt.com',port:587,security:'STARTTLS',username:'fmemo@sinoaopt.com',password:'',fromAddress:'fmemo@sinoaopt.com',fromName:'AI小醒',enabled:false});
const loading=ref(false),saving=ref(false),testing=ref(false),passwordConfigured=ref(false),testEmail=ref('');
const call=(action,data)=>request({url:'/api/blade-smart/registration-mail/'+action,method:'post',data,timeout:45000,meta:{silent:true,noProgress:true}});
const fail=e=>ElMessage.error(e?.response?.data?.msg||e?.message||'操作失败，请重试');
async function load(){loading.value=true;try{const {data}=await call('detail');const {passwordConfigured:configured,...settings}=data.data;Object.assign(form,settings,{password:''});passwordConfigured.value=configured;}catch(e){fail(e);}finally{loading.value=false;}}
async function save(){if(saving.value)return;saving.value=true;try{await call('save',{...form});form.password='';await load();ElMessage.success('SMTP 配置已保存');}catch(e){fail(e);}finally{saving.value=false;}}
async function test(){if(testing.value)return;if(!testEmail.value)return ElMessage.warning('请填写测试收件邮箱');testing.value=true;try{await call('test',{email:testEmail.value});ElMessage.success('测试邮件已提交，请检查收件箱或垃圾邮件');}catch(e){fail(e);}finally{testing.value=false;}}
onMounted(load);
</script>
<style scoped>
.mail-config{max-width:740px;margin-top:24px}.actions{margin-top:20px}.mail-note,p{color:#667085;line-height:1.8}.mail-note{font-size:13px}
</style>
