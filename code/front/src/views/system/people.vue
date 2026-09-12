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
               @cell-dblclick="editList"
               @row-update="rowUpdate"
               @row-save="rowSave"
               @row-del="rowDel"
               @search-change="searchChange"
               @search-reset="searchReset"
               @selection-change="selectionChange"
               @current-change="currentChange"
               @size-change="sizeChange"
               @refresh-change="refreshChange"
               @on-load="onLoad">
        <!-- 左边按钮 -->
        <template #menu-left="{ row,index }">
            <el-button class="button-size" round title="新增" v-if="permission.people_add" size="small" icon="el-icon-plus" @click.stop="addList()">
            </el-button>
            <el-button class="button-size" round title="删除" v-if="permission.people_delete" size="small" icon="el-icon-delete"
                @click.stop="handleDelete()">
            </el-button>
            <el-button class="button-size" round title="复制" v-if="permission.people_add" size="small" icon="el-icon-document-copy" @click.stop="copyList()">
            </el-button>

          <el-button class="button-size" round title="用户成组" @click="groupPeople" size="small" icon="el-icon-user" >
          </el-button>
        </template>

        <!-- 打开附件列表 -->
        <template #uploadList="{ row }">
            <row-attach :source-id="row.id"></row-attach>
        </template>

        <template #index="{row,index}">
            <div>{{index+1}}</div>
        </template>

        <template #menu-form="{ row,index,type }">

          <template v-if="type !== 'view'">
            <el-button icon="el-icon-check" size="small" circle @click="$refs.crud.rowSave()" title="保存"></el-button>
            <el-button circle icon="el-icon-close" size="small" title="取消" @click="$refs.crud.closeDialog()"></el-button>
          </template>

        </template>
    </avue-crud>
    <!-- 左下按钮 -->
    <page-attach ref="pageAttach"></page-attach>
  </basic-container>
</template>

