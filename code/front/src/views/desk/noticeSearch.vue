<template>
  <basic-container>
    <avue-crud
      :key="reload"
      class="notice-search"
      :option="option"
      :table-loading="loading"
      :data="data"
      v-model:page="page"
      v-model:search="query"
      ref="crud"
      v-model="form"
      @row-click="tableRowClick"
      @cell-mouse-enter="rowUnderline"
      @cell-mouse-leave="Underline"
      @search-change="searchChange"
      @search-reset="searchReset"
      @selection-change="selectionChange"
      @current-change="currentChange"
      @size-change="sizeChange"
      @refresh-change="refreshChange"
      @on-load="onLoad">
      <template #noticeTitleHeader="{  column  }">
        <el-tag class="titleHeader">{{ (column || {}).label }}</el-tag>
      </template>
      <template #messageCountHeader="{  column  }">
        <el-tag class="titleHeader">{{ (column || {}).label }}</el-tag>
      </template>
      <template #searchMenu="{  row, size  }">
        <avue-switch
          active-color="#13ce66"
          inactive-color="#00BBFF"
          v-model="switchTable"
          :dic="dic"
          @click="switchCheck()"
        ></avue-switch>
        <el-button
          size="small"
          title="搜索"
          circle
          icon="el-icon-refresh"
          @click="searchSubmit()"
        ></el-button>
      </template>
    </avue-crud>
    <!-- 左下按钮 -->
    <page-attach ref="pageAttach"></page-attach>
    <!-- 接收人详情弹窗 -->
    <el-dialog
      title="接收人详情"
      draggable
      v-model="sendtype"
      :append-to-body="true"
      class="avue-dialog avue-dialog--top"
      width="60%"
    >
      <avue-crud
        :option="receiveOption"
        v-model="receiveform"
        :data="receivedata"
        @cell-mouse-leave="receiveUnderline"
      >
      </avue-crud>
    </el-dialog>
    <el-dialog
      title="详情内容"
      draggable
      v-model="type"
      :append-to-body="true"
      class="avue-dialog avue-dialog--top"
      width="50%"
    >
      <avue-form
        v-if="this.switchTable == 0"
        :option="dialogOption"
        v-model="form"
        :data="data"
      >
      </avue-form>

      <avue-form
        v-if="this.switchTable == 1"
        :option="receiveOption"
        v-model="form"
        :data="data">
      </avue-form>
      <el-row v-if="this.switchTable == 1" style="padding-left: 90px">
        <span>未读未处理：{{messageCount.unread}} %； 已读未处理：{{messageCount.unprocess}} % 已读已处理：{{messageCount.processed}} %</span>
      </el-row>
      <div class="avue-dialog__footer">
        <el-popover trigger="click" placement="top">
          <el-link @click="toHref" type="primary">{{ form.href }}</el-link>
          <template #reference>
          <el-button v-if="form.href != ''"
            >查看链接</el-button
          >
          <el-button
            v-if="this.form.href == ''"
            type="info"
            plain
            disabled
            >查看链接</el-button
          >
          </template>
        </el-popover>
        <el-popover placement="top" trigger="click" width="800">
          <avue-form
            :option="detailOption"
            v-model="form"
            :data="data"
            width="60%"
          ></avue-form>
          <template #reference>
          <el-button v-if="this.form.noticeContent != ''"
            >内容详情</el-button
          >
          <el-button
            v-if="this.form.noticeContent == ''"
            type="info"
            plain
            disabled
            >内容详情</el-button
          >
          </template>
        </el-popover>
        <attach-dialog ref="attach" :source-id="this.form.noticeId" :just-read="true"></attach-dialog>
        <el-button
          plain
         @click="openAttach">附件内容</el-button>
        <el-button @click="Proprietaryinfo">专属信息</el-button>
      </div>
    </el-dialog>
  </basic-container>
</template>

