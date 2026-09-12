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
            <el-button class="button-size" round title="新增" v-if="permission.interactive_add" size="small" icon="el-icon-plus" @click.stop="addList()">
            </el-button>
            <el-button class="button-size" round title="删除" v-if="permission.interactive_delete" size="small" icon="el-icon-delete"
                @click.stop="handleDelete()">
            </el-button>
            <el-button class="button-size" round title="复制" v-if="permission.interactive_add" size="small" icon="el-icon-document-copy" @click.stop="copyList()">
            </el-button>
        </template>

      <template #interactiveCreateHeader="{  column  }">
        <span>{{ (column || {}).label }}</span>
        <el-popover  trigger="hover">
          <el-row>基础框架交互：框架⾃定义的交互，在业务开发前即已经实现了交互的功能</el-row>
          <el-row>业务系统交互：是在引⽤框架后，由业务系统根据交互需求进⾏的个性化交互</el-row>
          <template #reference><el-icon class="el-icon-info" /></template>
        </el-popover>
      </template>

      <template #interactiveCodeHeader="{  column  }">
        <span>{{ (column || {}).label }}</span>
        <el-popover  trigger="hover">
          <el-row>当前编码由开发人员填写，用于远程调用时关联对接地址</el-row>
          <template #reference><el-icon class="el-icon-info" /></template>
        </el-popover>
      </template>

        <!-- 打开附件列表 -->
        <template #uploadList="{ row }">
            <row-attach :source-id="row.id"></row-attach>
        </template>

        <template #index="{row,index}">
            <div>{{index+1}}</div>
        </template>

        <template #status="{ row }">
          <avue-switch
            active-color="#13ce66"
            inactive-color="#00BBFF"
            v-model="row.status"
            :dic="dic"
            inactiveColor="#DCDFE6"
            :inlinePrompt=true
            @click="changeStatus(row,row.status)"></avue-switch>
        </template>

      <template #syncRule="{ row }">
        <span @click="showRules(row)"  v-if="row.$syncRule"  style=" font-style: italic; color: blue;text-align: center;text-decoration: underline;">
          {{row.$syncRule}}
        </span>
        <span v-else>----</span>
      </template>

<!--      <template #syncRuleRemarkForm="{ row }">-->
<!--&lt;!&ndash;        {{syncRuleRemark}}&ndash;&gt;-->
<!--        <div v-html="syncRuleRemark"></div>-->
<!--      </template>-->


      <template #jobListForm="{ value }">
        <el-row v-if="value">
          <el-card v-for="(item,index) in value" :key="index" style="margin-top: 5px;margin-bottom: 5px" >
            <template #header><div class="clearfix">
              <span>{{item.jobCode  }}  {{item.jobName}}</span>
<!--              <el-button style="float: right; padding: 3px 0" type="text">操作按钮</el-button>-->
            </div></template>
          <job-show-form :item="item"></job-show-form>
          </el-card>
        </el-row>


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

    <el-dialog id="attach-dialog" title="同步规则详情"
               v-model="rulesDialog" draggable
               :append-to-body="true" :modal='false' width="43%">
      <!--      <div>请审批</div>-->
    <p>
      一、同步数据表：{{rulesRow.tableNames}}
    </p>

      <p>
        二、同步方式为：{{rulesRow.syncType}}
      </p>

      <el-row>
        同步规则为：<p v-html="rulesRow.syncTypeRemark"></p>
      </el-row>
    </el-dialog>
<!--    <el-dialog title="Cron表达式生成器" v-model="openCron" append-to-body destroy-on-close class="scrollbar">-->
<!--      <crontab @hide="openCron=false" :readOnly="true" :expression="expression"></crontab>-->
<!--    </el-dialog>-->
  </basic-container>
</template>

