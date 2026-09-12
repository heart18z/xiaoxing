<template>
  <el-dialog :title="'调用通知组件'"
             v-model="openDialog" draggable
             :append-to-body="true" :modal='false' width="70%"
             >
    <basic-container>
      <el-tabs v-model="activeName"  @tab-click="tabsChange">
        <el-tab-pane label="构建通知" name="first">
          <avue-form
              style="width: 80%"
              ref="form"
              v-model="formData.notice"
              :option="option"
              @submit="handleSubmit"
              :upload-after="uploadAfter"
              @reset-change="handleReset">
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

            <template #uploadFile="{  size  }">
              <el-button   class="el-icon-upload" @click="openAttach"></el-button>
            </template>
          </avue-form>
        </el-tab-pane>
        <el-tab-pane label="通知记录" name="second">
          <message-list ref="messageListRef" :source-ids ="sourceIds"></message-list>
        </el-tab-pane>
      </el-tabs>

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
                  :label="user.account"
                  :key="user.account"
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
    <message-attach ref="messageAttach"></message-attach>
  </el-dialog>
</template>

<script>
// import { remove } from "@/api/resource/oss";
import AvueUeditor from "avue-plugin-ueditor";
import messageList from "@/components/message-build/message-list/main.vue";
import {
  //addNotice,
  getUserListByGroupId,
  getGroupTree,} from "@/api/desk/notice/notice"
