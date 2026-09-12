<template>
  <basic-container>
    <avue-crud :option="option"
               v-model:search="query"
               @cell-click="rowView"
               @cell-dblclick="editList"
               :table-loading="loading"
               :data="data"
               v-model:page="page"
               :permission="permissionList"
               :before-open="beforeOpen"
               v-model="form"
               ref="crud"
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
        <el-button class="button-size" round title="新增" v-if="permission.bizDatasource_add" size="small"
                   icon="el-icon-plus" @click.stop="addList()">
        </el-button>
        <el-button class="button-size" round title="删除" v-if="permission.bizDatasource_delete" size="small"
                   icon="el-icon-delete"
                   @click.stop="handleDelete()">
        </el-button>
        <el-button class="button-size" round title="复制" v-if="permission.bizDatasource_add" size="small"
                   icon="el-icon-document-copy" @click.stop="copyList()">
        </el-button>
      </template>

      <template #index="{row,index}">
        <div>{{index + 1}}</div>
      </template>

      <template #linkTypeType="{ item,value,label }">
        <span>{{ item.dictValue }}</span> <span style="float: right">

              <el-tooltip effect="dark"
                          :content="item.remark"
                          placement="bottom">
                 <el-icon class="el-icon-info" />
              </el-tooltip>
      </span>
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
  import {getList, getDetail, add, update, remove} from "@/api/tool/datasource";
  import {mapGetters} from "vuex";
  import pageAttach from "@/components/attach-dialog/page-attach/main.vue";

  export default {
    components: {
      pageAttach
    },
    data() {
      return {
        form: {},
        query: {},
        loading: true,
        page: {
          pageSize: 30,
          currentPage: 1,
          total: 0
        },
        selectionList: [],
        option: {
          searchShowBtn:false,
          dialogDrag: true,
          saveBtn: false,
          updateBtn: false,
          cancelBtn: false,
          menu: false,
          addBtn: false,
          delBtn: false,
          editBtn: false,
          searchBtn: false,
          emptyBtn: false,
          height: 'auto',
          calcHeight: 30,
          dialogWidth: 900,
          tip: false,
          searchShow: true,
          searchMenuSpan: 6,
          border: true,
         // index: true,
          viewBtn: true,
          selection: true,
          dialogClickModal: false,
          column: [
            {
              label: '序号',
              prop: 'index',
              fixed: true,
              width: 70,
              display: false,
              align: "center"
            },
            {
              label: "名称",
              prop: "name",
              width: 120,
              rules: [{
                required: true,
                message: "请输入数据源名称",
                trigger: "blur"
              }]
            },
            {
              label: "驱动类",
              prop: "driverClass",
              type: 'select',
              dicData: [
                {
                  label: 'com.mysql.cj.jdbc.Driver',
                  value: 'com.mysql.cj.jdbc.Driver',
                }, {
                  label: 'org.postgresql.Driver',
                  value: 'org.postgresql.Driver',
                }, {
                  label: 'oracle.jdbc.OracleDriver',
                  value: 'oracle.jdbc.OracleDriver',
                }, {
                  label: 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
                  value: 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
                }, {
                  label: 'dm.jdbc.driver.DmDriver',
                  value: 'dm.jdbc.driver.DmDriver',
                }
              ],
              width: 200,
              rules: [{
                required: true,
                message: "请输入驱动类",
                trigger: "blur"
              }]
            },
            {
              label: "用户名",
              prop: "username",
              width: 120,
              rules: [{
                required: true,
                message: "请输入用户名",
                trigger: "blur"
              }]
            },
            {
              label: "密码",
              prop: "password",
              hide: true,
              rules: [{
                required: true,
                message: "请输入密码",
                trigger: "blur"
              }]
            },
            {
              label: "数据库地址",
              prop: "host",
              labelWidth: 100,
              placeholder: "请输入数据库地址，例如 127.0.0.1",
              rules: [{
                required: true,
                message: "请输入地址",
                trigger: "blur"
              }]
            },
            {
              label: "数据库端口",
              prop: "port",
              labelWidth: 100,
              width:110,
              placeholder: "请输入数据库端口，例如 3306",
              rules: [{
                required: true,
                message: "请输入端口",
                trigger: "blur"
              }]
            },
            {
              label: "数据库库名",
              prop: "databaseName",
              labelWidth: 100,
              placeholder: "请输入数据库数据库名称，例如 bladex",
              rules: [{
                required: true,
                message: "请输入数据库名称",
                trigger: "blur"
              }]
            },
            {
              label: "库链接类型",
              prop: "linkType",
              dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=baselink",
              labelWidth: 100,
              type:"select",
              placeholder: "请输入库链接类型",
              props: {
                label: 'dictValue',
                value: 'dictKey'
              },
              rules: [{
                required: true,
                message: "请输入库链接类型",
                trigger: "blur"
              }]
            },
            {
              label: "备注",
              prop: "remark",
              span: 24,
              // width: 200,
              overHidden:true,
              showOverflowTooltip:true,
              minRows: 3,
              type: "textarea"
            },
          ]
        },
        data: []
      };
    },
    computed: {
      ...mapGetters(["permission"]),
      permissionList() {
        return {
          addBtn: this.vaildData(this.permission.bizDatasource_add, false),
          viewBtn: this.vaildData(this.permission.bizDatasource_view, false),
          delBtn: this.vaildData(this.permission.bizDatasource_delete, false),
          editBtn: this.vaildData(this.permission.bizDatasource_edit, false)
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
      addList() {
        this.$refs.crud.rowAdd()
        this.form = {}
        this.$refs.crud.dicInit()
      },
      copyList() {
        if (this.selectionList.length === 0) {
          this.$message.warning("请选择至少一条数据");
          return;
        }
        this.$refs.crud.rowAdd()
        this.form = this.selectionList[0]
        this.form.id = ""
        this.$refs.crud.dicInit()
      },
      editList(row, {property}) {
        if (this.permission.bizDatasource_edit && property === "index") {
          clearTimeout(this.timer);
          this.$refs.crud.dicInit()
          this.$refs.crud.rowEdit(row, row.$index)
        }
      },
      rowView(row, {property}) {
        if (property === "index") {
          clearTimeout(this.timer);
          this.timer = setTimeout(() => {
            this.$refs.crud.rowView(row, row.$index)
          }, 250)
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
          window.console.log(error);
          loading();
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
          window.console.log(error);
          loading();
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
      currentChange(currentPage) {
        this.page.currentPage = currentPage;
      },
      sizeChange(pageSize) {
        this.page.pageSize = pageSize;
      },
      refreshChange() {
        this.onLoad(this.page, this.query);
      },
      onLoad(page, params = {}) {
        this.loading = true;
        getList(page.currentPage, page.pageSize, Object.assign(params, this.query)).then(res => {
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


