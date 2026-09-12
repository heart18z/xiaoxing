<template>
  <div ref="root">
    <div ref="footer" class="footer-left-bottom">
      <el-button class="button-size" round size="small" title="查看" :icon="QuestionFilled" @click="helpAction" />
      <el-button
        class="button-size"
        round
        size="small"
        title="上传"
        :icon="Upload"
        v-if="userInfo.role_name.includes('admin')"
        @click="uploadAction"
      />
    </div>
    <attach-dialog ref="attach" :source-id="menuId" :just-read="justRead" :introduce-oss="true" />
  </div>
</template>

<script>
import { QuestionFilled, Upload } from '@element-plus/icons-vue';
import { getMenuByPath } from '@/api/components-common/api';
import { mapGetters } from 'vuex';
import attachDialog from '@/components/attach-dialog/main.vue';

const FOOTER_GAP = 2;

export default {
  name: 'PageAttach',
  components: {
    attachDialog,
  },
  setup() {
    return {
      QuestionFilled,
      Upload,
    };
  },
  data() {
    return {
      menuId: '',
      justRead: false,
      resizeObserver: null,
      mutationObserver: null,
      positionRaf: null,
    };
  },
  mounted() {
    this.initMenuId();
    this.$nextTick(() => {
      this.bindPositionObservers();
      this.updateFooterPosition();
    });
  },
  activated() {
    this.$nextTick(() => {
      this.updateFooterPosition();
    });
  },
  beforeUnmount() {
    this.unbindPositionObservers();
  },
  computed: {
    ...mapGetters(['permission', 'userInfo']),
  },
  methods: {
    initMenuId() {
      this.menuId = this.$route.query.menuId;
      if (this.menuId == null || this.menuId === undefined) {
        const param = {
          pagePath: this.$route.path,
        };
        getMenuByPath(param).then(res => {
          if (res.data.code === 200) {
            this.menuId = res.data.data.id;
          }
        });
      }
    },
    getPositionContext() {
      const card = this.$el?.closest('.basic-container__card');
      if (!card) {
        return null;
      }
      const crud = this.getMainCrud(card);
      const table = crud?.querySelector('.el-table');
      if (!table) {
        return null;
      }
      return { card, table };
    },
    getMainCrud(card) {
      const cruds = card.querySelectorAll('.avue-crud');
      for (const crud of cruds) {
        if (!crud.closest('.el-dialog, .el-drawer')) {
          return crud;
        }
      }
      return cruds[0] || null;
    },
    updateFooterPosition() {
      const footer = this.$refs.footer;
      if (!footer) {
        return;
      }
      const context = this.getPositionContext();
      if (!context) {
        footer.style.visibility = 'hidden';
        return;
      }
      const { card, table } = context;
      const cardRect = card.getBoundingClientRect();
      const tableRect = table.getBoundingClientRect();
      footer.style.visibility = 'visible';
      footer.style.left = `${tableRect.left - cardRect.left}px`;
      footer.style.top = `${tableRect.bottom - cardRect.top + FOOTER_GAP}px`;
    },
    scheduleFooterPosition() {
      if (this.positionRaf) {
        cancelAnimationFrame(this.positionRaf);
      }
      this.positionRaf = requestAnimationFrame(() => {
        this.positionRaf = null;
        this.updateFooterPosition();
      });
    },
    bindPositionObservers() {
      this.unbindPositionObservers();
      window.addEventListener('resize', this.scheduleFooterPosition);

      const card = this.$el?.closest('.basic-container__card');
      if (!card) {
        return;
      }

      if (typeof ResizeObserver !== 'undefined') {
        this.resizeObserver = new ResizeObserver(this.scheduleFooterPosition);
        this.resizeObserver.observe(card);
        const context = this.getPositionContext();
        if (context?.table) {
          this.resizeObserver.observe(context.table);
        }
      }

      if (typeof MutationObserver !== 'undefined') {
        this.mutationObserver = new MutationObserver(() => {
          this.scheduleFooterPosition();
          const context = this.getPositionContext();
          if (context?.table && this.resizeObserver) {
            this.resizeObserver.observe(context.table);
          }
        });
        this.mutationObserver.observe(card, {
          childList: true,
          subtree: true,
          attributes: true,
        });
      }
    },
    unbindPositionObservers() {
      window.removeEventListener('resize', this.scheduleFooterPosition);
      if (this.positionRaf) {
        cancelAnimationFrame(this.positionRaf);
        this.positionRaf = null;
      }
      if (this.resizeObserver) {
        this.resizeObserver.disconnect();
        this.resizeObserver = null;
      }
      if (this.mutationObserver) {
        this.mutationObserver.disconnect();
        this.mutationObserver = null;
      }
    },
    uploadAction() {
      this.justRead = false;
      this.showAttachDialog();
    },
    helpAction() {
      this.justRead = true;
      this.showAttachDialog();
    },
    showAttachDialog() {
      this.$refs.attach.showAttachDialog();
    },
  },
};
</script>
