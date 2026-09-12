<template>
  <div class="login-container" ref="login" @keyup.enter="handleLogin">
    <top-color v-show="false"></top-color>
    <div class="login-weaper animate__animated animate__bounceInDown">
      <div class="login-left">
        <p class="title">{{ systemTitle }}</p>
      </div>
      <div class="login-border">
        <div class="login-main">
          <h4 class="login-title">
            {{ $t('login.title') }}{{ saber }}
            <top-lang></top-lang>
          </h4>
          <userLogin v-if="activeName === 'user'"></userLogin>
          <invitationlogin v-else-if="activeName === 'invitation'"></invitationlogin>
          <div class="login-menu">
            <a href="#" @click.prevent="activeName = 'user'">{{ $t('login.userLogin') }}</a>
            <a v-if="keycloakEnabled" @click="loginByKeycloak()">{{ $t('login.ssoLogin') }}</a>
            <a v-else-if="oauth2 !== 'unset' && oauth2 !== ''" @click="checkOauth2Setting()">{{
              $t('login.ssoLogin')
            }}</a>
            <a href="#" v-if="invitation === 'true'" @click.prevent="activeName = 'invitation'">邀请码登录</a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import userLogin from './userlogin.vue';
import invitationlogin from './invitationlogin.vue';
import { mapGetters } from 'vuex';
import { validatenull } from '@/utils/validate';
import topLang from '@/page/index/top/top-lang.vue';
import topColor from '@/page/index/top/top-color.vue';
import { getQueryString, getTopUrl } from '@/utils/util';
import { getLoginPageParam } from '@/api/system/param';
import func from '@/utils/func';
import { getKeycloakEnabled, getKeycloakLoginUrl } from '@/api/user';
import { loginUrl } from '@/api/system/user';
import website from '@/config/website';

export default {
  name: 'login',
  components: {
    userLogin,
    invitationlogin,
    topLang,
    topColor,
  },
  data() {
    return {
      activeName: 'user',
      socialForm: {
        tenantId: '000000',
        source: '',
        code: '',
        state: '',
      },
      systemTitle: '',
      systemId: '',
      saber: '',
      userCode: '',
      code: '',
      localUrl: '',
      oauth2: '',
      invitation: '',
      keycloakEnabled: false,
    };
  },
  watch: {
    $route() {
      this.handleLogin();
    },
  },
  created() {
    this.getLoginParam();
    this.checkKeycloakEnabled().then(() => {
      this.handleLogin();
    });
  },
  mounted() {
    this.getParams();
    if (func.notEmpty(this.userCode) && func.notEmpty(this.code)) {
      this.loginByCode();
    }
  },
  computed: {
    ...mapGetters(['website', 'tagWel']),
  },
  methods: {
    checkKeycloakEnabled() {
      return getKeycloakEnabled()
        .then(res => {
          this.keycloakEnabled = res.data.data === true;
        })
        .catch(() => {
          this.keycloakEnabled = false;
        });
    },
    loginByKeycloak() {
      getKeycloakLoginUrl(website.tenantId).then(res => {
        const url = res.data.data;
        if (func.isEmpty(url)) {
          this.$message.warning('Keycloak 登录地址未配置！');
        } else {
          window.location.href = url;
        }
      });
    },
    checkOauth2Setting() {
      if (this.oauth2 === 'disable') {
        this.$message.warning('单点登录尚未启用');
      } else {
        loginUrl().then(res => {
          const url = res.data;
          if (func.isEmpty(url)) {
            this.$message.warning('单点登录地址未配置！');
          } else {
            window.location.href = url + '?system_id=' + this.systemId + '&redirectUrl=' + this.localUrl;
          }
        });
      }
    },
    getParams() {
      this.localUrl = window.location.href.replace('#', '%23');
      this.userCode = this.$route.query.userCode || '';
      this.code = this.$route.query.code || '';
    },
    loginByCode() {
      const params = {
        userCode: this.userCode,
        code: this.code,
      };
      const loading = this.$loading({
        lock: true,
        text: '登录中,请稍后。。。',
        background: 'rgba(0, 0, 0, 0.7)',
      });
      this.$store
        .dispatch('LoginByCode', params)
        .then(() => {
          this.$router.push(this.tagWel.path || this.tagWel);
          loading.close();
          this.getParams();
        })
        .catch(() => {
          this.code = '';
          this.userCode = '';
          this.$router.push({ query: {} });
          this.localUrl = window.location.href.replace('#', '%23');
          loading.close();
        });
    },
    getLoginParam() {
      getLoginPageParam().then(res => {
        const data = res.data.data;
        const title = func.notEmpty(data['project.name']) ? data['project.name'] : '';
        this.$router.$avueRouter.setTitle(title);
        this.systemTitle = title;
        this.saber = title.split('_')[0];
        this.systemId = func.notEmpty(data['system.id']) ? data['system.id'] : '';
        this.oauth2 = func.trimAll(func.notEmpty(data['oauth2']) ? data['oauth2'] : '');
        this.invitation = func.notEmpty(data['invitation.code']) ? data['invitation.code'] : '';
        this.$store.commit('SET_SYSTEM_PARAM', data);
      });
    },
    handleLogin() {
      const topUrl = getTopUrl();
      const redirectUrl = '/oauth/redirect/';
      const ssoCode = '?code=';
      this.socialForm.source = getQueryString('source');
      this.socialForm.code = getQueryString('code');
      this.socialForm.state = getQueryString('state');
      if (validatenull(this.socialForm.source) && topUrl.includes(redirectUrl)) {
        let source = topUrl.split('?')[0];
        source = source.split(redirectUrl)[1];
        this.socialForm.source = source;
      }
      if (
        topUrl.includes(redirectUrl) &&
        !validatenull(this.socialForm.source) &&
        !validatenull(this.socialForm.code) &&
        !validatenull(this.socialForm.state)
      ) {
        const loading = this.$loading({
          lock: true,
          text: '第三方系统登录中,请稍后。。。',
          background: 'rgba(0, 0, 0, 0.7)',
        });
        this.$store
          .dispatch('LoginBySocial', this.socialForm)
          .then(() => {
            window.location.href = topUrl.split(redirectUrl)[0];
            this.$router.push(this.tagWel.path || this.tagWel);
            loading.close();
          })
          .catch(() => {
            loading.close();
          });
      } else if (
        !topUrl.includes(redirectUrl) &&
        !validatenull(this.socialForm.code) &&
        !validatenull(this.socialForm.state)
      ) {
        const isKeycloakCallback = window.location.pathname.includes('/oauth/keycloak/callback');
        const loginAction = this.keycloakEnabled || isKeycloakCallback ? 'LoginByKeycloak' : 'LoginBySso';
        const loading = this.$loading({
          lock: true,
          text: '单点系统登录中,请稍后。。。',
          background: 'rgba(0, 0, 0, 0.7)',
        });
        this.$store
          .dispatch(loginAction, this.socialForm)
          .then(() => {
            window.location.href = topUrl.split(ssoCode)[0];
            this.$router.push(this.tagWel.path || this.tagWel);
            loading.close();
          })
          .catch(() => {
            loading.close();
          });
      }
    },
  },
};
</script>

<style lang="scss">
@import '@/styles/login.scss';
</style>
