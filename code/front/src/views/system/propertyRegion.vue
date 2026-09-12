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
            <el-button class="button-size" round title="新增" v-if="permission.propertyRegion_add" size="small" icon="el-icon-plus" @click.stop="addList()">
            </el-button>
            <el-button class="button-size" round title="删除" v-if="permission.propertyRegion_delete" size="small" icon="el-icon-delete"
                @click.stop="handleDelete()">
            </el-button>
            <el-button class="button-size" round title="复制" v-if="permission.propertyRegion_add" size="small" icon="el-icon-document-copy" @click.stop="copyList()">
            </el-button>

<!--          <el-button class="button-size" round title="数据来源"  size="small"-->
<!--                     icon="el-icon-info" ></el-button>-->
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
  import {getList, getDetail, add, update, remove, doSync} from "@/api/system/propertyRegion";
  import {mapGetters} from "vuex";

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
              label: "ID",
              prop: "id",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "物业号",
              prop: "propertyCode",
              search: true,
              type: "input",
              rules: [{
                required: true,
                message: "请输入物业号",
                trigger: "blur"
              }]
            },
            {
              label: "物业名称",
              prop: "propertyName",
              type: "input",
              rules: [{
                required: true,
                message: "请输入物业名称",
                trigger: "blur"
              }]
            },
            // {
            //   label: "管理人",
            //   prop: "admin",
            //   type: "input",
            //   addDisplay: false,
            //   editDisplay: false,
            //   viewDisplay: false,
            //   hide: true,
            // },
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
          addBtn: this.vaildData(this.permission.propertyRegion_add, false),
          viewBtn: this.vaildData(this.permission.propertyRegion_view, false),
          delBtn: this.vaildData(this.permission.propertyRegion_delete, false),
          editBtn: this.vaildData(this.permission.propertyRegion_edit, false)
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
          isSync:"add"
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
        this.$refs.crud.dicInit()
      },
      editList(row,{property}) {
        if (this.permission.propertyRegion_edit && property === "index") {
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
          propertyCode
        } = this.query;

        let values = {
          propertyCode:propertyCode
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


