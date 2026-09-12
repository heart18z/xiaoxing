<template>

  <div style="display: inline-block">
    <el-button class="button-size" round title="消构建通知"  size="small"
               icon="el-icon-bell" @click.stop="buildMessage()">
    </el-button>
    <message-build ref = "messageBuild"></message-build>
  </div>
</template>

<script>
import messageBuild from "@/components/message-build/main.vue";
export default {
  name: 'MenuButton',
  components: {
    messageBuild
  },
  props: {
    selectionList: {
      type: Array,
      default: () => {
        return [];
      }
    },
  },
  methods: {
    buildMessage() {
      if (this.selectionList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      }
      this.$refs.messageBuild.openMessageBuild(this.selectionList)
    },
  },
}
</script>


