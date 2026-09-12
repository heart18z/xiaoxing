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
            <el-button class="button-size" round title="新增" v-if="permission.note_add" size="small" icon="el-icon-plus" @click.stop="addList()">
            </el-button>
            <el-button class="button-size" round title="删除" v-if="permission.note_delete" size="small" icon="el-icon-delete"
                @click.stop="handleDelete()">
            </el-button>
            <el-button class="button-size" round title="复制" v-if="permission.note_add" size="small" icon="el-icon-document-copy" @click.stop="copyList()">
            </el-button>
        </template>

        <!-- 打开附件列表 -->
        <template #uploadList="{ row }">
            <row-attach :source-id="row.id"></row-attach>
        </template>

        <template #index="{row,index}">
            <div>{{index+1}}</div>
        </template>


      <template #icon="{ row }">
        <div style="text-align:center;font-size: 30px">
          <i :class="row.icon"/>
        </div>
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
  import {getList, getDetail, add, update, remove} from "@/api/system/note";
  // import option from "@/option/system/note";
  import {mapGetters} from "vuex";
  import {exportBlob} from "@/api/common";
  import {getToken} from '@/utils/auth';
  import {downloadXls} from "@/utils/util";
  import {dateNow} from "@/utils/date";
  import NProgress from 'nprogress';
  import 'nprogress/nprogress.css';
  import iconList from "@/config/iconList";
  import func from "@/utils/func";
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
              label: "编号",
              prop: "id",
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
      //       {
      //         label: "租户ID",
      //         prop: 'tenantId',
      //         hide: true, display: false, addDisplay: false, editDisplay: false, viewDisplay: false, search: false,
      // hide: true, display: false, addDisplay: false, editDisplay: false, viewDisplay: false, search: false,
      //         type: "input",
      //         addDisplay: false,
      //         editDisplay: false,
      //         viewDisplay: false,
      //         hide: true,
      //       },
            // {
            //   label: "关联菜单",
            //   prop: "menuId",
            //   type: "tree",
            // dicMethod: "post",
            //   dicUrl: "/api/blade-system/menu/grant-top-tree",
            //   search: true,
            //   dicFormatter: (res) => {
            //     return res.data.menu
            //   },
            //   props: {
            //     label: 'title',
            //     value: 'id',
            //     res: 'id'
            //   },
            //   rules: [{
            //     required: true,
            //     message: "请选择关联菜单",
            //     trigger: "blur"
            //   }],
            //   typeformat: this.fmt,
            //   formatter: this.tableFmt
            // },
            {
              label: "注释名称",
              prop: "name",
              type: "input",
              search: true,
            },
            {
              label: "注释类型",
              prop: "type",
              type: "select",
              dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=note_type",
              props: {
                label: 'dictValue',
                value: 'dictKey'
              },
              search: true,
              change: this.typeChange
            },
            // {
            //   label: "注释参数",
            //   prop: "params",
            //   type: "input",
            // },

            {
              label: "图标",
              prop: "icon",
              type: "icon",
              width: 50,
              slot: true,
              disabled:true,
              iconList: iconList,
            },
            {
              label: "注释内容",
              prop: "content",
              type: "textarea",
              width: 300,
              span: 24,
              minRows: 6,
            },
            {
              label: "备注",
              prop: "remark",
              type: "input",
            },
            {
              label: "创建时间",
              prop: "createTime",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
            },
            {
              label: "创建人",
              prop: "createUser",
              type: "select",
              dicMethod: "post",
            dicUrl: "/api/blade-system/user/user-kv-list",
              props: {
                label: 'key',
                value: 'value',
              },
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
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
          addBtn: this.vaildData(this.permission.note_add, false),
          viewBtn: this.vaildData(this.permission.note_view, false),
          delBtn: this.vaildData(this.permission.note_delete, false),
          editBtn: this.vaildData(this.permission.note_edit, false)
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
      typeChange(data) {
        if (data.value === "note_sync") {
          this.form.icon= "el-icon-warning"
        } else if (data.value === "note_meaning") {
          this.form.icon= "el-icon-question"
        }
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
        if (this.permission.note_edit && property === "index") {
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
      handleExport() {
        let downloadUrl = `/api/blade-note/note/export-note?${this.website.tokenHeader}=${getToken()}`;
        const {
            name,
            type,
        } = this.query;
        let values = {
            name_: name,
            type_: type,
        };
        this.$confirm("是否导出数据?", "提示", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }).then(() => {
          NProgress.start();
          exportBlob(downloadUrl, values).then(res => {
            downloadXls(res.data, `注释${dateNow()}.xlsx`);
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
          name,
          type,
        } = this.query;

        let values = {
           name_equal: name,
           type_equal: type,
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


