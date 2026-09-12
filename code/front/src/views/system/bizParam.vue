<template>
  <basic-container>
    <avue-crud
        :option="optionParent"
        :table-loading="loading"
        :data="dataParent"
        :page="pageParent"
        v-model:search="query"
        ref="crud"
        v-model="formParent"
        :permission="permissionList"
        :before-open="beforeOpen"
        @row-del="rowDel"
        @row-update="rowUpdate"
        @row-save="rowSave"
        @search-change="searchChange"
        @search-reset="searchReset"
        @selection-change="selectionChange"
        @current-change="currentChange"
        @size-change="sizeChange"
        @refresh-change="refreshChange"
        @on-load="onLoadParent"
        @cell-click="rowView"
        @cell-dblclick="handleDbClick"
    >
      <template #menu-left="{ row,index }">
        <el-button class="button-size" round title="新增" v-if="permission.bizParam_add" size="small"
                   icon="el-icon-plus" @click.stop="addClick">
        </el-button>
        <el-button class="button-size" round title="删除" v-if="permission.bizParam_delete" size="small"
                   icon="el-icon-delete"
                   @click.stop="handleDelete()">
        </el-button>
        <el-button class="button-size" round title="复制" v-if="permission.bizParam_add" size="small"
                   icon="el-icon-document-copy" @click.stop="copyList()">
        </el-button>
<!--        <el-button class="button-size" round title="参数配置" v-if="permission.bizParam_edit" size="small"-->
<!--                   icon="el-icon-setting" @click.stop="handleRowClick()"></el-button>-->
      </template>
      <template #index="{row,index}">
        <div>{{index + 1}}</div>
      </template>

      <template #menu-form="{ row,index,type }">

        <template v-if="type !== 'view'">
        <el-button icon="el-icon-check" size="small" circle @click="$refs.crud.rowSave()" title="保存"></el-button>
        <el-button circle icon="el-icon-close" size="small" title="取消" @click="$refs.crud.closeDialog()"></el-button>
        </template>

      </template>

      <template #paramName="{  row  }">
        <el-tag @click="handleRowClick(row)" style="cursor: pointer">{{row.paramName}}</el-tag>
      </template>
      <template #status="{  row  }">
        <el-tag>{{row.status === 0 ? "否" : "是"}}</el-tag>
      </template>

      <template #isPublicParamHeader="{  column  }">
        <span class="titleHeader">{{ (column || {}).label }}</span>
        <el-tooltip
          effect="dark"
          content="初始来源：基础字典管理系统"
          placement="bottom">
          <el-icon class="el-icon-warning"></el-icon>
        </el-tooltip>
      </template>

      <template #systemSourceHeader="{  column  }">
        <span class="titleHeader">{{ (column || {}).label }}</span>
        <el-tooltip
          effect="dark"
          content="初始来源：基础字典管理系统"
          placement="bottom">
          <el-icon class="el-icon-warning"></el-icon>
        </el-tooltip>
      </template>
    </avue-crud>
    <!-- 左下按钮 -->
    <page-attach ref="pageAttach"></page-attach>
    <el-dialog
        :title="diaChildTitle"
        append-to-body
        v-model="box"
        draggable
        width="80%">
      <avue-crud
          :option="optionChild"
          :table-loading="loadingChild"
          :data="dataChild"
          ref="crudChild"
          v-model="formChild"
          v-model:search="queryChild"
          :permission="permissionList"
          :before-open="beforeOpenChild"
          :before-close="beforeCloseChild"
          @row-del="rowDelChild"
          @row-update="rowUpdateChild"
          @row-save="rowSaveChild"
          @search-change="searchChangeChild"
          @search-reset="searchResetChild"
          @selection-change="selectionChangeChild"
          @current-change="currentChangeChild"
          @size-change="sizeChangeChild"
          @refresh-change="refreshChangeChild"
          @on-load="onLoadChild"
          @cell-click="rowViewChild"
          @cell-dblclick="editChildClick">

        <template #menu-left="{ row,index }" v-if="!diaSearch">
          <el-button class="button-size" round title="新增" v-if="permission.bizParamChild_add" size="small"
                     icon="el-icon-plus" @click.stop="addChildClick">
          </el-button>
          <el-button class="button-size" round title="删除" v-if="permission.bizParamChild_del" size="small"
                     icon="el-icon-delete"
                     @click.stop="handleDeleteChild()">
          </el-button>
          <el-button class="button-size" round title="复制" v-if="permission.bizParamChild_add" size="small"
                     icon="el-icon-document-copy" @click.stop="copyChildList()">
          </el-button>
