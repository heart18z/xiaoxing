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
        <template #menu-left>
          <el-button
            class="button-size"
            round
            title="新增"
            v-if="permission.job_add"
            size="small"
            @click.stop="addList()"
          >
            <el-icon><Plus /></el-icon>
          </el-button>
          <el-button
            class="button-size"
            round
            title="删除"
            v-if="permission.job_delete"
            size="small"
            @click.stop="handleDelete()"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
          <el-button
            class="button-size"
            round
            title="复制"
            v-if="permission.job_add"
            size="small"
            @click.stop="copyList()"
          >
            <el-icon><DocumentCopy /></el-icon>
          </el-button>
          <el-button class="button-size" round title="查看日志" size="small" @click.stop="showLog()">
            <el-icon><Notebook /></el-icon>
          </el-button>
          <el-button
            class="button-size"
            round
            title="查看最近50次执行时间"
            size="small"
            @click.stop="showCronExpressionDate()"
          >
            <el-icon><Calendar /></el-icon>
          </el-button>
          <el-button class="button-size" round title="执行一次" size="small" @click.stop="runOneTime()">
            <el-icon><VideoPlay /></el-icon>
          </el-button>
        </template>

      <template #cronExpression-form>
        <el-input v-model="form.cronExpression">
          <template #append>
            <el-button type="primary" @click="handleShowCron">
              生成表达式
              <el-icon class="el-icon--right"><Clock /></el-icon>
            </el-button>
          </template>
        </el-input>
      </template>


        <template #index="{row,index}">
            <div>{{index+1}}</div>
        </template>

      <template #misfirePolicyType="{ item }">
       <el-row>
         <el-col :span="20" style="padding-left: 20px">{{item.dictValue}}</el-col>
         <el-col :span="4">
           <el-tooltip class="item" effect="dark" :disabled="item.remark===''||item.remark===null" :content="item.remark" placement="bottom-end">
             <el-icon class="el-icon-info"><InfoFilled /></el-icon>
           </el-tooltip>
         </el-col>
       </el-row>
      </template>

      <template #processStatus="{ row }">
        <avue-switch
          active-color="#13ce66"
          inactive-color="#00BBFF"
          v-model="row.processStatus"
          :dic="process_dic"
          inactiveColor="#DCDFE6"
          :inlinePrompt=true
          @click="handleStatusChange(row)"></avue-switch>
      </template>

        <template #menu-form="{ type }">
          <template v-if="type !== 'view'">
            <el-button class="form-menu-btn form-menu-btn--save" type="primary" size="small" circle title="保存" @click="$refs.crud.rowSave()">
              <el-icon><Check /></el-icon>
            </el-button>
            <el-button class="form-menu-btn" size="small" circle title="取消" @click="$refs.crud.closeDialog()">
              <el-icon><Close /></el-icon>
            </el-button>
          </template>
        </template>
    </avue-crud>
    <el-dialog title="Cron表达式生成器" v-model="openCron" append-to-body destroy-on-close class="scrollbar">
      <crontab @hide="openCron=false" @fill="crontabFill" :expression="expression"></crontab>
    </el-dialog>
    <el-dialog :title="'任务执行时间顺序'"
               v-model="openShowCronDate" append-to-body
               width="40%"
               destroy-on-close class="scrollbar">
      <el-checkbox-group v-model="checkJobList" @change="changeJob">
        <el-checkbox :label="item.id" v-for="(item,index) in jobSelection" :key="index" :disabled="item.processStatus ==='1'">
          {{item.jobName}}
          <div class="status-point" :style=" 'background-color:'+item.color" />
        </el-checkbox>
      </el-checkbox-group>
      <div class="popup-result">

        <ul class="popup-result-scroll">
          <template v-if='!openShowCronDateLoading'>
            <el-timeline>
              <el-timeline-item
                v-for="(activity, index) in cronExpressionDateList"
                :key="index"
                :color="activity.color"
                :timestamp="activity.runTime">
                {{activity.jobName}}
              </el-timeline-item>
            </el-timeline>

          </template>
          <li v-else>计算结果中...</li>
        </ul>
      </div>
    </el-dialog>
    <remote-sync-form ref = "remoteForm" @submit="submitRemoteForm">

    </remote-sync-form>
    <!-- 左下按钮 -->
    <page-attach ref="pageAttach"></page-attach>
    <Job-log ref="jobLog"></Job-log>
  </basic-container>
</template>

