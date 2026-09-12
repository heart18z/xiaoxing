<template>
  <div>
    <basic-container>
      <avue-form
        ref="form"
        v-model="formData.notice"
        :option="option"
        @submit="handleSubmit"
        :upload-after="uploadAfter"
        @reset-change="handleReset"
      >
        <template #target="{  size  }">
          <div style="display: flex">
            <el-input
              v-model="formData.notice.target"
              :disabled="true"
              placeholder="请选择 推送对象"
              :size="size"
            >
            </el-input>
            <el-button
              @click="selectUserHandle"
              style="margin-left: 5px"
              :size="size"
              type="primary"
              >选择</el-button
            >
          </div>
        </template>
      </avue-form>
    </basic-container>
    <el-dialog
      title="选择推送对象"
      draggable
      v-model="showDialog"
      class="user-dialog avue-dialog avue-dialog--top"
      width="40%"
      :append-to-body="true"
    >
      <div class="avue-dialog-main">
        <el-card style="width: 100%">
          <el-tree
            class="filter-tree"
            :data="treeData"
            :props="defaultProps"
            ref="tree"
            @node-click="handleNodeClick"
          >
          </el-tree>
        </el-card>
        <div style="width: 12px"></div>
        <el-card style="width: 100%">
          <div style="padding-bottom: 15px">
            <el-input
              size="small"
              v-model="keyWord"
              placeholder="请输入关键词搜索"
              @keydown.native.enter="searchHandle"
            >
              <template #append>
              <el-button
                icon="el-icon-search"
                @click="searchHandle"
              ></el-button>
              </template>
            </el-input>
          </div>

          <template v-if="userData.length !== 0">
            <el-checkbox
              :indeterminate="isIndeterminate"
              v-model="checkAll"
              @change="handleCheckAllChange"
              >全选</el-checkbox
            >
            <div style="margin-bottom: 8px"></div>
            <el-checkbox-group
              v-model="checkedUsers"
              @change="handleCheckedUsersChange"
            >
              <el-checkbox
                class="user-checkbox"
                v-for="user in userData"
                :label="user.id"
                :key="user.id"
                >{{ user.realName }}</el-checkbox
              >
            </el-checkbox-group>
          </template>
          <template v-else>
            <div style="color: #606266; text-align: center; width: 100%">
              暂无数据！
            </div>
          </template>
        </el-card>
      </div>
      <div class="avue-dialog__footer">
        <el-button size="small" @click="showDialog = false">取 消</el-button>
        <el-button size="small" @click="confirmAddUser" type="primary"
          >确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

  <script>
import { remove } from "@/api/resource/oss";
import AvueUeditor from "avue-plugin-ueditor";
import {addNotice, getUserListByGroupId,
  getGroupTree,} from "@/api/desk/notice/notice"

