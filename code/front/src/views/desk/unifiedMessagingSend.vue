<template>
  <basic-container>
    <avue-crud :option="option"
               v-model:search="query"
               :table-loading="loading"
               :data="data"
               v-model:page="page"
               :permission="permissionList"
               :before-open="beforeOpen"
               v-model="form"
               ref="crud"
               @cell-click="rowView"
               @search-change="searchChange"
               @search-reset="searchReset"
               @selection-change="selectionChange"
               @current-change="currentChange"
               @size-change="sizeChange"
               @refresh-change="refreshChange"
               @on-load="onLoad">
        <!-- 左边按钮 -->


        <!-- 打开附件列表 -->
        <template #uploadList="{ row }">
            <row-attach :source-id="row.id"></row-attach>
        </template>

        <template #index="{row,index}">
            <div>{{index+1}}</div>
        </template>

      <template #href="{row,index}">
        <span  @click="windowTo(row.href)" style="text-decoration:underline;cursor:pointer;">{{row.href}}</span>
      </template>

    </avue-crud>
    <!-- 左下按钮 -->
    <page-attach ref="pageAttach"></page-attach>
  </basic-container>
</template>

<script>
  import rowAttach from "@/components/attach-dialog/row-attach/main.vue"
  import pageAttach from "@/components/attach-dialog/page-attach/main.vue"
  import {getList, getDetail, add, update, remove} from "@/api/desk/unifiedMessagingSend";
 // import option from "@/option/desk/unifiedMessagingSend";
  import {mapGetters} from "vuex";
  import 'nprogress/nprogress.css';

  export default {
    components: {
    rowAttach,pageAttach
    },

    data() {
      return {
        form: {},
        query: {},
        search: {},
        loading: true,
        page: {
          pageSize: 10,
          currentPage: 1,
          total: 0
        },
        selectionList: [],
        option: {
          searchShowBtn:false,
          saveBtn: false,
          updateBtn: false,
          cancelBtn: false,
          menu: false,
          addBtn: false,
          delBtn: false,
          editBtn: false,
          dialogDrag: true,
          searchBtn: false,
          emptyBtn: false,
          height:'auto',
          calcHeight: 30,
          tip: false,
          searchShow: true,
          searchMenuSpan: 6,
          border: true,
          viewBtn: true,
          selection: true,
          labelWidth: 140,
          dialogClickModal: false,
          column: [
            {
              label:'序号',
              prop:'index',
              fixed:true,
              width: 70,
              display: false,
              align: "center"
            },
            {
              label: "编号",
              prop: "id",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "通知标题",
              prop: "noticeTitle",
              type: "input",
              search: true,
            },

            {
              label: "通知状态",
              width: 80,
              prop: "noticeStatus",
              type: "select",
              dicData: [{label: "关闭", value: 2,}, {label: "正常", value: 1,}]
            },
            {
              label: "发送频率",
              width: 100,
              prop: "sendRate",
              search: true,
              type: "select",
              dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=notice_frequency",
              props: {
                label: 'dictValue',
                value: 'dictKey'
              },
            },
            {
              label: "发送类型",
              prop: "sendType",
              width: 100,
              search: true,
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
            },
            {
              label: "首次发送时间",
              prop: "sentTime",
              type: "date",
              format: "yyyy-MM-dd hh:mm:ss",
              valueFormat: "yyyy-MM-dd hh:mm:ss",
            },
            {
              label: "截至发送时间",
              prop: "endSentTime",
              type: "date",
              format: "yyyy-MM-dd hh:mm:ss",
              valueFormat: "yyyy-MM-dd hh:mm:ss",
            },
            {
              label: "发送人",
              prop: "createUser",
              addDisplay: false,
              editDisplay: false,
              type: "select",
              dicMethod: "post",
            dicUrl: "/api/blade-system/user/user-kv-list",
              width: "100",
              dicFormatter: (res) => {
                return res.data
              },
              props: {
                label: 'key',
                value: 'value',
              },
              // viewDisplay: false,
              // hide: true,
            },
            {
              label: "创建部门",
              prop: "createDept",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "创建时间",
              prop: "createTime",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "修改人",
              prop: "updateUser",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "修改时间",
              prop: "updateTime",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            // {
            //   label: "备注",
            //   prop: "remark",
            //   type: "input",
            // },
            {
              label: "状态",
              prop: "status",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "是否已删除",
              prop: "isDeleted",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "租户ID",
              prop: 'tenantId',
              hide: true, display: false, addDisplay: false, editDisplay: false, viewDisplay: false, search: false,
      hide: true, display: false, addDisplay: false, editDisplay: false, viewDisplay: false, search: false,
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "链接",
              prop: "href",
              type: "input",
              width: 200,
              slot: true,
              overHidden: true,
            },
            {
              label: "关联数据id",
              prop: "fromId",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "发送目标",
              prop: "targetName",
              hide: true,
              type: "input",
            },
            {
              label: '附件列表',
              span: 24,
              width: "100",
              prop: 'uploadList',
              slot: true,
              display: false,
            },
            {
              label: "通知内容",
              prop: "noticeContent",
              type: "input",
              hide: true,
              row: true,
              span: 24,
              component: "AvueUeditor",
              options: {
                action: "/api/blade-resource/oss/endpoint/put-file",
                props: {
                  res: "data",
                  url: "link",
                },
              },
            },
          ]
        },
        data: [],
        timer: null
      };
    },
    computed: {
      ...mapGetters(["permission"]),
      permissionList() {
        return {
          addBtn: this.vaildData(this.permission.unifiedMessagingSend_add, false),
          viewBtn: this.vaildData(this.permission.unifiedMessagingSend_view, false),
          delBtn: this.vaildData(this.permission.unifiedMessagingSend_delete, false),
          editBtn: this.vaildData(this.permission.unifiedMessagingSend_edit, false)
        };
      },
      ids() {
        let ids = [];
        this.selectionList.forEach(ele => {
          ids.push(ele.id);
        });
        return ids.join(",");
      }
    },
    methods: {
      windowTo(href){
        window.open(href, '_blank')
      },
      addList() {
        this.$refs.crud.rowAdd()
        this.form = {}
        this.$refs.crud.dicInit()
      },
      copyList(){
        if (this.selectionList.length === 0) {
          this.$message.warning("请选择至少一条数据");
          return;
        }
        this.$refs.crud.rowAdd()
        this.form = this.selectionList[0]
        this.form.id = ""
        this.$refs.crud.dicInit()
      },
      editList(row,{property}) {
        if (this.permission.unifiedMessagingSend_edit && property === "index") {
            clearTimeout(this.timer);
            this.$refs.crud.dicInit()
            this.$refs.crud.rowEdit(row, row.$index)
        }
      },
      rowView(row, {property}) {
        if (property === "index") {
          clearTimeout(this.timer);
          this.timer = setTimeout(() =>{
            this.$refs.crud.rowView(row, row.$index)
          },250)
        }
      },
      rowSave(row, done, loading) {
        add(row).then(() => {
          this.onLoad(this.page);
          this.$message({
            type: "success",
            message: "操作成功!"
          });
          done();
        }, error => {
          loading();
          window.console.log(error);
        });
      },
      rowUpdate(row, index, done, loading) {
        update(row).then(() => {
          this.onLoad(this.page);
          this.$message({
            type: "success",
            message: "操作成功!"
          });
          done();
        }, error => {
          loading();
          console.log(error);
        });
      },
      rowDel(row) {
        this.$confirm("确定将选择数据删除?", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        })
          .then(() => {
            return remove(row.id);
          })
          .then(() => {
            this.onLoad(this.page);
            this.$message({
              type: "success",
              message: "操作成功!"
            });
          });
      },
      handleDelete() {
        if (this.selectionList.length === 0) {
          this.$message.warning("请选择至少一条数据");
          return;
        }
        this.$confirm("确定将选择数据删除?", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        })
          .then(() => {
            return remove(this.ids);
          })
          .then(() => {
            this.onLoad(this.page);
            this.$message({
              type: "success",
              message: "操作成功!"
            });
            this.$refs.crud.toggleSelection();
          });
      },

      beforeOpen(done, type) {
        if (["edit", "view"].includes(type)) {
          getDetail(this.form.id).then(res => {
            this.form = res.data.data;
          });
        }
        done();
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
      currentChange(currentPage){
        this.page.currentPage = currentPage;
      },
      sizeChange(pageSize){
        this.page.pageSize = pageSize;
      },
      refreshChange() {
        this.onLoad(this.page, this.query);
      },
      onLoad(page) {

        this.loading = true;

        const {
          noticeTitle,
          sendRate,
          sendType,
        } = this.query;

        let values = {
           noticeTitle_like: noticeTitle,
           sendRate_equal: sendRate,
           sendType_equal: sendType,

        };


        getList(page.currentPage, page.pageSize, values).then(res => {
          const data = res.data.data;
          this.page.total = data.total;
          this.data = data.records;
          this.loading = false;
          this.selectionClear();
        });
      }
    }
  };
</script>