import { submit } from "@/api/desk/unifiedMessagingSend"
import {mapGetters} from "vuex";
import messageAttach from "@/components/attach-dialog/message-attach/main.vue";
import func from "@/utils/func";
export default {
  name: "commonMessage",
  components: {
    // FileUpload,
    AvueUeditor,messageAttach,messageList
  },
  computed: {
    ...mapGetters(['userInfo']),
    // 'formData.notice.fileNames'() {
    //   console.log("this.fileList", this.fileList)
    //   let names = [];
    //   this.fileList.forEach(ele => {
    //     names.push(ele.diyFileName);
    //   });
    //   return names.join(",");
    // }
    sourceIds() {
      return this.sourceData.map(item => item.id)
    }
  },
  watch: {
    "fileList": {
      handler: function() {
        this.formData.notice.fileNames = this.fileList.map(item => item.diyFileName).join(",")
      },
      deep: true
    }
  },
  data() {
    return {
      activeName: "first",
      sourceData: [],
      fileList:[],
      openDialog: false,
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
        labelWidth: 120,
        column: [
          {
            label: "消息标题",
            prop: "noticeTitle",
            rules: [
              { required: true, message: "请输入消息标题", trigger: "blur" },
            ],
          },
          {
            label: "发送频率",
            prop: "sendRate",
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=notice_frequency",
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },
            rules: [
              { required: true, message: "发送频率", trigger: "change" },
            ],

            change: ()=>{},
          },
          {
            label: "首次发送时间",
            prop: "sentTime",
            type: "datetime",
            format: "yyyy-MM-dd hh:mm:ss",
            valueFormat: "yyyy-MM-dd hh:mm:ss",
            rules: [
              { required: true, message: "首次发送时间", trigger: "change" },
            ],
          },
          {
            label: "截至发送时间",
            prop: "endSentTime",
            format: "yyyy-MM-dd hh:mm:ss",
            valueFormat: "yyyy-MM-dd hh:mm:ss",

            type: "datetime",
            rules: [
              { required: true, message: "截至发送时间", trigger: "change" },
            ],
          },
          {
            label: "推送对象",
            prop: "target",
            // span: 16,
            // offset: 5,
            formslot: true,
            rules: [
              { required: true, message: "请选择推送对象", trigger: "change" },
            ],
          },
          {
            label: "发送人",
            prop: "sendUserName",
            disabled: true,
          },
          {
            label: "消息类型",
            prop: "sendType",
            type: "tree",
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
          // {
          //   label: "推送链接",
          //   prop: "link",
          //   // disabled: true,
          //   row:true,
          //   span:24,
          //   rules: [
          //     { required: true, message: "请输入推送链接", trigger: "blur" },
          //   ],
          // },
          // {
          //   label: "推送链接",
          //   prop: "href",
          //   row: true,
          //   span: 24,
          //   // rules: [
          //   //   {  message: "请输入推送链接", trigger: "blur" },
          //   //   { validator: validateHref, trigger: "blur" },
          //   // ],
          // },
          {
            label: "通知内容",
            prop: "noticeContent",
            component: "AvueUeditor",
            span: 24,
            options: {
              action: "/api/blade-resource/oss/endpoint/put-file",
              props: {
                res: "data",
                url: "link",
              },
            },

          },

          {
            type: "input",
            label: "消息附件",
            prop: "uploadFile",
            slot:true,
            span:4,
            // pull: 0.7,
          },
          {
            label: "",
            prop: "fileNames",
            span: 20,
            push: 0.3,
            // pull: 0.7,
            labelWidth: 0,
            disabled: true,
            placeholder:" "
          },
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
    const sendRateColumn = this.findObject(this.option.column, "sendRate");
    sendRateColumn.change = this.sendRateChange
  },
  methods: {
    tabsChange(tab) {
      if (tab.name === "second") {
        this.$refs.messageListRef.searchReset()
      }
    },
    sendRateChange ({value})  {
      const sentTimeColumn = this.findObject(this.option.column, "sentTime");
      const endSentTimeColumn = this.findObject(this.option.column, "endSentTime");
      if (value === "messfreq_01") {
        sentTimeColumn.display = false
        endSentTimeColumn.display = false
      } else {
        sentTimeColumn.display = true
        endSentTimeColumn.display = true
      }
    },
    openAttach() {
      let ids = []
      this.sourceData.forEach(item => {
        ids.push(item.id)
      })
      this.$refs.messageAttach.showAttachDialog(this.fileList,ids)
    },
    openMessageBuild(data) {
      // console.log("data....", data);
     // this.handleReset()
      this.activeName= "first";
      this.sourceData = data;
      this.openDialog = true;
      this.$nextTick(()=>{
        this.$refs.form.resetForm();
      })

      let ids = []
      this.sourceData.forEach(item => {
        ids.push(item.id)
      })
      // let url = window.location.href
      // if (url) {
      //   this.formData.notice.link = url.split("?")[0]+"?ids="+ids.join(",")
      // }


    },
    uploadAfter(res, done) {
      this.formData.fileData = res
      done();
    },
    // 搜索用户
    searchHandle() {
      this.handleNodeClick(this.selectTreeData);
    },
    // async initData() {
    //   const { data: { data = [], code } = {} } = await getGroupTree();
    //   // 设置全部
    //   const selectAllObj = {
    //     id: "",
    //     hasChildren: false,
    //     title: "全部",
    //   };
    //   this.treeData = code === 200 ? [selectAllObj, ...data] : [];
    //   //设置用户
    //   // console.log(this.userInfo)
    //   this.formData.notice.sendUserName = this.userInfo.real_name
    //
    // },
    async initData() {
      const { data: { data = [], code } = {} } = await getGroupTree();
      // 设置全部
      const selectAllObj = {
        account: "",
        hasChildren: false,
        title: "全部",
      };
      this.treeData = code === 200 ? [selectAllObj, ...data] : [];
      this.formData.notice.sendUserName = this.userInfo.real_name
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
      // console.log("val...>",val,this.userData)
      this.checkedUsers = val ? this.userData.map((v) => v.account) : [];
      // console.log("val...>",val,this.userData,this.checkedUsers)
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
    // 确定
    confirmAddUser() {
      // 获取所有数据
      // 二维数据
      const allArrs = Object.values(this.allTreeData) || [];
      // 转为一维数组
      const formatAllArrs = allArrs.flat();
      // 获取名字集合
      const recipientNameArr = formatAllArrs
          .filter((v) => this.checkedUsers.includes(v.account))
          .map((v) => v.realName);
      // 名字去重
      const newSetRecipientNameArr = [...new Set(recipientNameArr)];
      const { notice = {} } = this.formData;
      this.formData = {
        notice: {
          ...notice,
          target: newSetRecipientNameArr.join(","),
        },
        accountList: this.checkedUsers,
        nameList: newSetRecipientNameArr,
      };
      this.showDialog = false;
    },
    // 提交数据
    async handleSubmit(form, done) {
      for (let item of this.fileList) {
        if (func.isEmpty(item.id)) {
          this.$message.warning("请等待文件上传完成后保存！");
          done();
          return false;
        }
        if (func.isEmpty(item.diyFileName)) {
          this.$message.warning("请完善文件名称后保存！");
          done();
          return false;
        }
      }

      const { ...other } = form;
      const { accountList = [],nameList=[], } = this.formData;
      const attachList = this.fileList
      let baseUrl = ""
      const url = window.location.href
      if (url) {
        baseUrl = url.split("?")[0]
      }
      const fromIdList = this.sourceData.map(item => item.id)
      // 定义对象
      const addObj = {
        accountList,
        nameList,
        fromIdList,
        ...other,
        attachList,
        baseUrl
      };
      console.log(addObj)
      submit(addObj).then(() => {
        this.$message({
          type: "success",
          message: "操作成功!"
        });
        done();
        this.openDialog = false
      }, error => {
        done();
        window.console.log(error);
      });


      // const {
      //   data: { code, msg },
      // } = await addNotice(addObj);
      //
      // if (code === 200) {
      //   this.$refs.form.resetForm();
      //   this.$message.success(msg);
      // } else {
      //   this.$message.error(msg);
      // }
      // this.openDialog = false
      // done();
    },
    handleReset() {
      console.log("handleReset")
      // this.formData= {
      //   notice: {
      //     target: "",
      //   },
      //   ids: "",
      // }
      this.checkedUsers = [];
      this.fileList = []
      this.checkAll = false;
      this.isIndeterminate = false;
      this.initData()
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