<script>
  import pageAttach from "@/components/attach-dialog/page-attach/main.vue"
  import rowAttach from "@/components/attach-dialog/row-attach/main.vue"
  import {getList, getDetail, add, update, remove,doSync} from "@/api/system/people";
  import {mapGetters} from "vuex";
  import {exportBlob} from "@/api/common";
  import {getToken} from '@/utils/auth';
  import {downloadXls} from "@/utils/util";
  import {dateNow} from "@/utils/date";
  import NProgress from 'nprogress';
  import 'nprogress/nprogress.css';
  import {getDeptTree} from "@/api/system/dept";
  import {getPostList} from "@/api/system/post";
  import website from "@/config/website";


  export default {
    components: {
     pageAttach, rowAttach
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
              label: '头像',
              type: 'upload',
              listType: 'picture-img',
              accept: "image/jpeg, image/png",
              fileSize: 500,
              propsHttp: {
                res: 'data',
                url: 'link',
              },
              canvasOption: {
                text: ' ',
                ratio: 0.1
              },
              action: '/api/blade-resource/oss/endpoint/put-file',
              tip: '只能上传jpg/png用户头像，且不超过500kb',
              span: 24,
              row: true,
              prop: 'avatar'
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
              label: "创建人",
              prop: "createUser",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
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
              label: "账号",
              prop: "account",
              type: "input",
              rules: [{
                required: true,
                message: "请输入账号",
                trigger: "blur"
              }],
              search: true,
            },
            {
              label: "姓名",
              prop: "realName",
              type: "input",
              rules: [{
                required: true,
                message: "请输入姓名",
                trigger: "blur"
              }],
              search: true,
            },
            {
              label: "邮箱",
              prop: "email",
              type: "input",
              rules: [{
                required: true,
                message: "请输入邮箱",
                trigger: "blur"
              }]
            },
            {
              label: "手机",
              prop: "phone",
              type: "input",
              search: true,
              rules: [{
                required: true,
                message: "请输入手机",
                trigger: "blur"
              }]
            },
            {
              label: "微信",
              prop: "wechat",
              type: "input",
              rules: [{
                required: true,
                message: "请输入微信",
                trigger: "blur"
              }]
            },
            {
              label: "性别",
              prop: "sex",
              type: "select",
              dicData: [
                {
                  label: "男",
                  value: 1
                },
                {
                  label: "女",
                  value: 2
                },
                {
                  label: "未知",
                  value: 3
                }
              ],
              rules: [{
                required: true,
                message: "请选择性别",
                trigger: "change"
              }]
            },
            {
              label: "人司关系",
              prop: "relation",
              type: "input",
              rules: [{
                required: true,
                message: "请输入人司关系",
                trigger: "blur"
              }]
            },
            // {
            //   label: "备注",
            //   prop: "remark",
            //   type: "input",
            // },
            {
              label: "数据来源",
              prop: "isSync",
              type: "select",
              display: false,
              dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=data_source",
              props: {
                label: "dictValue",
                value: "dictKey"
              },
            },
            {
              label: "微信OPENID",
              prop: "wxOpenId",
              labelWidth:120,
              type: "input",
            },
            {
              label: "备注",
              prop: "remark",
              type: "input",
            },
            {
              label: "其他说明",
              prop: "otherDescribe",
              type: "input",
              hide: true
            },
            {
              label: "所属部门",
              prop: "deptId",
              type: "tree",
              multiple: true,
              dicData: [],
              props: {
                label: "title",
                value: "id"
              },
              // checkStrictly: true,
              slot: true,
            },
            {
              label: "所属岗位",
              prop: "postId",
              type: "tree",
              multiple: true,
              dicData: [],
              props: {
                label: "postName",
                value: "id"
              },
              rules: [],
            },
            {
              label: "用户成组",
              prop: "peopleGroupIds",
              type: "select",
              hide: true,
              dicMethod: "post",
            dicUrl: "/api/blade-system/peopleGroup/select",
              multiple:true,
              props: {
                label: 'groupName',
                value: 'id',
              },
            },
            {
              label: "身份证号码",
              prop: "idNo",
              type: "input",
              hide: true
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
          addBtn: this.vaildData(this.permission.people_add, false),
          viewBtn: this.vaildData(this.permission.people_view, false),
          delBtn: this.vaildData(this.permission.people_delete, false),
          editBtn: this.vaildData(this.permission.people_edit, false)
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
    created() {
      this.initDict()
    },
    methods: {
      groupPeople(){

      },
      initDict(){
        const tenantId = website.tenantId
        getDeptTree(tenantId).then(res => {
          const column = this.findObject(this.option.column, "deptId");
          column.dicData = res.data.data;
        });
        getPostList(tenantId).then(res => {
          const column = this.findObject(this.option.column, "postId");
          column.dicData = res.data.data;
        });
      },
      sync() {
        doSync().then(()=>{
          this.onLoad(this.page);
          this.$message({
            type: "success",
            message: "同步成功!"
          });
        })
      },
      addList() {
        this.$refs.crud.rowAdd()
        this.form = {
          isSync:'add'
        }
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
        this.form.isSync = "add"
        this.$refs.crud.dicInit()
      },
      editList(row,{property}) {

        if (this.permission.people_edit && property === "index") {
            clearTimeout(this.timer);
            if (row.isSync === "sync") {
              this.$message.warning("数据同步数据不允许编辑!")
              return
            }
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
        for (const row of this.selectionList) {
          if (row.isSync === "sync") {
            this.$message.warning("数据同步数据不允许删除!")
            return;
          }
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
      handleExport() {
        let downloadUrl = `/api/blade-people/people/export-people?${this.website.tokenHeader}=${getToken()}`;
        const {
            account,
            realName,
            phone,
        } = this.query;
        let values = {
            account_: account,
            realName_: realName,
            phone_: phone,
        };
        this.$confirm("是否导出数据?", "提示", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }).then(() => {
          NProgress.start();
          exportBlob(downloadUrl, values).then(res => {
            downloadXls(res.data, `人员${dateNow()}.xlsx`);
            NProgress.done();
          })
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
          account,
          realName,
          phone,
        } = this.query;

        let values = {
           account: account,
           realName_like: realName,
           phone_like: phone,
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


