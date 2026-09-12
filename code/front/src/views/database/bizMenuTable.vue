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
        <el-button class="button-size" round title="新增" v-if="permission.bizMenuTable_add" size="small"
                   icon="el-icon-plus" @click.stop="addList()">
        </el-button>
        <el-button class="button-size" round title="删除" v-if="permission.bizMenuTable_delete" size="small"
                   icon="el-icon-delete"
                   @click.stop="handleDelete()">
        </el-button>
        <el-button class="button-size" round title="复制" v-if="permission.bizMenuTable_add" size="small"
                   icon="el-icon-document-copy" @click.stop="copyList()">
        </el-button>

<!--        <message-build-button :selection-list="selectionList"/>-->
      </template>

      <template #tableNameSearch>
        <el-input
            v-model="query.tableName"
            placeholder="表名称"
            clearable
        ></el-input>
      </template>

      <template #index="{row,index}">
        <div>{{index + 1}}</div>
      </template>

      <template #menuIdType="{ item,value,label }">
        <span>{{item.title}}</span>
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
<!--    <message-build ref = "messageBuild"></message-build>-->
  </basic-container>
</template>

<script>
import pageAttach from "@/components/attach-dialog/page-attach/main.vue"
import {getList, getDetail, add, update, remove} from "@/api/database/bizMenuTable";
import {mapGetters} from "vuex";
import {exportBlob} from "@/api/common";
import {getToken} from '@/utils/auth';
import {downloadXls} from "@/utils/util";
import {dateNow} from "@/utils/date";
import NProgress from 'nprogress';
import 'nprogress/nprogress.css';
import {validatenull} from "@/utils/validate";
import {getTableList} from "@/api/tool/model";
import func from "@/utils/func";
import {getDictionary} from "@/api/system/dictbiz";
import messageBuildButton from "@/components/message-build/menu-button/main.vue";

export default {
  components: {
    pageAttach,messageBuildButton
  },
  watch: {
    'form.datasourceId'() {
      if (!validatenull(this.form.datasourceId)) {
        const fullLoading = this.$loading(this.loadingOption);
        getTableList(this.form.datasourceId).then(res => {
          const column = this.findObject(this.option.column, "tableName");
          column.dicData = res.data.data;
          fullLoading.close();
        }).catch(() => {
          fullLoading.close();
        })
      }
    },
  },
  data() {
    return {
      loadingOption: {
        lock: true,
        text: '物理表读取中',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0)'
      },
      form: {},
      query: {},
      search: {},
      loading: true,
      page: {
        pageSize: 30,
        currentPage: 1,
        total: 0
      },
      tableTypeData:[],
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
        tip: false,
        searchShow: true,
        searchMenuSpan: 6,
        border: true,
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
            label: "主键",
            prop: "id",
            type: "input",
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
          },
          {
            label: "数据源",
            prop: "datasourceId",
            search: false,
            // viewDisplay: false,
            hide: true,
            span: 24,
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-develop/datasource/select",
            props: {
              label: "name",
              value: "id"
            },
            rules: [{
              required: true,
              message: "请选择数据源",
              trigger: "blur"
            }]
          },
          {
            label: "表名称",
            prop: "tableName",
            type: "tree",
            slot: true,
            dicData: [],
            props: {
              label: "comment",
              value: "comment"
            },
            search: true,
            rules: []
          },
          {
            label: "关联菜单",
            prop: "menuId",
            type: "tree",
            dicMethod: "post",
            dicUrl: "/api/blade-system/menu/grant-top-tree",
            search: true,
            dicFormatter: (res) => {
              return res.data.menu
            },
            props: {
              label: 'title',
              value: 'id',
              res: 'id'
            },
            rules: [{
              required: true,
              message: "请选择关联菜单",
              trigger: "blur"
            }],
            typeformat: this.fmt,
            formatter: this.tableFmt
          },
          //tableType
          {
            label: "用途类型",
            prop: "tableType1",
            search: false,
            editDisplay:false,
            addDisplay:false,
            formatter: this.tableTypeFmt
          },
          {
            label: "用途类型",
            prop: "tableType",
            search: true,
            // type: 'radio',
            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=table_use_type",
            type: 'select',
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },
            dicFormatter: (res) => {
              const data = res.data.filter(item=> item.dictKey!="com_func_table" && item.dictKey!="com_conn_table")
              return data
            },
            viewDisplay:false,
            value:'biz_func_table',
            rules: [{
              required: true,
              message: "请选择用途类型",
              trigger: "blur"
            }],
            hide:true,
            change: ({  value }) => {
              // console.log("value",value)
              const menuId = this.findObject(
                  this.option.column,
                  "menuId"
              );
              if (func.notEmpty(value) && value.indexOf("conn")==-1 ) {
                menuId.rules = [{
                  required: true,
                  message: "请选择表名",
                  trigger: "blur"
                }]
              } else {
                menuId.rules = []
              }
            }
          },
          {
            label: "表创建时间",
            prop: "tableCreateTime",
            format: "yyyy-MM-dd",
            valueFormat: "yyyy-MM-dd HH:mm:ss",
            type: "date",
          },
          {
            label: "表作用描述",
            prop: "tableFunc",
            type: "textarea",
          },
          {
            label: "备注",
            prop: "remark",
            type: "input",
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
          // {
          //     label: '附件列表',
          //     span: 24,
          //     width: "100",
          //     prop: 'uploadList',
          //     slot: true,
          //     display: false,
          //   },
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
        addBtn: this.vaildData(this.permission.bizMenuTable_add, false),
        viewBtn: this.vaildData(this.permission.bizMenuTable_view, false),
        delBtn: this.vaildData(this.permission.bizMenuTable_delete, false),
        editBtn: this.vaildData(this.permission.bizMenuTable_edit, false)
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
    this.initDictData()
  },
  methods: {
    initDictData() {
      getDictionary({code:"table_use_type"}).then(res=>{
        this.tableTypeData = res.data.data
      })
    },
    tableFmt (item) {
      return this.treeFindPath(this.$refs.crud.DIC['menuId'],data => data.id === item.menuId,'title').join('/')
    },
    fmt(item) {
      return this.treeFindPath(this.$refs.crud.DIC['menuId'],data => data.id === item.id,'title').join('/')
    },
    treeFindPath(tree, func, field = "", path = []) {
      if (!tree) return []
      for (const data of tree) {
        field === "" ? path.push(data) : path.push(data[field]);
        if (func(data)) return path
        if (data.children) {
          const findChildren = this.treeFindPath(data.children, func, field, path)
          if (findChildren.length) return findChildren
        }
        path.pop()
      }
      return []
    },
    tableTypeFmt(data) {
      const labelData = this.tableTypeData.find(item=>item.dictKey == data.tableType)
      if (func.notEmpty(labelData)) {
        return labelData.dictValue
      }
      return ""
    },
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
      if (this.permission.bizMenuTable_edit && property === "index") {
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
    handleExport() {
      let downloadUrl = `/api/blade-bizMenuTable/bizMenuTable/export-bizMenuTable?${this.website.tokenHeader}=${getToken()}`;
      const {
        tableName,
        menuId,
      } = this.query;
      let values = {
        tableName_like: tableName,
        menuId_equal: menuId,
      };
      this.$confirm("是否导出数据?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        NProgress.start();
        exportBlob(downloadUrl, values).then(res => {
          downloadXls(res.data, `业务库表${dateNow()}.xlsx`);
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
      this.loading = true;

      const {
        tableName,
        menuId,
        tableType,
      } = this.query;

      let values = {
        tableName_like: tableName,
        menuId_equal: menuId,
        tableType_equal: tableType,
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


