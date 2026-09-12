<template>
  <div class="row-attach">
    <el-button class="button-size" circle size="small" title="附件" :icon="DocumentCopy" @click="showAttachDialog()"></el-button>
    <!-- 查看和附按钮 -->
    <attach-dialog ref="attach" :source-id="sourceId" :just-read="justRead"></attach-dialog>
  </div>
</template>

<script>

import { DocumentCopy } from '@element-plus/icons-vue';
import attachDialog from "@/components/attach-dialog/main.vue"
export default {
  name: 'RowAttach',
  components: {
    attachDialog
  },
  setup() {
    return {
      DocumentCopy,
    };
  },
  props: {
    sourceId: {
      type: String,
      default: ""
    },
    justRead: {
      type: Boolean,
      default: false
    },
  },
  methods: {
    // 展示附件列表
    showAttachDialog() {
      this.$refs.attach.showAttachDialog()
    }

  }
}
</script>

<style scoped>
.row-attach {
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
