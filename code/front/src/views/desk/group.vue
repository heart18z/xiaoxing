<template>
  <basic-container>
    <avue-crud :option="option"
               v-model:search="query"
               :table-loading="loading"
               :data="data"
               node-key="id"
               v-model:page="page"
               :permission="permissionList"
               :before-open="beforeOpen"
               v-model="form"
               ref="crud"
               @cell-click="rowView"
               @cell-dblclick="editList"
               @row-update="rowUpdate"
               @row-save="rowSave"
               @search-change="searchChange"
               @search-reset="searchReset"
               @selection-change="selectionChange"
               @current-change="currentChange"
               @size-change="sizeChange"
               @refresh-change="refreshChange"
               @tree-load="treeLoad"
               @on-load="onLoad">
      <!-- 左边按钮 -->
      <template #menu-left="{ row,index }">
        <el-button class="button-size" round title="新增" v-if="permission.group_add" size="small" icon="el-icon-plus"
                   @click.stop="addList()">
        </el-button>
        <el-button class="button-size" round title="删除" v-if="permission.group_delete" size="small"
                   icon="el-icon-delete"
                   @click.stop="handleDelete()">
        </el-button>
        <el-button class="button-size" round title="复制" v-if="permission.group_add" size="small"
                   icon="el-icon-document-copy" @click.stop="copyList()">
        </el-button>
        <el-button class="button-size" round title="新增用户" v-if="permission.group_add" size="small"
                   icon="el-icon-setting" @click.stop="addUserToGroup()">
        </el-button>
      </template>

      <!-- 打开附件列表 -->
      <template #uploadList="{ row }">
        <row-attach :source-id="row.id"></row-attach>
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
    <el-dialog
        draggable
        v-model="groupUserDialog"
        :title="title"
        :append-to-body="true"
        width="50%">
      <div style="display: flex">
        <el-card style="width: 20%;
        height: 300px;
        display: inline-block;}">
          <el-tree
              class="filter-tree"
              :data="treeData"
              :props="defaultProps"
              ref="tree"
              @node-click="handleNodeClick"
          >
          </el-tree>
        </el-card>
        <el-transfer
            :key="transferKey"
            ref="transferRef"
            style="text-align: left; display: inline-block"
            v-model="selectedGroupUser"
            filterable
            :titles="['可选用户', '已选择']"
            :button-texts="['', '']"
            :format="{
            noChecked: '${total}',
            hasChecked: '${checked}/${total}'
            }"
            :filter-method="transferFilter"
            :props="{key: 'userId', label:'name', disabled}"

            :data="allUser">
          <template #default="{ option }"> {{ option.name }}</template>

          <!--          <el-button class="transfer-footer" slot="left-footer" size="small">操作</el-button>-->
          <!--          <el-button class="transfer-footer" slot="right-footer" size="small">操作</el-button>-->
        </el-transfer>
      </div>
      <el-row>
        <el-col :span="24" style="float: right;width: 100%;text-align: right;">
          <!--            <el-button-->
          <!--                style="float: right;"-->
          <!--                title="提交"-->
          <!--                circle-->
          <!--                size="small"-->
          <!--                class="button-size"-->
          <!--                icon="el-icon-check"-->
          <!--                @click="handleSubmit()"-->
          <!--            ></el-button>-->
          <el-button icon="el-icon-check" size="small" circle @click="handleSubmit()" title="保存"></el-button>
          <el-button circle icon="el-icon-close" size="small" title="取消" @click="groupUserDialog = false"></el-button>
        </el-col>

      </el-row>

    </el-dialog>
  </basic-container>
</template>

<script>
import pageAttach from "@/components/attach-dialog/page-attach/main.vue"
import rowAttach from "@/components/attach-dialog/row-attach/main.vue"
import {
  getDetail,
  getTree,
  add,
  update,
  remove,
  getLazyTreeList,
  getGroupUserList,
  submitGroupUser
} from "@/api/desk/group/group";
import {mapGetters} from "vuex";
import {exportBlob} from "@/api/common";
import {getToken} from '@/utils/auth';
import {downloadXls} from "@/utils/util";
import {dateNow} from "@/utils/date";
import NProgress from 'nprogress';
import 'nprogress/nprogress.css';
import func from "@/utils/func";
import {getDeptTree} from "@/api/desk/notice/notice"