<script>
import remoteSyncForm from "@/views/quartz/remoteSyncForm/index.vue";
import Crontab from '@/components/Crontab/index.vue'
import pageAttach from "@/components/attach-dialog/page-attach/main.vue"
import {
  addJob,
  changeJobStatus, cleanData,
  getDetail,
  getList,
  queryCronExpression,
  remove,
  run,
  updateJob
} from "@/api/quartz/job";
import {mapGetters} from "vuex";
import JobLog from "@/views/quartz/jobLog.vue";
import func from "@/utils/func";

const permission_dic=[
    {value:"1",label:"禁止"},
    {value:"0",label:"允许"},

  ]
  const process_dic=[

    {value:"1",label:"暂停"},
    {value:"0",label:"启用"},

  ]
export default {
    components: {
     pageAttach,Crontab,JobLog,remoteSyncForm
    },
    data() {
      return {
        checkJobList:[],
        jobSelection:[],
        openShowCronDateLoading:false,
        openShowCronDateTaskName:"",
        openShowCronDate:false,
        cronExpressionDateList:[],
        expression: "",
        jobId: "",
        process_dic:process_dic,
        permission_dic:permission_dic,
        openCron:false,
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
              label: "任务编号",
              prop: "jobCode",
              type: "input",
              width: 100,
              display:false,
            },
            {
              label: "任务名称",
              prop: "jobName",
              type: "input",
              display: false,
              search: true,
              rules: [{
                required: true,
                message: "请输入任务名称",
                trigger: "blur"
              }]
            },
            {
              label: "对外交互",
              prop: "isExternal",
              type: "radio",
              rules: [{
                required: true,
                message: "请选择是否对外交互",
                trigger: "blur"
              }],
              display: false,
              dicData: [
                {
                  label: "是",
                  value: 1
                },
                {
                  label: "否",
                  value: 0
                }
              ],
              span: 12,
            },
            {
              label: "交互任务",
              prop: "interactionId",
              type: "select",
              display: false,
              rules: [{
                required: true,
                message: "请输入调用目标字符串",
                trigger: "blur"
              }],
              dicMethod: "post",
            dicUrl: "/api/blade-interactive/interactive/dict",
              props: {
                label: 'key',
                value: 'value',
              },
            },
            {
              label: "调用目标",
              prop: "invokeTarget",
              type: "tree",
              // labelWidth: 120,
              rules: [{
                required: true,
                message: "请输入调用目标字符串",
                trigger: "blur"
              }],
              display: false,
              dicMethod: "post",
            dicUrl: "/api/blade-task/task/quartzTaskName-name-list",
              parent:false,
              props: {
                label: "dictValue",
                value: "dictKey"
              },
            },
            {
              label: "cron执行表达式",
              prop: "cronExpression",
              type: "input",
              display: false,
              labelWidth: 130,
              rules: [{
                required: true,
                message: "请输入cron执行表达式",
                trigger: "blur"
              }]
            },
            {
              label: "MisFire策略",
              prop: "misfirePolicy",
              type: "select",
              display: false,
              labelTip:"周期性任务需要在某个规定的时间执行，但是由于某种原因导致任务未执行，称为MisFire",
              rules: [{
                required: true,
                message: "请选择执行策略",
                trigger: "blur"
              }],
              width: 100,
              labelWidth: 120,
              dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=quartz_mis_fire",
              props: {
                label: 'dictValue',
                value: 'dictKey'
              },
            },
            {
              label: "并发执行",
              prop: "concurrent",
              width: 80,
              type: "select",
              display: false,
              rules: [{
                required: true,
                message: "请选择是否并发执行",
                trigger: "blur"
              }],
              value: 0,
              dicData: permission_dic
            },
            {
              label: "运行状态",
              prop: "processStatus",
              type: "switch",
              display: false,
              value: 0,
              rules: [{
                required: true,
                message: "请选择运行状态",
                trigger: "blur"
              }],
              addDisplay: false,
              editDisplay: false,
              dicData: process_dic,
              search: true,
            },
            {
              label: "备注",
              prop: "remark",
              display: false,
              type: "input",
            },
          ],
          group: [
            {
              label: '基础信息',
              prop: 'info',
              icon: 'el-icon-connection',
              column: [

                {
                  label: "任务名称",
                  prop: "jobName",
                  type: "input",
                  rules: [{
                    required: true,
                    message: "请输入任务名称",
                    trigger: "blur"
                  }]
                },
                {
                  label: "调用目标",
                  prop: "invokeTarget",
                  type: "tree",
                  // labelWidth: 120,
                  // parent:false,
                  rules: [{
                    required: true,
                    message: "请输入调用目标字符串",
                    trigger: "blur"
                  }],
                  dicFormatter: (res) => {
                    let data = res.data
                    for (let i = 0; i < data.length; i++) {
                      data[i].disabled = true
                    }
                    return data
                  },
                  hide: true,
                  dicMethod: "post",
            dicUrl: "/api/blade-task/task/quartzTaskName-name-list",
                  props: {
                    label: "dictValue",
                    value: "dictKey"
                  },
                },
                {
                  label: "对外交互",
                  prop: "isExternal",
                  type: "radio",
                  rules: [{
                    required: true,
                    message: "请选择是否对外交互",
                    trigger: "blur"
                  }],
                  value: 0,
                  change: this.isExternalChange,
                  dicData: [
                    {
                      label: "是",
                      value: 1
                    },
                    {
                      label: "否",
                      value: 0
                    }
                  ],
                  span: 12,
                },
                {
                  label: "交互任务",
                  prop: "interactionId",
                  type: "select",
                  rules: [{
                    required: true,
                    message: "请输入调用目标字符串",
                    trigger: "blur"
                  }],
                  dicMethod: "post",
            dicUrl: "/api/blade-interactive/interactive/dict",
                  props: {
                    label: 'key',
                    value: 'value',
                  },
                  display: false
                },
                {
                  label: "备注",
                  prop: "remark",
                  type: "input",
                  row:true,
                  span:24
                },
              ],
            },
            {
              label: '创建策略',
              prop: 'strategy',
              icon: 'el-icon-connection',
              column: [
                {
                  label: "cron执行表达式",
                  prop: "cronExpression",
                  type: "input",
                  formslot: true,
                  labelWidth: 130,
                  rules: [{
                    required: true,
                    message: "请输入cron执行表达式",
                    trigger: "blur"
                  }]
                },
                {
                  label: "MisFire策略",
                  prop: "misfirePolicy",
                  type: "select",
                  labelTip:"周期性任务需要在某个规定的时间执行，但是由于某种原因导致任务未执行，称为MisFire",
                  rules: [{
                    required: true,
                    message: "请选择执行策略",
                    trigger: "blur"
                  }],
                  labelWidth: 120,
                  dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=quartz_mis_fire",
                  props: {
                    label: 'dictValue',
                    value: 'dictKey'
                  },
                  hide:true,
                },
                {
                  label: "并发执行",
                  prop: "concurrent",
                  type: "select",
                  rules: [{
                    required: true,
                    message: "请选择是否并发执行",
                    trigger: "blur"
                  }],
                  value: "0",
                  dicData: permission_dic
                },
                {
                  label: "运行状态",
                  prop: "processStatus",
                  type: "switch",
                  value: 0,
                  rules: [{
                    required: true,
                    message: "请选择运行状态",
                    trigger: "blur"
                  }],
                  addDisplay: false,
                  editDisplay: false,
                  dicData: process_dic,
                  search: true,
                },

                {
                  label: "远程调用",
                  prop: "jobRunTemplate",
                  type: "input",
                  focus: this.remoteConfigFocus
                },
              ]
            },
          ]
        }
        ,
        data: [],
        timer: null
      };
    },
    computed: {
      ...mapGetters(["permission"]),
      permissionList() {
        return {
          addBtn: this.vaildData(this.permission.job_add, false),
          viewBtn: this.vaildData(this.permission.job_view, false),
          delBtn: this.vaildData(this.permission.job_delete, false),
          editBtn: this.vaildData(this.permission.job_edit, false)
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
      remoteConfigFocus() {
        this.$refs.remoteForm.open( this.form.jobRunTemplate || "{}")
      },
      submitRemoteForm(data) {
        this.form["jobRunTemplate"] = JSON.stringify(data)

      },
      async showCronExpressionDate() {
        this.cronExpressionDateList = []
        this.openShowCronDateLoading = true

        this.openShowCronDate = true
        //只取50条，超出再说。。。
        const data =await getList(1, 50, {});
        this.jobSelection = data.data.data.records;

      if (this.jobSelection.length>0) {
        // 指定最大多少种颜色
        let colors =this.generateDistinctColors(this.jobSelection.length)
        for (let i = 0; i < this.jobSelection.length; i++) {
          this.jobSelection[i].color = colors[i];
        }

      }

        this.checkJobList = this.jobSelection.filter(f=>f.processStatus ==='0').map(i=>i.id)
        this.changeJob()
      },
      generateDistinctColors(numColors) {
        const colors = [];
        const step = 360 / numColors; // 每种颜色之间的角度步长

        for (let i = 0; i < numColors; i++) {
          const hue = i * step; // 计算色相值
          const color = `hsl(${hue}, 70%, 70%)`; // 使用 HSL 模型生成颜色
          colors.push(color);
        }

        return colors;
      },
      changeJob() {
        this.cronExpressionDateList = []
        if (this.checkJobList.length ===0) {
          return;
        }
        queryCronExpression(this.checkJobList.join(",")).then((res)=>{
          this.openShowCronDateLoading= false
          const map = {};
          const list =this.jobSelection
          for(let index in list){
            map[list[index].id] = list[index].color;
          }
          this.cronExpressionDateList = res.data.data

          for (let i = 0; i < this.cronExpressionDateList.length; i++) {
            this.cronExpressionDateList[i].color = map[this.cronExpressionDateList[i].jobId]
          }
          this.cronExpressionDateList.sort((a, b) => {
            if (a.runTime < b.runTime) return -1;
            if (a.runTime > b.runTime) return 1;
            return 0;
          })
          //console.log("this.cronExpressionDateList",this.cronExpressionDateList)
        })
      },
      color(){
        this.colorAngle = Math.floor(Math.random()*360);
        return  'hsla('+ this.colorAngle +',100%,50%,1)';
      },

      isExternalChange({value}) {
        const column = this.findObject(this.option.group, "interactionId");
        column.display = value === 1;
      },
      runOneTime() {
        if (this.selectionList.length>1) {
          this.$message.warning("只能选择一条数据查看日志")
          return
        } else if (this.selectionList.length === 0) {
          this.$message.warning("请选择一条数据后查看日志")
          return
        }
        run(this.selectionList[0]).then(()=>{
          this.onLoad(this.page);
          this.$message({
            type: "success",
            message: "操作成功!"
          });
        })
      },
      showLog() {
        if (this.selectionList.length>1) {
          this.$message.warning("只能选择一条数据查看日志")
          return
        } else if (this.selectionList.length === 0) {
          this.$message.warning("请选择一条数据后查看日志")
          return
        }
        this.jobId = this.selectionList[0].id
        this.$refs.jobLog.openDialog(this.jobId)
      },
      // 任务状态修改
      handleStatusChange(row) {
        const status =row.processStatus
         changeJobStatus(row.id, row.processStatus).then(()=>{
           this.onLoad(this.page)
           this.$message.success("操作成功");
           if (func.notEmpty(row.tableName) && status ==="1") {
             this.$confirm('关闭成功！是否要清空相关表('+row.tableName+')的数据').then(()=> {
               cleanData(row)
             }).then(() => {
               this.onLoad(this.page)
               this.$message.success("清空成功");
             }).catch(()=> {
               this.onLoad(this.page)
             });
           }
         }).catch(()=> {
           this.onLoad(this.page)
         });




      },
      /** cron表达式按钮操作 */
      handleShowCron() {
        this.expression = this.form.cronExpression;
        this.openCron = true;
      },
      /** 确定后回传值 */
      crontabFill(value) {
        this.form.cronExpression = value;
      },
      addList() {
        this.$refs.crud.rowAdd()
        this.form = {
          misfirePolicy: "1"
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
        if (this.permission.job_edit && property === "index") {
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
        addJob(row).then(() => {
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
        updateJob(row).then(() => {
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
          jobName,
          processStatus,
        } = this.query;

        let values = {
           jobName_equal: jobName,
           processStatus_equal: processStatus,
        };

        getList(page.currentPage, page.pageSize, values).then(res => {
          const data = res.data.data;
          this.page.total = data.total;
          this.data = data.records;
          this.loading = false;
          this.selectionClear();
        });
      },

    }
  };
</script>

<style scoped>
.form-menu-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.form-menu-btn + .form-menu-btn {
  margin-left: 8px;
}

.form-menu-btn .el-icon {
  margin: 0;
}

.status-point {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.popup-result {
  box-sizing: border-box;
  line-height: 24px;
  margin: 25px auto;
  padding: 15px 10px 10px;
  border: 1px solid #ccc;
  position: relative;
}
.popup-result .title {
  position: absolute;
  top: -28px;
  left: 50%;
  width: 140px;
  font-size: 14px;
  margin-left: -70px;
  text-align: center;
  line-height: 30px;
  background: #fff;
}
.popup-result table {
  text-align: center;
  width: 100%;
  margin: 0 auto;
}
.popup-result table span {
  display: block;
  width: 100%;
  font-family: arial;
  line-height: 30px;
  height: 30px;
  white-space: nowrap;
  overflow: hidden;
  border: 1px solid #e8e8e8;
}
.popup-result-scroll {
  font-size: 12px;
  line-height: 24px;
  height: 30em;
  overflow-y: auto;
}
</style>
