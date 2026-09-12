<template>
  <div class="avue-logo">
    <transition name="fade">
      <span v-if="getScreen(isCollapse)" class="avue-logo_subtitle" key="0">
        <img class="logo-img" src="/img/logo.png" />
      </span>
    </transition>
    <transition-group name="fade">
      <template v-if="getScreen(!isCollapse)">
        <span class="avue-logo_title" key="1" :title="indexTitle">{{ indexTitle }}</span>
      </template>
    </transition-group>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import func from '@/utils/func';

export default {
  name: 'logo',
  computed: {
    ...mapGetters(['isCollapse', 'systemParam']),
    indexTitle() {
      const name = this.systemParam['project.name'];
      return func.notEmpty(name) ? name : '';
    },
  },
};
</script>

<style scoped>
.avue-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: normal;
  padding: 0 8px;
  box-sizing: border-box;
}

.avue-logo_title {
  display: block;
  width: 100%;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
  text-align: center;
  white-space: normal;
  word-break: break-all;
}

.logo-img {
  width: 40px;
  margin-top: 5px;
}
</style>