<script>
  import pageAttach from "@/components/attach-dialog/page-attach/main.vue"
  import rowAttach from "@/components/attach-dialog/row-attach/main.vue"
  import {getList, add, update, remove, changeStatus} from "@/api/system/interactive";
  import {mapGetters} from "vuex";
  import {validatenull} from "@/utils/validate";
  import {getTableList} from "@/api/tool/model";
  import func from "@/utils/func";
  // import {getDictionary} from "@/api/system/dictbiz";
  import Crontab from "@/components/Crontab/index.vue";
  import jobShowForm from "@/components/job-show-form/index.vue";

  export default {

    components: {
      Crontab,jobShowForm,
     pageAttach, rowAttach
    },
    watch: {
      'form.datasourceId'() {
        if (!validatenull(this.form.datasourceId) && this.form.datasourceId!==-1) {
          const fullLoading = this.$loading(this.loadingOption);
          getTableList(this.form.datasourceId).then(res => {
            const column = this.findObject(this.option.group[1].column, "relatedTableName");
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
        expression:"",
        openCron:false,
        rulesRow:{},
        rulesDialog:false,
        loadingOption: {
          lock: true,
          text: '物理表读取中',
          spinner: 'el-icon-loading',
          background: 'rgba(0, 0, 0, 0)'
        },
        dic:[{label: "", value: 2,}, {label: "", value: 1,}],
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
              label: "交互名称",
              prop: "interactiveName",
              type: "input",
              search: true,
              rules: [{
                required: true,
                message: "请输入交互名称",
                trigger: "blur"
              }],
            },
            {
              label: "关联定时任务",
              prop: "jobsName",
              overHidden:true,
              type: "textarea",
              display: false,
            },
            // {
            //   label: "绑定任务",
            //   prop: "taskCode",
            //   type: "select",
            //   dicMethod: "post",
            // dicUrl: "/api/blade-task/task/code-name-list",
            //   props: {
            //     label: 'key',
            //     value: 'value',
            //   },
            //   rules: [{
            //     required: true,
            //     message: "请选择绑定任务",
            //     trigger: "blur"
            //   }],
            // },
            {
              label: "交互类型",
              width: 100,
              prop: "interactiveType",
              type: "select",
              rules: [{
                required: true,
                message: "请选择交互类型",
                trigger: "blur"
              }],
              dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=interactive_type",
              props: {
                label: "dictValue",
                value: "dictKey"
              },
              search: true,
            },
            {
              label: "交互方式",
              width: 100,
              prop: "interactiveMethod",
              type: "select",
              rules: [{
                required: true,
                message: "请选择交互方式",
                trigger: "blur"
              }],
              dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=interactive_method",
              props: {
                label: "dictValue",
                value: "dictKey"
              },
              search: true,
            },
            {
              label: "同步规则",
              prop: "syncRule",
              dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=sync_rule",
              props: {
                label: "dictValue",
                value: "dictKey"
              },
              width:100,
              type: "select",
              display:false,
            },
            {
              label: "对方名称",
              prop: "targetSystem",
              type: "input",
            },
            {
              label: "主要用途",
              prop: "purpose",
              type: "input",
            },
            {
              label: "状态",
              prop: "status",
              type: "input",
              slot: true,
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              width: 80,
            },
            {
              label: "对接地址",
              prop: "address",
              overHidden:true,
              type: "input",
            },
            {
              label: "备注说明",
              prop: "remark",
              type: "input",
              hide: true
            },

            {
              label: "创建阶段",
              prop: "interactiveCreate",
              type: "select",
              dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=interactive_step",
              props: {
                label: "dictValue",
                value: "dictKey"
              },
              value: "biz",
              disabled: true
            },

            {
              label: '交互编码',
              width: "100",
              prop: 'interactiveCode',
            },

            {
              label: '附件列表',
              span: 24,
              width: "100",
              prop: 'uploadList',
              slot: true,
              display: false,
            },
          ],
          group: [
            {
              label: '定时任务',
              prop: 'jobListInfo',
              icon: 'el-icon-message-solid',
              addDisplay:false,
              column: [

                {
                  label: "",
                  labelWidth:0,
                  prop: "jobList",
                  row:true,
                  span:24
                },

              ]
            },
            {
              label: '关联数据表',
              prop: 'info',
              icon: 'el-icon-connection',
              column: [

                {
                  label: "数据源",
                  prop: "datasourceId",
                  search: false,
                  hide: true,
                  type: "select",
                  dicMethod: "post",
            dicUrl: "/api/blade-develop/datasource/select",
                  props: {
                    label: "name",
                    value: "id"
                  },
                },
                {
                  label: "表名称",
                  prop: "relatedTableName",
                  type: "tree",
                  slot: true,
                  dicData: [],
                  props: {
                    label: "comment",
                    value: "comment"
                  },
                  multiple:true,
                  hide: true,
                },


              ]
            },
            {
              label: '数据同步',
              prop: 'syncInfo',
              icon: 'el-icon-paperclip',
              column: [

                {
                  label: "同步方式",
                  prop: "syncRule",
                  dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=sync_rule",
                  props: {
                    label: "dictValue",
                    value: "dictKey"
                  },
                  type: "select",
                  // display:false,
                  change: this.syncRuleChange,
                  hide: true,
                },
                {
                  label: "同步规则",
                  prop: "syncRuleRemark",
                  type:"textarea",
                  hide: true,
                  labelTip:"根据实际规则修改说明"
                },

              ]
            }
          ]
        },
        data: [],
        timer: null,
        syncRuleRemark:""
      };
    },
    computed: {
      ...mapGetters(["permission"]),
      permissionList() {
        return {
          addBtn: this.vaildData(this.permission.interactive_add, false),
          viewBtn: this.vaildData(this.permission.interactive_view, false),
          delBtn: this.vaildData(this.permission.interactive_delete, false),
          editBtn: this.vaildData(this.permission.interactive_edit, false)
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
      test(d) {
        console.log("d",d)
      },
      /** cron表达式按钮操作 */
      handleShowCron() {
        this.expression = this.form.cronExpression;
        this.openCron = true;
      },
    async showRules(row){

      let syncRule = row.$syncRule
      let syncRuleRemark= row.syncRuleRemark
      let tableNames =row.relatedTableName
      // if (func.notEmpty(row.datasourceId) && func.notEmpty(row.relatedTableName)) {
      //   const res = await getTableList(row.datasourceId)
      //   const tableNameList = row.relatedTableName.split(",")
      //   console.log("tableNameList",tableNameList)
      //   const data = res.data.data
      //   if (data) {
      //     tableNames =data.filter(i=>tableNameList.includes(i)).map(i=>i.comment).join(",")
      //   }
      // }

      // if (func.notEmpty(row.syncRule)) {
      //   const res =  await getDictionary({code:"sync_rule"})
      //   const data = res.data.data
      //   if (data) {
      //     const item = data.find(i=>i.dictKey === row.syncRule)
      //     if (item) {
      //       syncRule = item.dictValue
      //       if (func.notEmpty(item.remark)) {
      //         syncRuleRemark = item.remark.replaceAll("；","</br>")
      //
      //       }
      //     }
      //   }
      // }

      this.rulesRow = {
        syncType: syncRule,
        syncTypeRemark: syncRuleRemark,
        tableNames:tableNames,
      }
      this.rulesDialog = true

    },
    syncRuleChange(data) {
      if(func.isEmpty(data.value) || func.isEmpty(data.item.remark)) {
        this.form.syncRuleRemark = ""
      }else {
        this.form.syncRuleRemark = data.item.remark
          //.replaceAll("；","</br>")
      }
    },
    async changeStatus(row, type) {
      changeStatus({id:row.id,status:type}).then(() => {
        this.onLoad(this.page)
        this.$message({
          type: "success",
          message: "操作成功!"
        });
      }).catch(()=>{
        this.onLoad(this.page)
      });

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
        if (this.permission.interactive_edit && property === "index") {
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
        row.relatedTableName = func.join(row.relatedTableName);
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
        row.relatedTableName = func.join(row.relatedTableName);
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
      beforeOpen(done) {
        // if (["edit", "view"].includes(type)) {
        //   getDetail(this.form.id).then(res => {
        //     this.form = res.data.data;
        //   });
        // }
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
          interactiveName,
          interactiveType,
          interactiveMethod,
        } = this.query;

        let values = {
           interactiveName_like: interactiveName,
           interactiveType_equal: interactiveType,
           interactiveMethod_equal: interactiveMethod,
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


