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
               @search-change="searchChange"
               @search-reset="searchReset"
               @selection-change="selectionChange"
               @current-change="currentChange"
               @size-change="sizeChange"
               @refresh-change="refreshChange"
               @on-load="onLoad">
      <!-- 左边按钮 -->
      <template #menu-left="{ row,index }">
        <el-button class="button-size" round title="新增" size="small" icon="el-icon-plus"
                   @click.stop="addList()">
        </el-button>
        <el-button class="button-size" round title="删除" size="small"
                   icon="el-icon-delete"
                   @click.stop="handleDelete()">
        </el-button>
        <el-button class="button-size" round title="复制" size="small"
                   icon="el-icon-document-copy" @click.stop="copyList()">
        </el-button>
      </template>

      <template #peopleGroupChildrenType="{ item,value,label }">
        <span style="float: left">{{ item.realName }}</span>
        <span style="float: right; color: #8492a6; font-size: 13px" v-if="item.foreignNo">{{ item.foreignNo }}</span>
      </template>

      <template slot="peopleGroupChildren" slot-scope="scope">
        <el-link type="primary" @click="checkUser(scope.row)">查看</el-link>
      </template>

      <template #peopleGroupChildrenForm="{ type,disabled }">
        <el-input
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4}"
          placeholder="请选择人员"
          v-model="form.userNames" @focus="choosePeople(form.peopleGroupChildren)">
        </el-input>
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
      append-to-body
      draggable
      destroy-on-close
      v-if="dialogDrag"
      title="人员列表"
      center
      class="avue-dialog avue-crud__dialog"
      v-model="dialogDrag" width="40%"
    >
      <el-input
        type="textarea"
        :autosize="{ minRows: 2}"
        readonly
        v-model="content">
      </el-input>

      <div class="avue-dialog__footer avue-dialog__footer--right">
        <el-button circle icon="el-icon-close" size="small" @click="closeDialogDrag()"
                   title="取消"></el-button>
      </div>

    </el-dialog>
    <el-dialog
      append-to-body
      draggable
      destroy-on-close
      v-if="dialogDrag2"
      title="人员列表"
      center
      class="avue-dialog avue-crud__dialog"
      v-model="dialogDrag2" width="50%"
    >
      <avue-crud :option="option2"
                 v-model:search="query2"
                 :table-loading="loading2"
                 :data="data2"
                 v-model:page="page2"
                 v-model="form2"
                 ref="crud2"
                 @search-change="searchChange2"
                 @search-reset="searchReset2"
                 @selection-change="selectionChange2"
                 @current-change="currentChange2"
                 @size-change="sizeChange2"
                 @refresh-change="refreshChange2"
                 @on-load="onLoad2"></avue-crud>

      <div class="avue-dialog__footer avue-dialog__footer--right">
        <el-button icon="el-icon-check" size="small" circle @click="submitPeople()" title="提交"></el-button>
        <el-button circle icon="el-icon-close" size="small" @click="closeDialogDrag2()"
                   title="取消"></el-button>
      </div>

    </el-dialog>

  </basic-container>
</template>

<script>
import pageAttach from "@/components/attach-dialog/page-attach/main.vue"
import rowAttach from "@/components/attach-dialog/row-attach/main.vue"
import selectLazy from "@/components/select-lazy/index.vue"
import {mapGetters} from "vuex";
import 'nprogress/nprogress.css';
import {
  existPeopleGroupName,
  getPeopleGroupDetail,
  getPeopleGroupListPage,
  removePeopleGroup,
  submitPeopleGroup
} from "@/api/system/peopleGroup";
import {getPeopleListByIds, getPeoplePageList} from "@/api/system/people";