export default {
  components: {
    pageAttach, rowAttach
  },
  data() {
    return {
      selectTreeData: {
        id: ""
      },
      transferKey: new Date().getTime(),
      // 树数据
      treeData: [],
      defaultProps: {
        children: "children",
        label: "title",
      },
      title: "",
      selectedGroupUser: [],
      unSelectedGroupUser: [],
      allUser: [],
      groupUserDialog: false,
      form: {},
      query: {},
      search: {},
      loading: true,
      page: {
        pageSize: 30,
        currentPage: 1,
        total: 0
      },
      selectionList: [],
      option: {
        dialogDrag: true,
        lazy: true,
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
            prop: 'no',
            width: 90,
            display: false,
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
            label: "上级分组",
            prop: "parentId",
            type: "tree",
            dicMethod: "post",
            dicUrl: "/api/blade-group/group/tree",
            dicData: [],
            props: {
              label: "title",
            },
            value: 0,
            hide: true,
          },
          {
            label: "祖级列表",
            prop: "ancestors",
            type: "input",
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
          },
          {
            label: "分组名",
            prop: "groupName",
            type: "input",
            rules: [{
              required: true,
              message: "请输入分组名",
              trigger: "blur"
            }],
            search: true,
          },
          {
            label: "分组全称",
            prop: "groupFullName",
            type: "input",
            search: true,
          },
          {
            label: "排序",
            prop: "sort",
            type: "input",
          },
          {
            label: "备注",
            prop: "remark",
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
            label: '附件列表',
            span: 24,
            width: "100",
            prop: 'uploadList',
            slot: true,
            display: false,
          },
        ]
      },
      data: [],
      timer: null
    };
  },
  mounted() {
    this.initData();
  },
  computed: {
    ...mapGetters(["permission"]),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.group_add, false),
        viewBtn: this.validData(this.permission.group_view, false),
        delBtn: this.validData(this.permission.group_delete, false),
        editBtn: this.validData(this.permission.group_edit, false)
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
    transferFilter(query, item) {
      if (!this.selectedGroupUser.find(it => it === item.userId)) {
        // 左边的搜索框
        return item.name.indexOf(query) > -1 && item.deptId.includes(this.selectTreeData.id);
      }
      else {
        return item.name.indexOf(query) > -1;
      }
    },
    // 节点选择
    async handleNodeClick(row) {
      this.selectTreeData = row;
      // console.log(row.id)
      //this.$refs.transferRef.clearQuery('left')
      this.transferKey = new Date().getTime()
    },
    async initData() {
      const {data: {data = [], code} = {}} = await getDeptTree();
      // 设置全部
      const selectAllObj = {
        id: "",
        hasChildren: false,
        title: "全部",
      };
      this.treeData = code === 200 ? [selectAllObj, ...data] : [];
    },
    addUserToGroup() {
      if (this.selectionList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      }
      this.title = this.selectionList[0].groupName + "配置用户"
      this.groupUserDialog = true
      this.selectedGroupUser = []
      this.allUser = []
      getGroupUserList({groupId: this.selectionList[0].id}).then(res => {
        let users = res.data.data
        users = users.filter(item => {
          item.name = item.name + "(" + item.account + ")"
          return item
        })
        //console.log("res.data.data",res.data.data)
        this.allUser = users
        const list = users.filter(item => item.groupId == this.selectionList[0].id)
        this.selectedGroupUser = list.map(item => item.userId)
      })
    },
    handleSubmit() {
      // console.log("this.selectedGroupUser",this.selectedGroupUser)
      const data = {
        id: this.selectionList[0].id,
        userId: this.selectedGroupUser
      }
      submitGroupUser(data).then(() => {
        this.$message({
          type: "success",
          message: "操作成功!"
        });
        this.groupUserDialog = false
      })
    },
    addList() {
      this.$refs.crud.rowAdd()
      this.form = {}
      if (this.selectionList.length > 0) {
        this.form.parentId = this.selectionList[0].id
      }
      this.$refs.crud.dicInit()
    },
    copyList() {
      if (this.selectionList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      }
      this.$refs.crud.rowAdd()
      this.form = {}
      if (this.selectionList.length > 0) {
        this.form = this.selectionList[0]
        this.form.id = ""
      }
      this.$refs.crud.dicInit()
    },
    editList(row, {property}) {
      if (this.permission.group_edit && property === "no") {
        clearTimeout(this.timer);
        this.$refs.crud.dicInit()
        this.$refs.crud.rowEdit(row, row.$index)
      }
    },
    rowView(row, {property}) {
      if (property === "no") {
        clearTimeout(this.timer);          //首先要清除定时器
        this.timer = setTimeout(() => {
          this.$refs.crud.rowView(row, row.$index)
        }, 250)
      }
    },
    rowSave(row, done, loading) {
      func.isEmpty(row.parentId) ? row.parentId = 0 : row.parentId
      add(row).then(() => {
        this.onLoad(this.page);
        this.$message({
          type: "success",
          message: "操作成功!"
        });
        this.$refs.crud.refreshTable();
        done();
      }, error => {
        loading();
        window.console.log(error);
      });
    },
    rowUpdate(row, index, done, loading) {
      func.isEmpty(row.parentId) ? row.parentId = 0 : row.parentId
      update(row).then(() => {
        this.onLoad(this.page);
        this.$message({
          type: "success",
          message: "操作成功!"
        });
        this.$refs.crud.refreshTable();
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
      }).then(() => {
        return remove(row.id);
      }).then(() => {
        this.onLoad(this.page);
        this.$refs.crud.refreshTable();
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
            this.$refs.crud.refreshTable();
            this.$message({
              type: "success",
              message: "操作成功!"
            });
            this.$refs.crud.toggleSelection();
          });
    },
    handleExport() {
      let downloadUrl = `/api/blade-group/group/export-group?${this.website.tokenHeader}=${getToken()}`;
      const {
        groupName,
        groupFullName,
      } = this.query;
      let values = {
        groupName_: groupName,
        groupFullName_: groupFullName,
      };
      this.$confirm("是否导出数据?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        NProgress.start();
        exportBlob(downloadUrl, values).then(res => {
          downloadXls(res.data, `用户分组${dateNow()}.xlsx`);
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
        groupName,
        groupFullName,
      } = this.query;

      let values = {
        groupName_equal: groupName,
        groupFullName_equal: groupFullName,
      };

      getLazyTreeList(page.currentPage, page.pageSize, values).then(res => {
        this.data = res.data.data.records;
        let index = 1
        this.data.forEach(row => {
          row.no = index + ''
          index++
        })
        this.page.total = res.data.data.total
        this.loading = false;
        getTree().then(res => {
          const column = this.findObject(this.option.column, "parentId");
          column.dicData = res.data.data;
        });
      });
    },
    treeLoad(tree, treeNode, resolve) {
      let values = {
        parentId: tree.id
      };
      getLazyTreeList(1, 500, values).then(res => {
        let childList = []
        let index = 1
        res.data.data.records.forEach(row => {
          row.no = index + ''
          childList.push(row)
          index++
        })
        resolve(childList);
      });
    },
  }
};
</script>
