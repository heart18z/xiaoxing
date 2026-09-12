<template>
  <el-form
    class="login-form"
    status-icon
    :rules="loginRules"
    ref="loginForm"
    :model="loginForm"
    label-width="0"
  >
    <el-form-item prop="invitationCode">
      <el-input
        size="small"
        @keyup.enter="handleLogin"
        v-model="loginForm.invitationCode"
        autocomplete="off"
        placeholder="请输入邀请码"
      >
        <template #prefix>
          <i class="icon-yonghu" />
        </template>
      </el-input>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" size="small" @click.prevent="handleLogin" class="login-submit">
        {{ $t('login.submit') }}
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { mapGetters } from 'vuex';

export default {
  name: 'invitationlogin',
  data() {
    return {
      loginForm: {
        invitationCode: '',
      },
      loginRules: {
        invitationCode: [{ required: true, message: '请输入邀请码', trigger: 'blur' }],
      },
    };
  },
  computed: {
    ...mapGetters(['tagWel']),
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          const loading = this.$loading({
            lock: true,
            text: '登录中,请稍后。。。',
            background: 'rgba(0, 0, 0, 0.7)',
          });
          this.$store
            .dispatch('LoginByInvitationCode', this.loginForm.invitationCode)
            .then(() => {
              this.$router.push(this.tagWel.path || this.tagWel);
              loading.close();
            })
            .catch(() => {
              loading.close();
            });
        }
      });
    },
  },
};
</script>