<!--          <el-button class="button-size" round title="参数配置" v-if="permission.bizParamChild_add" size="small"-->
<!--                     icon="el-icon-setting" @click.stop="handleAdd()"></el-button>-->
        </template>
        <template #paramValueForm="{  type  }">
          <el-input :disabled="['view', 'edit'].indexOf(type) !== -1" v-model="formChild.paramValue">
            <template #prepend>{{defaultPrefix}}</template>
          </el-input>
        </template>
        <!--        <template #index="{row,index}" >-->
        <!--          <span>{{index+1}}</span>-->
        <!--        </template>-->
        <template #status="{  row  }">
          <el-tag>{{row.status === 0 ? "否" : "是"}}</el-tag>
        </template>
      </avue-crud>
    </el-dialog>

  </basic-container>
</template>


<script>
import {
  getParentList,
  getChildList,
  remove,
  update,
  add,
  getBizParam,
  getBizParamTree,
} from "@/api/system/bizParam";
import {optionParent, optionChild} from "@/option/system/bizParam";
import {mapGetters} from "vuex";
import {deepClone} from "@/utils/util";
import pageAttach from "@/components/attach-dialog/page-attach/main.vue"
import func from "@/utils/func";
// import func from "@/utils/func";
export default {
  components: {pageAttach},
  data() {
    return {
      diaSearch: false,
      diaChildTitle: "",
      timer: null,
      query: {
        paramName: "",
        paramValue: "",
        isPublicParam:"",
        isSync: "",
        systemSource: ""
      },
      queryChild: {
        paramName: "",
        paramValue: "",
        isPublicParam:"",
        isSync: "",
        systemSource: ""
      },
      curRow: {},
      paramName: "暂无",
      parentId: -1,
      paramKey: "",
      hierarchical: "",
      currentHierarchy: "",
      formParent: {
        createUsername: '',
      },
      formChild: {
        paramValue: '',
      },
      selectionList: [],
      selectionChildList: [],

      box: false,
      loading: true,
      loadingChild: true,
      pageParent: {
        pageSize: 30,
        pageSizes: [10, 30, 50, 100, 200],
        currentPage: 1,
        total: 0,
      },
      pageChild: {
        pageSize: 30,
        pageSizes: [10, 30, 50, 100, 200],
        currentPage: 1,
        total: 0,
      },
      dataParent: [],
      dataChild: [],
      optionParent: optionParent,
      optionChild: optionChild,
      creatorInfos: {},
      defaultPrefix: '',
    };
  },
  computed: {
    ...mapGetters(["permission", "userInfo"]),
    permissionList() {
      return {
        delBtn: this.vaildData(this.permission.bizParam_delete, false),
        delChildBtn: this.vaildData(this.permission.bizParamChild_del, false),
        editBtn: this.vaildData(this.permission.bizParam_edit, false),
        viewBtn: false,
      };
    },
    ids() {
      let ids = [];
      this.selectionList.forEach((ele) => {
        ids.push(ele.id);
      });
      return ids.join(",");
    },
    childIds() {
      let childIds = [];
      this.selectionChildList.forEach((ele) => {
        childIds.push(ele.id);
      });
      return childIds.join(",");
    }
  },
  mounted() {
    this.initData();
  },
  created() {
    // assign(this)
  },
  methods: {
    initData() {
      getBizParamTree().then((res) => {
        const column = this.findObject(this.optionChild.column, "parentId");
        column.dicData = res.data.data;
      });
    },
    handleAdd(row) {
      //console.log("add", this.selectionChildList,row)
      if (row == undefined && this.selectionChildList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      } else if (row == undefined && this.selectionChildList.length !== 0) {
        row = this.selectionChildList[0]
      }
      if (row.currentHierarchy >= row.hierarchical) {
        this.$message({message: "层级限制不能超过 " + row.hierarchical + "层", type: "warning",});
      } else {
        this.formChild.paramValue = "";
        this.formChild.paramName = "";
        this.formChild.sort = 0;
        this.formChild.status = 1;
        this.formChild.remark = "";
        this.formChild.currentHierarchy = row.currentHierarchy + 1;
        this.formChild.hierarchical = row.hierarchical;
        this.formChild.parentId = row.id;
        this.defaultPrefix = row.paramValue + "_";
        this.$refs.crudChild.rowAdd();
      }
    },
    rowSave(row, done, loading) {
      const form = {...row, paramValue: row.paramKey};
      add(form).then(() => {
        this.onLoadParent(this.pageParent);
        this.$message({type: "success", message: "操作成功!"});
        done();
      }, (error) => {
        window.console.log(error);
        loading();
      });
    },
    rowUpdate(row, index, done, loading) {
      if (row.hierarchical < row.maxHierarchy) {
        this.$message({
          message: "层级限制(" + row.hierarchical + ")不能小于当前层级(" + row.maxHierarchy + ")",
          type: "warning"
        });
        loading();
      } else {
        update(row).then(() => {
          this.onLoadParent(this.pageParent);
          this.$message({
            type: "success",
            message: "操作成功!",
          });
          this.onLoadChild(this.pageChild);
          done();
        }, (error) => {
          window.console.log(error);
          loading();
        });
      }
    },
    rowDel(row) {
      this.$confirm("确定将选择数据删除?", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        return remove(row.id);
      }).then(() => {
        this.onLoadParent(this.pageParent);
        this.$message({type: "success", message: "操作成功!"});
      });
    },
    handleDbClick(row, {property}) {
      if (!this.userInfo.role_name.includes('admin')) {
        this.$message.warning("只有管理员可以维护应用字典！");
        return;
      }

      // if (row.account == "admin" && (this.userInfo.account != "admin" )) {
      //   this.$message.warning("admin用户创建的字典，必须由admin用户进行修改！");
      //   return;
      // }


      // console.log("row",row)
      if (property === "index") {
        clearTimeout(this.timer);
        if (this.permission.bizParam_edit) {
          this.$refs.crud.rowEdit(row, row.$index)
        }
      }
    },
    openSearchClick() {
      this.queryChild.paramName = this.query.paramName
      this.queryChild.paramValue = this.query.paramValue
      this.queryChild.isPublicParam = this.query.isPublicParam
      this.queryChild.systemSource = this.query.systemSource
      this.queryChild.isSync = this.query.isSync

      this.box = true;
      this.diaChildTitle = "参数查找"
      this.parentId= -1
      this.diaSearch = true
      this.refreshChangeChild()
      // this.optionChild.leftHeader=false
    },

    handleRowClick(row) {
      this.searchReset()
      this.queryChild.paramName = ""
      this.optionChild.header=true
      if (row == undefined && this.selectionList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      } else if (row == undefined && this.selectionList.length !== 0) {
        row = this.selectionList[0]
      }


      if (!this.userInfo.role_name.includes('admin')) {
        this.$message.warning("只有管理员可以维护应用字典！");
        return;
      }

      // if (row.account == "admin" && (this.userInfo.account != "admin" )) {
      //   this.$message.warning("admin用户创建的字典，必须由admin用户进行修改！");
      //   return;
      // }

      // this.query = {};
      this.parentId = row.id;
      this.paramName = row.paramName;
      this.paramKey = row.paramKey;
      this.hierarchical = row.hierarchical;
      this.currentHierarchy = row.currentHierarchy + 1;
      const paramKey = this.findObject(this.optionChild.column, "paramKey");
      paramKey.value = row.paramKey;
      this.formChild.paramKey = row.paramKey;


      const parentId = this.findObject(this.optionChild.column, "parentId");
      parentId.value = row.id;
      this.formChild.parentId = row.id;

      const hierarchical = this.findObject(this.optionChild.column, "hierarchical");
      hierarchical.value = row.hierarchical;
      this.formChild.hierarchical = row.hierarchical;

      const currentHierarchy = this.findObject(this.optionChild.column, "currentHierarchy");
      currentHierarchy.value = row.currentHierarchy + 1;
      this.formChild.currentHierarchy = row.currentHierarchy + 1;

      this.diaChildTitle = this.paramName+ "参数配置"
      this.diaSearch = false
      this.box = true;
      this.onLoadChild(this.pageChild);
    },
    searchReset() {
      this.query = {
        paramName: "",
        paramKey: ""
      };
      this.query["paramName"] = ""
      this.onLoadParent(this.pageParent);
    },
    searchChange(params, done) {
      this.query = params;
      this.pageParent.currentPage = 1;
      this.onLoadParent(this.pageParent, params);
      done();
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    selectionClear() {
      this.selectionList = [];
      this.$refs.crud.toggleSelection();
    },
    addClick() {
      if (!this.userInfo.role_name.includes('admin')) {
        this.$message.warning("只有管理员可以维护应用字典！");
        return;
      }

      if (this.selectionList.length >0) {
        this.handleRowClick()
      }else {
        this.$refs['crud'].rowAdd();
      }
    },
    copyList() {
      if (!this.userInfo.role_name.includes('admin')) {
        this.$message.warning("只有管理员可以维护应用字典！");
        return;
      }
      if (this.selectionList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      }
      this.$refs.crud.rowAdd()
      this.formParent = {}
      if (this.selectionList.length > 0) {
        this.formParent = deepClone(this.selectionList[0])
        this.formParent.id = ""
      }
    },
    editClick(row) {
      this.$refs['crud'].rowEdit({...row, paramValue: ''});
    },
    copyChildList() {
      if (this.selectionChildList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      }
      this.$refs['crudChild'].rowAdd();
      this.formChild = {}
      if (this.selectionChildList.length > 0) {
        this.formChild = deepClone(this.selectionChildList[0])
        this.formChild.id = ""
        const index = this.formChild.paramValue.lastIndexOf("_");
        this.formChild.paramValue = this.formChild.paramValue.substring(index + 1, this.formChild.paramValue.length);

      }
      this.defaultPrefix = this.paramKey + '_';

    },
    addChildClick() {

      if (this.selectionChildList.length>0) {
        this.handleAdd()
      } else {

        this.defaultPrefix = this.paramKey + '_';
        this.$refs['crudChild'].rowAdd();
      }
    },
    editChildClick(row, {property}) {
      if (property === "no") {
        clearTimeout(this.timer);
        if (this.permission.bizParamChild_add) {
          const tmpRow = {...row};
          this.$refs['crudChild'].rowEdit(tmpRow);
        }
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
    rowViewChild(row, {property}) {
      if (property === "index") {
        clearTimeout(this.timer);          //首先要清除定时器
        this.timer = setTimeout(() => {
          this.$refs.crudChild.rowView(row, row.$index)
        }, 250)
      }
    },
    handleDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      }

      if (!this.userInfo.role_name.includes('admin')) {
        this.$message.warning("只有管理员可以维护应用字典！");
        return;
      }
      this.$confirm("确定将选择数据删除?", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        return remove(this.ids);
      }).then(() => {
        this.onLoadParent(this.pageParent);
        this.$message({type: "success", message: "操作成功!"});
        this.$refs.crud.toggleSelection();
      });
    },
    beforeOpen(done, type) {
      if (["edit", "view"].includes(type)) {
        getBizParam(this.formParent.id).then((res) => {
          this.formParent = res.data.data;
        });
      }
      done();
    },
    currentChange(currentPage) {
      this.pageParent.currentPage = currentPage;
    },
    sizeChange(pageSize) {
      this.pageParent.pageSize = pageSize;
    },
    refreshChange() {
      if (
        func.notEmpty(this.query.paramName ) ||
        func.notEmpty(this.query.paramValue) ||
        func.notEmpty(this.query.isPublicParam) ||
        func.notEmpty(this.query.systemSource) ||
        func.notEmpty(this.query.isSync)
      ) {
        this.openSearchClick()
        this.onLoadParent(this.pageParent, this.query);
      }
      else {
        this.onLoadParent(this.pageParent, this.query);
      }
    },
    rowSaveChild(row, done, loading) {
      add({...row, paramValue: this.defaultPrefix + row.paramValue}).then(() => {
        this.onLoadChild(this.pageChild);
        this.$message({type: "success", message: "操作成功!"});
        done();
      }, (error) => {
        window.console.log(error);
        loading();
      });
    },
    rowUpdateChild(row, index, done, loading) {
      update({...row, paramValue: this.defaultPrefix + row.paramValue}).then(() => {
        this.onLoadChild(this.pageChild);
        this.$message({type: "success", message: "操作成功!"});
        done();
      }, (error) => {
        window.console.log(error);
        loading();
      });
    },
    rowDelChild(row) {
      this.$confirm("确定将选择数据删除?", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        return remove(row.id);
      }).then(() => {
        this.onLoadChild(this.pageChild);
        this.$message({type: "success", message: "操作成功!"});
      });
    },
    searchResetChild() {
      this.queryChild = {};
      this.onLoadChild(this.pageChild);
    },
    searchChangeChild(params, done) {
      this.queryChild = params;
      this.pageChild.currentPage = 1;
      this.onLoadChild(this.pageChild, params);
      done();
    },
    selectionChangeChild(list) {
      this.selectionChildList = list;
    },
    selectionClearChild() {
      this.selectionList = [];
      this.$refs.crudChild.toggleSelection();
    },
    handleDeleteChild() {
      if (this.selectionChildList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      }
      this.$confirm("确定将选择数据删除?", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        return remove(this.childIds);
      }).then(() => {
        this.onLoadChild(this.pageChild);
        this.$message({type: "success", message: "操作成功!"});
        this.$refs.crudChild.toggleSelection();
      });
    },
    beforeOpenChild(done, type) {
      if (["add", "edit"].includes(type)) {
        this.initData();
      }
      if (["edit", "view"].includes(type)) {
        getBizParam(this.formChild.id).then(({data}) => {
          this.defaultPrefix = '';
          if (data.data.paramValue) {
            let spereIndex = data.data.paramValue.lastIndexOf("_");
            if (spereIndex !== -1) {
              this.defaultPrefix = data.data.paramValue.substring(0, spereIndex + 1);
              data.data.paramValue = data.data.paramValue.substring(spereIndex + 1);
            }
          }
          this.formChild = data.data;


        });


      }
      done();
    },

    beforeCloseChild(done) {
      this.$refs.crudChild.value.parentId = this.parentId;
      this.$refs.crudChild.option.column.filter((item) => {
        if (item.prop === "parentId") {
          item.value = this.parentId;
        }
      });
      done();
    },
    currentChangeChild(currentPage) {
      this.pageChild.currentPage = currentPage;
    },
    sizeChangeChild(pageSize) {
      this.pageChild.pageSize = pageSize;
    },
    refreshChangeChild() {
      this.onLoadChild(this.pageChild, this.queryChild);
    },
    onLoadParent(page, params = {}) {
      this.loading = true;
      getParentList(
          page.currentPage,
          page.pageSize,
          Object.assign(params, this.query)
      ).then((res) => {
        const data = res.data.data;
        this.pageParent.total = data.total;
        this.dataParent = data.records;
        this.loading = false;
        this.selectionClear();
      })
    },
    onLoadChild(page) {
      this.loadingChild = true;
      const param = {
        paramName: this.queryChild.paramName,
        paramValue: this.queryChild.paramValue,
        isPublicParam: this.queryChild.isPublicParam,
        isSync: this.queryChild.isSync,
        systemSource: this.queryChild.systemSource
      }

      getChildList(
        page.currentPage,
        page.pageSize,
        this.parentId,
        param
      ).then((res) => {
        this.dataChild = res.data.data;
        this.loadingChild = false;
        this.selectionClear();
        const tf = (data) => {
          let i = 1;
          for (let child of data) {
            child.no = i++
            if (child.children && child.children.length) {
              tf(child.children);
            }
          }
        }
        tf(this.dataChild)
      })
    },
    deepTreeToPath(tree, property, value) {
      let s = [];
      const deep = (nodes) => {
        for (let node of nodes) {
          s.push(node);
          if (node[property] === value) {
            return true;
          }
          if (node.children && node.children.length) {
            let found = deep(node.children);
            if (found) {
              return true;
            }
          }
          s.pop();
        }
      }
      deep(tree);
      return s;
    },
  },
};
</script>

