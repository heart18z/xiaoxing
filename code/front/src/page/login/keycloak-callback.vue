<template>
  <div class="keycloak-callback"></div>
</template>

<script>
import func from '@/utils/func';

export default {
  name: 'keycloakCallback',
  created() {
    const searchParams = new URLSearchParams(window.location.search);
    const code = this.$route.query.code || searchParams.get('code');
    const state = this.$route.query.state || searchParams.get('state');
    if (func.isEmpty(code)) {
      const error =
        this.$route.query.error_description ||
        this.$route.query.error ||
        searchParams.get('error_description') ||
        searchParams.get('error');
      this.$message.error(error || 'Keycloak 授权失败：未获取到授权码');
      this.$router.replace({ path: '/login' });
      return;
    }
    const loading = this.$loading({
      lock: true,
      text: 'Keycloak 登录中，请稍后...',
      background: 'rgba(0, 0, 0, 0.7)',
    });
    this.$store
      .dispatch('LoginByKeycloak', { code, state })
      .then(() => {
        loading.close();
        const tagWel = this.$store.getters.tagWel;
        this.$router.replace({ path: tagWel.path || tagWel });
      })
      .catch(() => {
        loading.close();
        this.$router.replace({ path: '/login' });
      });
  },
};
</script>