export default {
  components: {
    pageAttach, rowAttach,selectLazy
  },
  data() {
    let validatePass = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请选输入用户组名'));
      } else {
        existPeopleGroupName(this.form).then(res => {
          if (res.data.data) {
            callback(new Error('用户组名已存在，请修改'));
          } else {
            callback();
          }
        })
      }
    };
    return {
      content: "",
      dialogDrag: false,
      dialogDrag2: false,
      peopleList: [],
      peopleListNum: 0,
      form: {
        userNames: ""
      },
      form2: {},
      query: {},
      query2: {},
      search: {},
      loading: true,
      loading2: true,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0
      },
      page2: {
        pageSize: 10,
        currentPage: 1,
        total: 0
      },
      selectionList: [],
      selectionList2: [],
      first: false,
      option: {
        searchIndex: 3,
        searchIcon: true,
        index: true,//表格前面的#是否展示
        indexLabel: '序号',//index属性为true即可，indexLabel设置表格的序号的标题,默认为#
        indexWidth: 70,
        saveBtn: false,//表格弹窗保存按钮
        dialogDrag: true,//弹窗是否可以拖拽
        updateBtn: false,//表格弹窗修改按钮
        cancelBtn: false,//表格弹窗取消按钮
        menu: false,//是否有操作栏
        addBtn: false,
        delBtn: false,
        editBtn: false,
        searchBtn: false,
        emptyBtn: false,
        searchShowBtn: false,
        tip: true,//多选提示
        border: true,
        selection: true,//是否有选择框
        dialogClickModal: true,//表格弹窗是否可以通过点击modal关闭
        align: 'center',
        labelWidth: 120,
        column: [
          {
            label: "用户组名",
            span: 12,
            prop: "groupName",
            type: "input",
            rules: [
              {
                required: true,
                message: "请选输入用户组名",
                trigger: "blur"
              },
              {validator: validatePass, trigger: 'blur'}
            ],
          },
          {
            label: "用户组名",
            span: 12,
            prop: "groupNameList",
            search: true,
            display: false,
            hide: true,
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-system/peopleGroup/peopleGroupNames",
          },
          {
            label: "用户组类型",
            prop: "groupType",
            type: "tree",
            searchLabelWidth: 120,
            search: true,
            span: 12,
            parent: false,

            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=yhzlx",
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },

            rules: [
              {
                required: true,
                message: "请选择用户组类型",
                trigger: "blur"
              }
            ],
          },
          {
            label: "成组说明",
            prop: "remark",
            span: 24,
            type: 'textarea',
            minRows: 2,
            maxRows: 5,
            rules: [
              {
                required: false,
                message: "请输入成组说明",
                trigger: "blur"
              }
            ],
          },
          {
            label: "组员人数",
            span: 12,
            prop: "userCount",
            display: false,
          },
          {
            label: "人员列表",
            prop: "peopleGroupChildren",
            span: 24,
            formslot: true,
            rules: [
              {
                required: false,
                message: "请选择人员",
                trigger: "blur"
              }
            ],
          },
          {
            label: "更新时间",
            prop: "updateTime",
            display: false,
          },
          {
            label: "更新人",
            prop: "updateUserName",
            display: false,
          },
        ]
      },
      option2: {
        reserveSelection: true,
        searchIndex: 3,
        searchIcon: true,
        index: true,//表格前面的#是否展示
        indexLabel: '序号',//index属性为true即可，indexLabel设置表格的序号的标题,默认为#
        indexWidth: 70,
        saveBtn: false,//表格弹窗保存按钮
        dialogDrag: true,//弹窗是否可以拖拽
        updateBtn: false,//表格弹窗修改按钮
        cancelBtn: false,//表格弹窗取消按钮
        menu: false,//是否有操作栏
        addBtn: false,
        delBtn: false,
        editBtn: false,
        searchShowBtn: false,
        tip: false,//多选提示
        border: true,
        searchMenuSpan:5,
        selection: true,//是否有选择框
        dialogClickModal: true,//表格弹窗是否可以通过点击modal关闭
        align: 'center',
        labelWidth: 120,
        column: [
          {
            label: "姓名",
            prop: "realName",
            search: true,
            searchSpan:9,
          },
          {
            label: "手机号",
            prop: "phone",
            search: true,
            searchSpan:9,
          },
          {
            label: "数据来源",
            prop: "isSync",
          },
          {
            label: "备注",
            prop: "remark",
            search: true,
            searchSpan:9,
          },
        ]
      },
      data: [],
      data2: [],
      selectUserIds: [],
      timer: null
    };
  },
  computed: {
    ...mapGetters(["permission"]),
    permissionList() {
      return {
        // addBtn: this.vaildData(this.permission.group_add, false),
        // viewBtn: this.vaildData(this.permission.group_view, false),
        // delBtn: this.vaildData(this.permission.group_delete, false),
        // editBtn: this.vaildData(this.permission.group_edit, false)
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
  watch: {
    'query': {
      handler( ) {
        this.onLoad(this.page);
      },
      immediate: false,
      deep: true
    },
  },
  methods: {
    choosePeople() {
      this.dialogDrag2 = true;
    },
    closeDialogDrag() {
      this.dialogDrag = false;
    },
    closeDialogDrag2() {
      this.dialogDrag2 = false;
    },
    submitPeople() {
      this.form.peopleGroupChildren = [...new Set(this.form.peopleGroupChildren)]
      getPeopleListByIds({ids: this.form.peopleGroupChildren }).then(res => {
        this.form.userNames = res.data.data.map(item2 => item2.realName).join(",")
        this.dialogDrag2 = false;
      })
    },
    checkUser(row) {
      this.content = row.userNames;
      this.dialogDrag = true;
    },
    addList() {
      this.$refs.crud.rowAdd()
      this.form = {}
      this.form.peopleGroupChildren = []
      this.$refs.crud.dicInit()
    },
    copyList() {
      if (this.selectionList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      }
      this.$refs.crud.rowAdd()
      getPeopleGroupDetail(this.selectionList[0].id).then(res => {
        this.form = JSON.parse(JSON.stringify(res.data.data));
        // this.form.peopleGroupChildren = this.form.peopleGroupChildren.split(",")
        this.form.id = ""
        this.$refs.crud.dicInit()
      });
    },
    editList(row, {property}) {
      if (!property) {
        clearTimeout(this.timer);
        this.$refs.crud.dicInit()
        this.$refs.crud.rowEdit(row, row.$index)
      }
    },
    rowView(row, {property}) {
      if (!property) {
        clearTimeout(this.timer);
        this.timer = setTimeout(() => {
          this.$refs.crud.rowView(row, row.$index)
        }, 250)
      }
    },
    rowSave(row, done, loading) {
      if (!Array.isArray(row.peopleGroupChildren)) {
        row.peopleGroupChildren = row.peopleGroupChildren.split(",")
      }
      submitPeopleGroup(row).then(() => {
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
      if (!Array.isArray(row.peopleGroupChildren)) {
        row.peopleGroupChildren = row.peopleGroupChildren.split(",")
      }
      submitPeopleGroup(row).then(() => {
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
          return removePeopleGroup(this.ids);
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
        getPeopleGroupDetail(this.form.id).then(res => {
          this.form = res.data.data;
          this.form2.userIds = this.form.peopleGroupChildren;
        });
      }
      done();
    },
    searchReset() {
      this.query = {};
      this.onLoad(this.page);
    },
    searchReset2() {
      this.query2 = {};
      this.onLoad2(this.page2);
    },
    searchChange(params, done) {
      this.query = params;
      this.page.currentPage = 1;
      this.onLoad(this.page);
      done();
    },
    searchChange2(params, done) {
      this.query2 = params;
      this.page2.currentPage = 1;
      this.onLoad2(this.page2);
      done();
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    getDifference(arr1, arr2) {
      const set2 = new Set(arr2);
      return arr1.filter(item => !set2.has(item));
    },
    selectionChange2(list) {
      if(this.first){
        let oldSelectionList = JSON.parse(JSON.stringify(this.selectionList2));
        if(oldSelectionList.length > list.length){
          let a = oldSelectionList.map(item=>item.id);
          let b = list.map(item=>item.id);
          let deleteArray = this.getDifference(a, b)
          deleteArray.forEach(item => {
            const index = this.form.peopleGroupChildren.findIndex(fl => fl === item)
            if (index != -1) {
              this.form.peopleGroupChildren.splice(index, 1);
            }
          });
        }else if(oldSelectionList.length < list.length){
          let a = list.map(item=>item.id);
          let b = oldSelectionList.map(item=>item.id);
          let addArray = this.getDifference(a, b)
          addArray.forEach(i=>{
            this.form.peopleGroupChildren.push(i)
          })
        }
      }
      this.selectionList2 = list;
    },
    selectionClear() {
      this.selectionList = [];
      this.$refs.crud.toggleSelection();
    },
    selectionClear2() {
      this.selectionList2 = [];
      this.$refs.crud2.toggleSelection();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
    },
    currentChange2(currentPage) {
      this.page2.currentPage = currentPage;
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
    },
    sizeChange2(pageSize) {
      this.page2.pageSize = pageSize;
    },
    refreshChange() {
      this.onLoad(this.page);
    },
    refreshChange2() {
      this.onLoad2(this.page2);
    },
    onLoad(page) {
      this.loading = true;
      getPeopleGroupListPage(page.currentPage, page.pageSize, this.query).then(res => {
        const data = res.data.data;
        this.page.total = data.total;
        this.data = data.records;
        this.loading = false;
        this.selectionClear();
      });
    },
    onLoad2(page) {
      this.loading2 = true;
      this.first = false;
      getPeoplePageList(page.currentPage, page.pageSize, this.query2).then(res => {
        const data = res.data.data;
        this.page2.total = data.total;
        this.data2 = data.records;
        this.loading2 = false;
        if (this.form.peopleGroupChildren.length > 0) {
          this.$refs.crud2.toggleSelection(this.data2.filter(item=>this.form.peopleGroupChildren.indexOf(item.id) != -1))
          this.first = true;
        }else {
          this.first = true;
          this.selectionClear2();

        }
      });
    }
  }
};
</script>