export default {
  name: "commonMessage",
  components: {
    // FileUpload,
    AvueUeditor,
  },
  data() {
    return {
      keyWord: "",
      checkAll: false,
      checkedUsers: [],
      isIndeterminate: false,
      formData: {
        notice: {
          target: "",
        },
        ids: "",
      },
      // 树数据
      treeData: [],
      // 用户数据
      userData: [],
      showDialog: false,
      option: {
        column: [
          {
            label: "消息标题",
            prop: "noticeTitle",
            row: true,
            offset: 5,
            span: 12,
            rules: [
              { required: true, message: "请输入消息标题", trigger: "blur" },
            ],
          },
          {
            label: "推送对象",
            prop: "target",
            span: 12,
            offset: 5,
            formslot: true,
            row: true,
            rules: [
              { required: true, message: "请选择推送对象", trigger: "change" },
            ],
          },
          // {
          //   label: "发布方式",
          //   prop: "send_type",
          //   span: 12,
          //   offset:5,
          //   type: "select",
          //   row: true,
          //   dicData: [
          //     {
          //       label: "即时发布",
          //       value: "0",
          //     },
          //     {
          //       label: "定时发布",
          //       value: "1",
          //     },
          //   ],
          //   change: ({ value }) => {
          //   // this.$message.success('change事件查看控制台')
          //   console.log('值改变', value)
          //     if (value == 0) {
          //       const releaseTime = this.findObject(this.option.column, "releaseTime");
          //       releaseTime.display = false
          //     }else if(value == 1) {
          //       const releaseTime = this.findObject(this.option.column, "releaseTime");
          //       releaseTime.display = true
          //     }
          // },
          //   rules: [
          //     { required: true, message: "请选择发布类型", trigger: "change" },
          //   ],
          // },
          {
            label: "消息类型",
            prop: "category",
            type: "tree",
            span: 12,
            offset: 5,
            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary-tree?code=notice_type",
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },
            rules: [
              { required: true, message: "请选择消息类型", trigger: "change" },
            ],
            dicFormatter: (res) => {
              let data = res.data
              for (let i = 0; i < data.length; i++) {
                data[i].disabled = true
              }
              return data
            },
            row: true,
            search: true,
            searchSpan: 5,
          },

          {
            label: "推送链接",
            prop: "href",
            row: true,
            span: 12,
            offset: 5,
            // rules: [
            //   {  message: "请输入推送链接", trigger: "blur" },
            //   { validator: validateHref, trigger: "blur" },
            // ],
          },
          {
            label: "通知内容",
            prop: "noticeContent",
            component: "AvueUeditor",
            options: {
              action: "/api/blade-resource/oss/endpoint/put-file",
              props: {
                res: "data",
                url: "link",
              },
            },
            offset: 5,
          },

          {
            type: "upload",
            label: "消息附件",
            span: 12,
            offset: 5,
            display: true,
            showFileList: true,
            multiple: false,
            limit: 1,
            canvasOption: {},
            prop: "file",
            detail: false,
            action: "/api/blade-resource/oss/endpoint/put-file-attach",
            // accept: "video/mp4",
            propsHttp: {
              url: "link",
              name: "originalName",
              id: "attachId",
              res: "data",
            },
            onRemove: (file) => {
              // console.error("file", file);
              this.$confirm("此操作将永久删除该文件, 是否继续?", "提示", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning",
              }).then(() => {
                let a = file.url.split("/");
                let id = a[a.length - 1];
                return remove(id);
              });
              // .catch(() => {
              //   this.$message({
              //     type: "info",
              //     message: "已取消删除",
              //   });
              // });
            },
            tip: " *注:请注意文件命名是否规整（文件小于50M）",
          },
          // {
          //   label: "消息附件",
          //   prop: "file",
          //   span: 24,
          //   formslot: true,
          // },
        ],
      },
      defaultProps: {
        children: "children",
        label: "title",
      },
      selectRow: {},
      allTreeData: {},
      // 选择树数据
      selectTreeData: {},
    };
  },
  mounted() {
    this.initData();
  },
  methods: {
    uploadAfter(res, done) {
      this.formData.fileData = res
      done();
    },
    // 搜索用户
    searchHandle() {
      this.handleNodeClick(this.selectTreeData);
    },
    async initData() {
      const { data: { data = [], code } = {} } = await getGroupTree();
      // 设置全部
      const selectAllObj = {
        id: "",
        hasChildren: false,
        title: "全部",
      };
      this.treeData = code === 200 ? [selectAllObj, ...data] : [];
    },
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    // 选择对象
    selectUserHandle() {
      this.showDialog = true;
    },
    // 节点选择
    async handleNodeClick(row) {
      const { title = "" } = row;
      this.selectTreeData = row;
      const { data: { data = [], code } = {} } = await getUserListByGroupId(
        row.id,
        this.keyWord
      );
      this.userData = code === 200 ? data : [];
      // 保存到对象里
      if (!this.keyWord) {
        this.allTreeData[title] = data;
      }
    },
    // 全选
    handleCheckAllChange(val) {
      this.checkedUsers = val ? this.userData.map((v) => v.id) : [];
      this.isIndeterminate = false;
    },
    // 选择
    handleCheckedUsersChange(value) {
      let checkedCount = value.length;
      this.checkAll = checkedCount === this.userData.length;
      this.isIndeterminate =
        checkedCount > 0 && checkedCount < this.userData.length;
    },
    // 确定
    confirmAddUser() {
      // 获取所有数据
      // 二维数据
      const allArrs = Object.values(this.allTreeData) || [];
      // 转为一维数组
      const formatAllArrs = allArrs.flat();
      // 获取名字集合
      const recipientNameArr = formatAllArrs
       .filter((v) => this.checkedUsers.includes(v.id))
        .map((v) => v.realName);
      // 名字去重
      const newSetRecipientNameArr = [...new Set(recipientNameArr)];
      const { notice = {} } = this.formData;
      this.formData = {
        notice: {
          ...notice,
           target: newSetRecipientNameArr.join(","),
        },
        ids: this.checkedUsers,
      };
      this.showDialog = false;
    },
    // 提交数据
    async handleSubmit(form, done) {
      const { ...other } = form;
      const { ids = "",fileData= {} } = this.formData;
      const attachSourceList = [{
        id: fileData.attachId,
        diyFileName: fileData.originalName
      }]
      // 定义对象
      const addObj = {
        notice: {
          ...other
        },
        ids,
        attachSourceList
      };
      const {
        data: { code, msg },
      } = await addNotice(addObj);

      if (code === 200) {
        this.$refs.form.resetForm();
        this.$message.success(msg);
      } else {
        this.$message.error(msg);
      }
      done();
    },
    handleReset() {
      this.checkedUsers = [];
      this.checkAll = false;
      this.isIndeterminate = false;
    },
  },
};
</script>

  <style lang="scss" scoped>
.avue-dialog-main {
  display: flex;
}

.user-checkbox.el-checkbox {
  margin-right: 0px;
  width: 50%;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.user-dialog {
  max-height: 800px !important;
}

:deep(.w-e-text-container) {
  height: calc(50vh - 300px) !important;
}
</style>
