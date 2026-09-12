<template>
  <div>
    <el-dialog :title="'定时任务日志'"
               v-model="showDialog" draggable
               :append-to-body="true" :modal='false' width="70%"
    >
      <basic-container>
        <avue-crud :option="option"
                   v-model:search="query"
                   :table-loading="loading"
                   :data="data"
                   v-model:page="page"
                   :before-open="beforeOpen"
                   v-model="form"
                   ref="crud"
                   @cell-click="rowView"
                   @row-update="rowUpdate"
                   @row-save="rowSave"
                   @row-del="rowDel"
                   @search-change="searchChange"
                   @search-reset="searchReset"
                   @selection-change="selectionChange"
                   @current-change="currentChange"
                   @size-change="sizeChange"
                   @refresh-change="refreshChange"
        >
          <!-- 左边按钮 -->
          <template #menu-left="{ row,index }">

            <el-button class="button-size" round title="删除"  size="small" icon="el-icon-delete"
                       @click.stop="handleDelete()">
            </el-button>

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
      </basic-container>
    </el-dialog>
  </div>

</template>

<script>
  import {getList, getDetail, add, update, remove} from "@/api/quartz/jobLog";
  export default {
    data() {
      return {
        jobId:"",
        showDialog:false,
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
          showOverflowTooltip:true,
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
              label: "日志ID",
              prop: "jobLogId",
              type: "input",
              width: 80
            },
            {
              label: "任务id",
              prop: "jobId",
              type: "input",
              hide: true
            },
            {
              label: "任务名称",
              prop: "jobName",
              type: "input",
              width: 100
              // search: true,
            },
            {
              label: "任务组名",
              prop: "jobGroup",
              width: 80,
              type: "input",
            },
            {
              label: "调用目标字符串",
              prop: "invokeTarget",
              width: 120,
              type: "input",
              // search: true,
            },
            {
              label: "日志信息",
              prop: "jobMessage",
              overHidden:true,
              type: "input",
            },
            {
              label: "执行状态",
              prop: "status",
              width: 70,
              type: "select",
              dicData:[
                {value: "0",label: "正常"},
                {value: "1",label: "失败"}
              ]
            },
            {
              label: "异常信息",
              prop: "exceptionInfo",
              // width: 180,
              overHidden:true,
              type: "input",
            },
            {
              label: "执行时间",
              prop: "startTime",
              width: 150,
              type: "input",
              // addDisplay: false,
              // editDisplay: false,
              // viewDisplay: false,
              // hide: true,
            },
            // {
            //   label: '附件列表',
            //   span: 24,
            //   width: "100",
            //   prop: 'uploadList',
            //   slot: true,
            //   display: false,
            // },
          ]
        }
        ,
        data: [],
        timer: null
      };
    },
    computed: {

      ids() {
        let ids = [];
        this.selectionList.forEach(ele => {
          ids.push(ele.jobLogId);
        });
        return ids.join(",");
      }
    },
    methods: {
      openDialog(jobId) {
        this.showDialog = true
        this.jobId =jobId
        this.data = []
        this.onLoad(this.page)
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
        this.form.jobLogId = ""
        this.$refs.crud.dicInit()
      },
      editList(row,{property}) {
        if (this.permission.jobLog_edit && property === "index") {
            clearTimeout(this.timer);
            this.$refs.crud.dicInit()
            this.$refs.crud.rowEdit(row, row.$index)
        }
      },
      rowView(row, {property}) {
        if (property === "index") {
          this.$refs.crud.rowView(row, row.$index)
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
            return remove(row.jobLogId);
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
          getDetail(this.form.jobLogId).then(res => {
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
        this.onLoad(this.page, this.query);
      },
      sizeChange(pageSize){
        this.page.pageSize = pageSize;
        this.onLoad(this.page, this.query);
      },
      refreshChange() {
        this.onLoad(this.page, this.query);
      },
      onLoad(page) {
        this.loading = true;

        // const {
        //
        // } = this.query;

        let values = {
            jobId:this.jobId,

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


