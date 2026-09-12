<template>
  <el-form
    class="login-form"
    status-icon
    :rules="loginRules"
    ref="loginForm"
    :model="loginForm"
    label-width="0"
  >
    <el-form-item prop="username">
      <el-input
        size="small"
        @keyup.enter="handleLogin"
        v-model="loginForm.username"
        autocomplete="off"
        :placeholder="$t('login.username')"
      >
        <template #prefix>
          <i class="icon-yonghu" />
        </template>
      </el-input>
    </el-form-item>
    <el-form-item prop="password">
      <el-input
        size="small"
        @keyup.enter="handleLogin"
        type="password"
        show-password
        v-model="loginForm.password"
        autocomplete="new-password"
        :placeholder="$t('login.password')"
      >
        <template #prefix>
          <i class="icon-mima" />
        </template>
      </el-input>
    </el-form-item>
    <el-form-item v-if="captchaMode === 'true'" prop="code">
      <el-row :span="24">
        <el-col :span="16">
          <el-input
            size="small"
            @keyup.enter="handleLogin"
            v-model="loginForm.code"
            autocomplete="off"
            :placeholder="$t('login.code')"
          >
            <template #prefix>
              <i class="icon-yanzhengma" />
            </template>
          </el-input>
        </el-col>
        <el-col :span="8">
          <div class="login-code">
            <img :src="loginForm.image" class="login-code-img" @click="refreshCode" />
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <el-checkbox v-if="showRememberMe === 'true'" v-model="loginForm.rememberMe">记住密码</el-checkbox>
    <el-form-item>
      <el-button type="primary" size="small" @click.prevent="handleLogin" class="login-submit">
        {{ $t('login.submit') }}
      </el-button>
    </el-form-item>
    <el-dialog title="用户信息选择" append-to-body v-model="userBox" width="350px">
      <avue-form :option="userOption" v-model="userForm" @submit="submitLogin" />
    </el-dialog>
  </el-form>
</template>

<script>
import { mapGetters } from 'vuex';
import { getCaptcha } from '@/api/user';
import website from '@/config/website';
import crypto from '@/utils/crypto';
import Cookies from 'js-cookie';

export default {
  name: 'userlogin',
  data() {
    const validatePass = (rule, value, callback) => {
      const ISPWD = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[\W_]).{8,20}$/;
      if (value === '') {
        callback(new Error('请输入密码'));
      } else if (ISPWD.test(value)) {
        callback();
      } else {
        callback(new Error('密码必须8-20位字符,同时含有字母、特殊字符和数字组合'));
      }
    };
    return {
      loginForm: {
        rememberMe: false,
        tenantId: '000000',
        deptId: '',
        roleId: '',
        username: '',
        password: '',
        type: 'account',
        code: '',
        key: '',
        image: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7',
      },
      loginRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ validator: validatePass, trigger: 'blur' }],
      },
      userBox: false,
      userForm: {
        deptId: '',
        roleId: '',
      },
      userOption: {
        labelWidth: 70,
        submitBtn: true,
        emptyBtn: false,
        submitText: '登录',
        column: [
          {
            label: '部门',
            prop: 'deptId',
            type: 'select',
            props: {
              label: 'deptName',
              value: 'id',
            },
            dicMethod: 'post',
            dicUrl: '/api/blade-system/dept/select',
            span: 24,
            display: false,
            rules: [
              {
                required: true,
                message: '请选择部门',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '角色',
            prop: 'roleId',
            type: 'select',
            props: {
              label: 'roleName',
              value: 'id',
            },
            dicMethod: 'post',
            dicUrl: '/api/blade-system/role/select',
            span: 24,
            display: false,
            rules: [
              {
                required: true,
                message: '请选择角色',
                trigger: 'blur',
              },
            ],
          },
        ],
      },
    };
  },
  created() {
    this.getTenant();
    this.refreshCode();
    this.getCookie();
  },
  mounted() {
    if (this.adminPasswd === 'true') {
      this.$nextTick(() => {
        this.fillAdminPasswd();
      });
    }
  },
  watch: {
    captchaMode() {
      if (this.captchaMode === 'true') {
        this.refreshCode();
      }
    },
    adminPasswd() {
      if (this.adminPasswd === 'true') {
        this.fillAdminPasswd();
      }
    },
  },
  computed: {
    ...mapGetters(['tagWel', 'userInfo', 'website']),
    captchaMode() {
      return this.$store.getters.systemParam['chekcode'];
    },
    adminPasswd() {
      return this.$store.getters.systemParam['admin.passwd'];
    },
    showRememberMe() {
      return this.$store.getters.systemParam['showRememberMe'];
    },
  },
  methods: {
    getCookie() {
      const username = Cookies.get('username');
      const password = Cookies.get('password');
      const rememberMe = Cookies.get('rememberMe');
      this.loginForm.username = username === undefined ? this.loginForm.username : username;
      this.loginForm.password = password === undefined ? this.loginForm.password : crypto.decrypt(password);
      this.loginForm.rememberMe = rememberMe === undefined ? false : Boolean(rememberMe);
    },
    refreshCode() {
      if (this.captchaMode === 'true') {
        getCaptcha().then(res => {
          const data = res.data.data;
          this.loginForm.key = data.key;
          this.loginForm.image = data.image;
        });
      }
    },
    fillAdminPasswd() {
      if (this.loginForm.rememberMe) {
        return;
      }
      const adminPassword = this.$store.getters.systemParam['adminer.adm.produ'] || 'Dfyj123';
      const password = adminPassword.split(',');
      let strPassword = 'Dfyj123';
      if (password.length > 0) {
        strPassword = password[0];
      }
      this.$nextTick(() => {
        this.loginForm.password = strPassword;
        this.loginForm.username = 'adminer';
      });
    },
    submitLogin(form, done) {
      if (form.deptId !== '') {
        this.loginForm.deptId = form.deptId;
      }
      if (form.roleId !== '') {
        this.loginForm.roleId = form.roleId;
      }
      this.handleLogin();
      done();
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          const loading = this.$loading({
            lock: true,
            text: '登录中,请稍后。。。',
            background: 'rgba(0, 0, 0, 0.7)',
          });
          if (this.loginForm.rememberMe) {
            Cookies.set('username', this.loginForm.username, { expires: 30 });
            Cookies.set('password', crypto.encrypt(this.loginForm.password), { expires: 30 });
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 });
          } else {
            Cookies.remove('username');
            Cookies.remove('password');
            Cookies.remove('rememberMe');
          }
          this.$store
            .dispatch('LoginByUsername', this.loginForm)
            .then(() => {
              if (this.website.switchMode) {
                const deptId = this.userInfo.dept_id;
                const roleId = this.userInfo.role_id;
                if (deptId.includes(',') || roleId.includes(',')) {
                  this.loginForm.deptId = deptId;
                  this.loginForm.roleId = roleId;
                  this.userBox = true;
                  this.$store.dispatch('LogOut').then(() => {
                    loading.close();
                  });
                  return false;
                }
              }
              this.$router.push(this.tagWel.path || this.tagWel);
              this.refreshCode();
              loading.close();
            })
            .catch(() => {
              loading.close();
              this.refreshCode();
            });
        }
      });
    },
    getTenant() {
      this.loginForm.tenantId = website.tenantId;
    },
  },
};
</script>

<style></style>
