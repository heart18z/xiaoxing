<template>
  <basic-container>
      <avue-crud :option="option"
                 v-model:search="search"
                 :table-loading="loading"
                 :data="data"
                 node-key="id"
                 v-model:page="page"
                 :permission="permissionList"
                 highlight-current
                 :before-open="beforeOpen"
                 v-model="form"
                 ref="crud"
                 @row-update="rowUpdate"
                 @row-save="rowSave"
                 @search-change="searchChange"
                 @search-reset="searchReset"
                 @selection-change="selectionChange"
                 @current-change="currentChange"
                 @size-change="sizeChange"
                 @refresh-change="refreshChange"
                 @tree-load="treeLoad"
                 @on-load="onLoad"
                 @cell-click="rowView"
                 @cell-dblclick="editList"
      >
        <!-- 左边按钮 -->
        <template #menu-left="{ row,index }">
          <el-button class="button-size" round title="新增" v-if="permission.standard_add" size="small" icon="el-icon-plus" @click.stop="addList()">
          </el-button>
          <el-button class="button-size" round title="删除" v-if="permission.standard_delete" size="small" icon="el-icon-delete"
                     @click.stop="handleDelete()">
          </el-button>

          <message-build-button :selection-list="selectionList"/>
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
  </basic-container>
</template>

<script>
import BizParam, {  getLabel } from "@/components/bizParam/index.vue";
// import Constant from "@/constant/BizParam";
import pageAttach from "@/components/attach-dialog/page-attach/main.vue"
import rowAttach from "@/components/attach-dialog/row-attach/main.vue"
import {getDetail, add, update, remove, getLazyTreeList} from "@/api/demo/standard/standard";
import option  from "@/option/demo/standard/standard";
import {mapGetters} from "vuex";
import 'nprogress/nprogress.css';
import messageBuildButton from "@/components/message-build/menu-button/main.vue";

export default {
  components: {
    pageAttach, rowAttach,BizParam,messageBuildButton
  },
  data() {
    return {
      nodeMaps: new Map(),
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
      option: option,
      data: [],
      // pubModeParent: {},
      // pubModeOptions: [],
      timer: null
    };
  },
  computed: {
    ...mapGetters(["permission","userInfo"]),
    permissionList() {
      return {
        addBtn: this.vaildData(this.permission.standard_add, false),
        viewBtn: this.vaildData(this.permission.standard_view, false),
        delBtn: this.vaildData(this.permission.standard_delete, false),
        editBtn: this.vaildData(this.permission.standard_edit, false)
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
  mounted() {
  },
  methods: {
    getLabel,
    // init() {
      // loadParams(Constant.PUBMODE).then(({ parent, options }) => {
      //   this.pubModeParent = parent;
      //   this.pubModeOptions = options;
      //   // console.log( this.pubModeParent ,this.pubModeOptions)
      // }).catch(err=> {
      //   console.error(err);
      // })
    // },
    //新增
    addList() {
      this.$refs.crud.rowAdd()
      this.form = {}
      console.log("this.form.parentId==>",this.form.parentId)
      if (this.selectionList.length > 0) {
        this.form.parentId = this.selectionList[0].id
      }
      this.$refs.crud.dicInit()
    },
    editList(row, {property}) {
      if (property === "no") {
        clearTimeout(this.timer);
        if (this.permission.standard_edit) {
          this.$refs.crud.dicInit()
          this.$refs.crud.rowEdit(row, row.$index)
        }
      }
    },
    rowView(row, {property}) {
      if (property === "no") {
        clearTimeout(this.timer);          //首先要清除定时器
        this.timer = setTimeout(() =>{
          this.$refs.crud.rowView(row, row.$index)
        },250)
      }
    },
    rowSave(row, done, loading) {
      add(row).then(() => {
        this.onLoad(this.page);
        this.updateTable()
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
        this.updateTable();
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
    handleDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning("请选择至少一条数据!");
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
          this.updateTable();
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
      //this.$refs.noteSetting.initFormNote()
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
      params.parentId = 0

      if (this.$route.query.fromId) {
        params.id = this.$route.query.fromId
      }

      getLazyTreeList(params).then(res => {
        const data = res.data.data;
        // this.page.total = data.total;
        this.data = data;
        let index = 1
        this.data.forEach(row => {
          row.no = index + ''
          index++
        })
        this.loading = false;
        this.selectionClear();
      });
    },
    // 树节点加载
    treeLoad(tree, treeNode, resolve) {
      const params = {parentId: tree.id}
      getLazyTreeList(params).then(res => {
        // let childList =res.data.data
        let childList = []
        let index = 1
        res.data.data.forEach(row => {
          row.no = index + ''
          childList.push(row)
          index++
        })
        this.nodeMaps.set(tree.id, {tree, treeNode, resolve}) // 将当前选中节点数据存储到maps中
        resolve(childList);
      });
    },
    updateTable() { // 在删除或者添加操作成功之后，调用此函数
      this.nodeMaps.forEach((item, key) => {
        const {tree, treeNode, resolve} = this.nodeMaps.get(key)
        this.treeLoad(tree, treeNode, resolve)
      })
    }
  }
};
</script>