<script>
import { mapGetters } from "vuex";
import {getNoticeManagePage,getMessageBySendUser} from "@/api/desk/notice/notice"
import attachDialog from "@/components/attach-dialog/main.vue";
import pageAttach from "@/components/attach-dialog/page-attach/main.vue"
let list = [];
export default {
  components: {
    attachDialog,pageAttach
  },
  data() {
    return {
      messageCount: {
        unread: 0,
        unprocess: 0,
        processed: 0
      },
      reload: Math.random(),
      componentName: "",
      switchTable: 0,
      dic: [
        {
          label: "作为接收人",
          value: 0,
        },
        {
          label: "作为发送人",
          value: 1,
        },
      ],
      type: false,
      sendtype: false,
      receivedata: [],
      form: {},
      receiveform: {},
      query: {},
      loading: true,
      page: {
        pageSize: 30,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      search: {
        $noticeReadStatus: "",
        $receiveUserId: "",
        createUser: "",
        noticeReadStatus: "",
        rangeTime: [],
        receiveUserId: "",
      },
      data: list,
      option: {
        height: "auto",
        menu: false,
        calcHeight: 10,
        dialogWidth: 950,
        tip: false,
        searchShow: true,
        searchShowBtn: false,
        searchMenuSpan: 24,
        searchMenuPosition: "right",
        border: true,
        index: true,
        addBtn: false,
        viewBtn: true,
        searchBtn: false,
        emptyBtn: false,
        refreshBtn: false,
        columnBtn: false,
        dialogClickModal: false,
        // selection:true,
        align: "center",
        searchLabelWidth: 100,
        column: [
          {
            label: "标题",
            prop: "noticeTitle",
            rules: [
              {
                required: true,
                message: "请输入通知标题",
                trigger: "blur",
              },
            ],
            width: 230,
            search: false,
            searchSpan: 6,
            order: 23,
          },
          {
            label: "发送人",
            prop: "sendUserName",
            search: true,
            order: 20,
            searchSpan: 6,
            dicMethod: "post",
            dicUrl: "/api/blade-bizNotice/bizNotice/selectSendNameList",
            props: {
              label: "name",
              value: "id",
            },
            type: "select",
            rules: [
              {
                required: true,
                message: "请输入通知标题",
                trigger: "blur",
              },
            ],
            hide: false,
            headerslot: true,
          },
          {
            label: "接收人数量",
            prop: "messageCount",
            // type: "select",
            searchSpan: 6,
            row: true,
            search: false,
            hide: true,
            headerslot: true,
          },
          {
            label: "接收人",
            prop: "receiveUserId",
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-bizNotice/bizNotice/selectRecipientNameList",
            props: {
              label: "name",
              value: "id",
            },
            hide: true,
            searchSpan: 6,
            row: true,
            search: false,
            rules: [
              {
                required: true,
                message: "请输入通知标题",
                trigger: "blur",
              },
            ],
          },
          {
            label: "状态",
            prop: "noticeReadStatus",
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=notice_process_status",
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },
            row: true,
            search: true,
            searchSpan: 6,
            hide: false,
          },
          {
            label: "内容",
            prop: "noticeContent",
            overHidden: true,
            rules: [
              {
                required: true,
                message: "请输入通知标题",
                trigger: "blur",
              },
            ],
            hide: true,
          },
          {
            label: "消息类型",
            prop: "category",
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=notice_type",
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },
            search: true,
            searchSpan: 6,
          },
          {
            label: "链接",
            prop: "href",
            row: true,
            rules: [
              {
                required: true,
                message: "请输入通知标题",
                trigger: "blur",
              },
            ],
            hide: true,
          },
          {
            label: "附件",
            prop: "attachmentName",
            row: true,
            overHidden: true,
            rules: [
              {
                required: true,
                message: "请输入通知标题",
                trigger: "blur",
              },
            ],
            hide: true,
          },
          {
            label: "处理意见",
            prop: "noticeReply",
            type: "textarea",
            hide: true,
          },
          {
            label: "专属交互",
            prop: "",
            row: true,
            rules: [
              {
                required: true,
                message: "请输入通知标题",
                trigger: "blur",
              },
            ],
            hide: true,
          },
          {
            label: "时间范围",
            prop: "rangeTime",
            type: "datetimerange",
            format: "yyyy-MM-dd hh:mm:ss",
            valueFormat: "yyyy-MM-dd hh:mm:ss",
            startPlaceholder: "开始日期",
            endPlaceholder: "结束日期",
            searchRange: true,
            search: true,
            searchSpan: 6,
            hide: true,
            display: true,
            rules: [
              {
                required: true,
                message: "请输入通知时间",
                trigger: "blur",
              },
            ],
          },
          {
            label: "创建时间",
            prop: "noticeSentTime",
            type: "datetime",
            order: 1
          },
          {
            label: "更新时间",
            prop: "updateTime",
            type: "datetime",
          },
          {
            label: "最新回复时间",
            prop: "noticeReplyTime",
            type: "datetime",
            hide: true
          }
        ],
      },
    };
  },
  computed: {
    ...mapGetters(["userInfo"]),
    ids() {
      let ids = [];
      this.selectionList.forEach((ele) => {
        ids.push(ele.id);
      });
      return ids.join(",");
    },
    dialogOption() {
      return {
        emptyBtn: false,
        submitBtn: false,
        column: [
          {
            label: "标题",
            prop: "noticeTitle",
            rules: [
              {
                required: true,
                message: "请输入通知标题",
                trigger: "blur",
              },
            ],
            disabled: true,
          },
          {
            label: "发布时间",
            prop: "createTime",
            disabled: true,
          },
          {
            label: "发布人",
            prop: "sendUserName",
            disabled: true,
          },
          {
            label: "被通知人",
            prop: "receiveUserName",
            multiple: true,
            disabled: true,
            // display: false,
          },
          // {
          //   label: "发送人",
          //   prop: "createUser",
          //   search: true,
          //   searchSpan: 5,
          //   dicMethod: "post",
          //  dicUrl: "/api/blade-desk/notice/selectListName",
          //   props: {
          //     label: "name",
          //     value: "userid",
          //   },
          //   disabled: true,
          //   type: "select",
          //   rules: [
          //     {
          //       required: true,
          //       message: "请输入通知标题",
          //       trigger: "blur",
          //     },
          //   ],
          // },

          // {
          //   label: "消息类型",
          //   prop: "category",
          //   type: "select",
          //   dicMethod: "post",
          //  dicUrl:
          //     "/api/blade-system/bizParam/getListByPath?path=file.file_tz",
          //   props: {
          //     label: "paramName",
          //     value: "paramValue",
          //   },
          //   search: true,
          //   searchSpan: 5,
          //   disabled: true,
          //   display: false,
          // },
          {
            label: "状态",
            prop: "noticeReadStatus",
            type: "select",
            disabled: true,
            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=notice_process_status",
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },
            // display: false,
          },
          {
            label: "处理时间",
            prop: "updateTime",
            disabled: true,
            display: false,
          },
          {
            label: "处理意见",
            prop: "noticeReply",
            span: 24,
            type: "textarea",
            disabled: true,
            display: true,
            hide: true,
            overHidden: true,
          },
        ],
      };
    },
    receiveOption() {
      return {
        index: true,
        menu: false,
        addBtn: false,
        emptyBtn: false,
        submitBtn: false,
        column: [

          {
            label: "接收人",
            prop: "receiveUserName",
            disabled: true,
            hide: false,
            display: false,
          },
          {
            label: "标题",
            prop: "noticeTitle",
            disabled: true,
          },
          {
            label: "发布人",
            prop: "sendUserName",
            // dicMethod: "post",
            dicMethod: "post",
            dicUrl: "/api/blade-bizNotice/bizNotice/selectRecipientNameList",
            // props: {
            //   label: "name",
            //   value: "id",
            // },
            disabled: true,
            hide: true,
            display: true,
          },
          {
            label: "发布时间",
            prop: "noticeSentTime",
            disabled: true,
            hide: true,
            display: true,
          },
          {
            label: "接收人数量",
            prop: "messageCount",
            row: true,
            disabled: true,
            search: false,
            hide: true,
          },
          {
            label: "状态",
            prop: "noticeReadStatus",
            type: "select",
            disabled: true,
            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=notice_process_status",
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },
            display: false,
          },
          {
            label: "处理意见",
            prop: "noticeReply",
            span: 24,
            type: "textarea",
            disabled: true,
            display: false,
          },
          {
            label: "最近反馈时间",
            prop: "noticeReplyTime",
            disabled: true,
            display: false,
          },
        ],
      };
    },
    detailOption() {
      return {
        submitBtn: false,
        emptyBtn: false,
        column: [
          {
            label: "",
            prop: "noticeContent",
            component: "AvueUeditor",
            options: {
              action: "/api/blade-resource/oss/endpoint/put-file",
              props: {
                res: "data",
                url: "link",
              },
            },
            labelWidth: 0,
            // type:'textarea',
            disabled: true,
            Rows: 5,
            span: 24,
            showWordLimit: true,
          },
        ],
      };
    },
  },
  methods: {
    openAttach() {
      this.$refs.attach.showAttachDialog()
    },
    switchCheck() {
      const sendUserName = this.findObject(this.option.column, "sendUserName");
      const messageCount = this.findObject(
        this.option.column,
        "messageCount"
      );
      const receiveUserId = this.findObject(this.option.column, "receiveUserId");
      const noticeReadStatus = this.findObject(
        this.option.column,
        "noticeReadStatus"
      );
      const noticeReply = this.findObject(this.option.column, "noticeReply");
      const updateTime = this.findObject(this.option.column, "updateTime");
      const noticeTitle = this.findObject(this.option.column, "noticeTitle");
      const noticeReplyTime = this.findObject(this.option.column, "noticeReplyTime");
      if (this.switchTable == 0) {
        messageCount.hide = true;
        receiveUserId.search = false;
        noticeReadStatus.hide = false;
        noticeReadStatus.search = true;
        noticeTitle.search = false;
        updateTime.hide = false;
        noticeReplyTime.hide = true;
        sendUserName.hide = false;
        sendUserName.search = true;
        this.reload = Math.random();
      } else if (this.switchTable == 1) {
        noticeReplyTime.hide = false
        receiveUserId.search = true;
        messageCount.hide = false;
        noticeReadStatus.hide = true;
        noticeReadStatus.search = false;
        noticeTitle.search = true;
        noticeTitle.searchLabel = "标题关键词";
        noticeReply.hide = true;
        updateTime.hide = true;
        if ( !this.userInfo.role_name.includes('admin')) {
          sendUserName.hide = true;
        }
        sendUserName.search = false;
        this.reload = Math.random();
      }
      this.onLoad({"currentPage":this.page.currentPage,
        "pageSize":this.page.pageSize})
    },
    //鼠标滑动出现下画线
    rowUnderline(row, column, cell) {
      if (
        column.label == "附件" ||
        column.label == "标题" ||
        column.label == "接收人数量"
      ) {
        cell.style = "text-decoration:underline;color: #222DFE";
      }
    },
    //鼠标移走清除下画线
    Underline(row, column, cell) {
      cell.style = "text-decoration:none;color: none";
    },
    // 点击行数据
    tableRowClick(row, column) {
      if (column.label == "标题") {
        this["type"] = true;
        this.form = row;
        this.countMessageCount(row.noticeId)
        // console.log(this.form)
      } else if (column.label == "接收人数量") {
        this["sendtype"] = true;
        getMessageBySendUser({noticeId:row.noticeId}).then((res) => {
          this.receivedata = res.data.data;
        });
      }
    },
    countMessageCount(noticeId) {
      getMessageBySendUser({noticeId:noticeId}).then((res) => {
        const messages  = res.data.data;
        const group = this.groupByMethod(messages,'noticeReadStatus')
        const allDataLength = messages.length
        if (group) {
          const unread=group['unread']?group['unread'].length:0
          this.messageCount.unread = this.keepTwoDecimal((unread/allDataLength)*100)
          const unprocess=group['unprocess']?group['unprocess'].length:0
          this.messageCount.unprocess = this.keepTwoDecimal((unprocess/allDataLength)*100)
          const processed=group['processed']?group['processed'].length:0
          this.messageCount.processed = this.keepTwoDecimal((processed/allDataLength)*100)
        }
      });
    },
    keepTwoDecimal(num)  {
      return Number(num.toFixed(2));
    },
    // 分组
    groupByMethod (arr, fnOrProperty) {
      return arr.map(typeof fnOrProperty === 'function' ? fnOrProperty : val => val[fnOrProperty])
          .reduce((acc, val, i) => {
            acc[val] = (acc[val] || []).concat(arr[i]);
            return acc;
          }, {});
    },

    // receiveRowUnderline(row, column, cell, event) {
    //   // if (column.label == "接收人") {
    //   //   cell.style = "text-decoration:underline;color: #222DFE"
    //   // }
    // },
    //鼠标移走清除下画线
    receiveUnderline(row, column, cell) {
      cell.style = "text-decoration:none;color: none";
    },
    searchSubmit() {
      this.onLoad({"currentPage":this.page.currentPage,
        "pageSize":this.page.pageSize,})
    },
    // Proprietaryinfo() {
    //   this.$message.warning("该功能正在开发中");
    // },
    async Proprietaryinfo() {
      const { specialInfo = "" } = this.form;
      if (!specialInfo) return;
      // 修改
      const comps = {
        // 数据纠错
        each_tz_tz_jc: "CheckStaffDetailsNew",
      };
      const comp = comps[specialInfo] || "";
      this.componentName = comp;
    },
    toHref() {
      this.type = false;
      let toSrc = this.form.href;
      window.open("http://" + toSrc );
    },
    searchReset() {
      this.query = {};
      this.onLoad(this.page);
    },
    searchChange(params, done) {
      this.query = params;
      this.page.currentPage = 1;
      this.onLoad(this.page, params);
      done();
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    selectionClear() {
      this.selectionList = [];
      this.$refs.crud.toggleSelection();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
    },
    refreshChange() {
      this.onLoad(this.page, this.query);
    },
    onLoad(page) {
      if (this.query.sendUserName != null
        && this.query.sendUserName != undefined
        && this.query.sendUserName != '') {
        this.query.sendUserId = this.query.sendUserName
      } else {
        this.query.sendUserId = ""
      }
      const { rangeTime } = this.query;
      let values = {
        isRecipient:this.switchTable === 0,
        ...this.query,
      };
      if (rangeTime) {
        values.firstTime = rangeTime[0]
        values.endTime = rangeTime[1]
      }
      this.loading = true;
      getNoticeManagePage(
        page.currentPage,
        page.pageSize,
         values
      ).then((res) => {
        const data = res.data.data;
        this.page.total = data.total;
        this.data = data.records;
        this.loading = false;
        // this.selectionClear();
      });
    },
  },
};
</script>

<style  lang="scss" scoped>
// .switch{
//   position: relative;
//   height: 60px;
//   left: 29px;
// }

.titleHeader {
  border: none;
  text-decoration: underline;
  background-color: transparent;
}

.el-switch {
  display: -webkit-inline-box;
  display: -ms-inline-flexbox;
  display: inline-flex;
  -webkit-box-align: center;
  -ms-flex-align: center;
  align-items: center;
  position: absolute;
  font-size: 14px;
  line-height: 20px;
  height: 20px;
  vertical-align: middle;
  top: -100px;
  left: 22px;
}

:deep(.w-e-text-container) {
  height: calc(60vh - 300px) !important;
}

:deep(.avue-form__group--flex) {
  display: -webkit-box;
  display: -ms-flexbox;
  display: flex;
  margin-top: 50px;
}

:deep(.avue-dialog__footer) {
  display: flex;
  flex-wrap: nowrap;
  flex-direction: row;
  justify-content: space-around;
}

.notice-search :deep(.avue-crud__menu) {
  display: none;
}
:deep(.w-e-toolbar .w-e-menu) {
  display: none;
}
</style>
