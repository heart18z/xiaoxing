<template>
  <el-form
    class="login-form"
    status-icon
    :rules="loginRules"
    ref="loginForm"
    :model="loginForm"
    label-width="0"
  >
    <el-form-item prop="phone">
      <el-input
        @keyup.enter="handleLogin"
        v-model="loginForm.phone"
        auto-complete="off"
        :placeholder="$t('login.phone')"
      >
        <template #prefix>
          <i class="icon-shouji" />
        </template>
      </el-input>
    </el-form-item>
    <el-form-item prop="code">
      <el-input
        @keyup.enter="handleLogin"
        v-model="loginForm.codeValue"
        auto-complete="off"
        :placeholder="$t('login.code')"
      >
        <template #prefix>
          <i class="icon-yanzhengma"></i>
        </template>
        <template #append>
          <span @click="handleSend" class="msg-text" :class="[{ display: msgKey }]">{{
            msgText
          }}</span>
        </template>
      </el-input>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click.prevent="handleLogin" class="login-submit">{{
        $t('login.submit')
      }}</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { isvalidatemobile } from '@/utils/validate';
import { mapGetters } from 'vuex';
import { sendSms } from '@/api/user';
import { encrypt } from '@/utils/sm2';

export default {
  name: 'codelogin',
  data() {
    const validatePhone = (rule, value, callback) => {
      if (isvalidatemobile(value)[0]) {
        callback(new Error(isvalidatemobile(value)[1]));
      } else {
        callback();
      }
    };
    return {
      msgText: '',
      msgTime: '',
      msgKey: false,
      loginForm: {
        tenantId: '000000',
        phone: '',
        codeValue: '',
        codeId: '',
      },
      loginRules: {
        phone: [{ required: true, trigger: 'blur', validator: validatePhone }],
        codeValue: [{ required: true, trigger: 'blur' }],
      },
    };
  },
  created() {
    this.getMsg();
  },
  mounted() {},
  computed: {
    ...mapGetters(['tagWel']),
    config() {
      return {
        MSGINIT: this.$t('login.msgText'),
        MSGSCUCCESS: this.$t('login.msgSuccess'),
        MSGTIME: 60,
      };
    },
  },
  props: [],
  methods: {
    handleSend() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          if (this.msgKey) return;
          this.msgText = this.msgTime + this.config.MSGSCUCCESS;
          this.msgKey = true;
          const time = setInterval(() => {
            this.msgTime--;
            this.msgText = this.msgTime + this.config.MSGSCUCCESS;
            if (this.msgTime === 0) {
              this.msgTime = this.config.MSGTIME;
              this.msgText = this.config.MSGINIT;
              this.msgKey = false;
              clearInterval(time);
            }
          }, 1000);
          sendSms(this.loginForm.tenantId, encrypt(this.loginForm.phone)).then(res => {
            const data = res.data;
            if (data.success) {
              this.loginForm.codeId = data.data.id;
              this.$message.success(data.msg);
            } else {
              this.$message.error(data.msg);
            }
          });
        }
      });
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          const loading = this.$loading({
            lock: true,
            text: '登录中,请稍后',
            background: 'rgba(0, 0, 0, 0.7)',
          });
          this.$store
            .dispatch('LoginByPhone', this.loginForm)
            .then(() => {
              loading.close();
              this.$router.push(this.tagWel);
            })
            .catch(err => {
              console.log(err);
              loading.close();
            });
        }
      });
    },
    getMsg() {
      this.msgText = this.config.MSGINIT;
      this.msgTime = this.config.MSGTIME;
    },
  },
};
</script>

<style>
.msg-text {
  display: block;
  width: 60px;
  font-size: 12px;
  text-align: center;
  cursor: pointer;
}

.msg-text.display {
  color: #ccc;
}
</style>
