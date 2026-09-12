<template>
  <el-tooltip placement="top" :append-to-body="true">
    <template #content>
      <div>{{ content }}</div>
    </template>
    <el-icon class="note-icon">
      <component :is="iconComponent" />
    </el-icon>
  </el-tooltip>
</template>

<script>
import { WarningFilled, QuestionFilled } from '@element-plus/icons-vue';

const ICON_MAP = {
  'el-icon-warning': WarningFilled,
  'el-icon-question': QuestionFilled,
};

export default {
  name: 'ZdyPopover',
  props: {
    icon: {
      type: String,
      default: 'el-icon-warning',
    },
    content: {
      type: String,
      default: '',
    },
  },
  computed: {
    iconComponent() {
      return ICON_MAP[this.icon] || WarningFilled;
    },
  },
};
</script>

<style lang="scss" scoped>
.note-icon {
  margin-left: 4px;
  color: #e6a23c;
  font-size: 14px;
  vertical-align: middle;
  cursor: pointer;
}
</style>
