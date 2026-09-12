<template>
  <basic-container>
    <avue-crud :option="option"
               v-model:search="query"
               :table-loading="loading"
               :data="data"
               v-model:page="page"
               v-model="form"
               ref="crud"
               @cell-click="rowView"
               @selection-change="selectionChange"
               @current-change="currentChange"
               @size-change="sizeChange"
               @refresh-change="refreshChange">

      <!-- 左边按钮 -->
      <template #menu-left="{ row,index }">
        <el-button class="button-size" round title="数据同步" size="small"
                   icon="el-icon-plus" @click.stop="transferDataClick()">
        </el-button>
      </template>

    </avue-crud>
    <!-- 左下按钮 -->
    <page-attach ref="pageAttach"></page-attach>
    <el-dialog title="数据同步"  v-model="showTransferDataDialog" draggable
               :append-to-body="true" :modal='false' width="50%">
      <!--      <avue-select v-model="form" placeholder="请选择内容" type="tree" :dic="dic"></avue-select>-->
      <!--      <avue-select v-model="form" placeholder="请选择内容" type="tree" :dic="dic"></avue-select>-->
      <div style="display: flex">
        <el-row style="width: 45%">
          <el-row>数据来源选择</el-row>
          <el-row style="margin-top: 20px">
            <avue-form ref="syncForm1" :option="sourceFormOption" v-model="sourceForm"  >

            </avue-form>
          </el-row>
        </el-row>
        <el-divider direction="vertical" ></el-divider>
        <el-row style="width: 45%">
          <el-row>数据目标选择</el-row>
          <el-row style="margin-top: 20px">
            <avue-form ref="syncForm2" :option="targetFormOption" v-model="targetForm" >

            </avue-form>
          </el-row>
        </el-row>
      </div>
      <template #footer><span class="dialog-footer">
             <el-button icon="el-icon-check" size="small" circle title="开始同步"
                        @click="submitForm"></el-button>
            <el-button circle icon="el-icon-close" size="small" title="取消" @click="showTransferDataDialog = false"></el-button>

          </span></template>
    </el-dialog>

  </basic-container>
</template>

<script>
import {getPage,getDatasource,submitSync} from "@/api/database/configDatabaseDataClone";
import {mapGetters} from "vuex";
import _ from 'lodash'
import {getTableList} from "@/api/tool/model";
import {validatenull} from "@/utils/validate";
import pageAttach from "@/components/attach-dialog/page-attach/main.vue";


export default {
  components: {
    pageAttach
  },
  props:{
    component:{
      type:Boolean,
      default:false
    }
  },
  data() {
    return {
      sourceForm:{
        datasourceId:"",
        tableName:""
      },
      targetForm:{
        datasourceId:"",
        tableName:""
      },
      showTransferDataDialog:false,
      loadingOption: {
        lock: true,
        text: '物理表读取中',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0)'
      },
      loadingDiaLogOption: {
        lock: true,
        text: '数据同步中',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0)'
      },
      form: {},
      query: {},
      search: {},
      loading: false,
      page: {
        pageSize: 30,
        currentPage: 1,
        total: 0
      },
      selectionList: [],
      option: {
        searchShowBtn:false,
        // height: 'auto',
        tip: false,
        searchShow: true,
        searchMenuSpan: 6,
        border: true,
        index: true,
        viewBtn: true,
        selection: true,
        dialogClickModal: false,
        size: 'small',
        dialogDrag: true,
        menu: false,
        addBtn: false,
        submitBtn: false,
        saveBtn: false,
        cancelBtn: false,
        updateBtn: false,
        searchBtn:false,
        emptyBtn:false,
        addTitle: "新增数据源配置",
        editTitle: '编辑数据源配置',
        column: [
          {
            label: "数据源",
            prop: "datasourceId",
            type: "select",
            // defaultExpandAll: true,
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
            search: true,
            dicData: [],
            props: {
              label: 'name',
              value: 'id'
            }
          },
          {
            label: "表名称",
            prop: "tableName",
            type: "tree",
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
            slot: true,
            dicData: [],
            props: {
              label: "comment",
              value: "name"
            },
            search: true,
          },
          {
            label: "数据",
            prop: "data",
          }
        ]
      },
      targetFormOption:{
        menuBtn:false,
        column: [
          {
            label: "数据源",
            prop: "datasourceId",
            type: "select",
            // defaultExpandAll: true,
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            span: 24,
            hide: true,
            search: true,
            dicData: [],
            props: {
              label: 'name',
              value: 'id'
            },
            rules: [{
              required: true,
              message: "请选择数据源",
              trigger: "blur",
            }, ],
          },
          {
            label: "表名称",
            prop: "tableName",
            type: "tree",
            span: 24,
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
            slot: true,
            dicData: [],
            props: {
              label: "comment",
              value: "name"
            },
            search: true,
            rules: [{
              required: true,
              message: "请选择表",
              trigger: "blur",
            }, ],
          }
        ]
      },
      sourceFormOption:{
        menuBtn:false,
        column: [
          {
            label: "数据源",
            prop: "datasourceId",
            type: "select",
            // defaultExpandAll: true,
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            span: 24,
            hide: true,
            search: true,
            dicData: [],
            props: {
              label: 'name',
              value: 'id'
            },
            rules: [{
              required: true,
              message: "请选择数据源",
              trigger: "blur",
            }, ],
          },
          {
            label: "表名称",
            prop: "tableName",
            type: "tree",
            span: 24,
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
            slot: true,
            dicData: [],
            props: {
              label: "comment",
              value: "name"
            },
            search: true,
            rules: [{
              required: true,
              message: "请选择表",
              trigger: "blur",
            }, ],
          }
        ]
      },
      data: [],
      valueSpaceOption:{
        code:'',
        name:''
      }
    };
  },
  created() {
    this.initData()
  },
  watch: {
    'query.datasourceId'() {
      if (!validatenull(this.query.datasourceId)) {
        const fullLoading = this.$loading(this.loadingOption);
        getTableList(this.query.datasourceId).then(res => {
          const column = this.findObject(this.option.column, "tableName");
          column.dicData = res.data.data;
          fullLoading.close();
        }).catch(() => {
          fullLoading.close();
        })
      }
    },
    'query.tableName'() {
      this.onLoad(this.page, this.query);
    },
    'sourceForm.datasourceId'() {
      if (!validatenull(this.sourceForm.datasourceId)) {
        const fullLoading = this.$loading(this.loadingOption);
        getTableList(this.sourceForm.datasourceId).then(res => {
          const column = this.findObject(this.sourceFormOption.column, "tableName");
          column.dicData = res.data.data;
          fullLoading.close();
        }).catch(() => {
          fullLoading.close();
        })
      }
    },
    'targetForm.datasourceId'() {
      if (!validatenull(this.targetForm.datasourceId)) {
        const fullLoading = this.$loading(this.loadingOption);
        getTableList(this.targetForm.datasourceId).then(res => {
          const column = this.findObject(this.targetFormOption.column, "tableName");
          column.dicData = res.data.data;
          fullLoading.close();
        }).catch(() => {
          fullLoading.close();
        })
      }
    },
  },
  computed: {
    ...mapGetters(["permission"]),
    permissionList() {
      return {
        addBtn: this.vaildData(this.permission.databaseDataClone_add, false),
        viewBtn: this.vaildData(this.permission.databaseDataClone_view, false),
        delBtn: this.vaildData(this.permission.databaseDataClone_delete, false),
        editBtn: this.vaildData(this.permission.databaseDataClone_edit, false)
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
    resetForm(){
      this.sourceForm={
        datasourceId:"",
        tableName:""
      }
      this.$refs['syncForm1'].resetFields();
      this.targetForm={
        datasourceId:"",
        tableName:""
      }
      this.$refs['syncForm2'].resetFields();
    },
    async submitForm() {
      // console.log(a,b,c)
      let flag = false
      await this.$refs['syncForm2'].validate((valid, done) => {
        if (!valid) {
          flag =true
          done()
          return true
        } else {
          done()
        }
      });
      await this.$refs['syncForm1'].validate((valid, done) => {
        if (!valid) {
          flag =true
          done()
          return false
        } else {
          done()
        }
      });
      // let valid = await this.$refs.syncForm2.validate()
      // let valid1 = await this.$refs.syncForm1.validate()
      if (flag) {
        return
      }
      if (this.sourceForm.datasourceId == this.targetForm.datasourceId
          &&this.sourceForm.tableName == this.targetForm.tableName) {
        this.$message.warning("同一个表不能同步")
        //this.resetForm()
        return
      }

      const data = {
        sourceDataSourceId: this.sourceForm.datasourceId,
        targetDataSourceId: this.targetForm.datasourceId,
        sourceTableName:this.sourceForm.tableName,
        targetTableName:this.targetForm.tableName
      }
      const fullLoading = this.$loading(this.loadingDiaLogOption);
      submitSync(data).then(res=>{
        // console.log("res",res)
        if (res.data.code ==200) {
          this.query.datasourceId = this.targetForm.datasourceId
          this.query.tableName = this.targetForm.tableName
          this.$message.success("操作成功！")
        }
        fullLoading.close();
      }).catch(() => {
        fullLoading.close();
      })
    },
    transferDataClick() {
      // console.log(this.permissionList,this.permission)
      this.showTransferDataDialog = true
    },
    rowView(row, {property}) {
      if (property === "index") {
        this.$refs.crud.rowView(row, row.$index)
      }
    },
    initData() {
      let datasourceDicData = this.findObject(this.option.column, "datasourceId")
      let sourceFormOptionColumn = this.findObject(this.sourceFormOption.column, "datasourceId")
      let targetFormOptionColumn = this.findObject(this.targetFormOption.column, "datasourceId")
      getDatasource()
          .then(res => {
            datasourceDicData.dicData = res.data.data
            sourceFormOptionColumn.dicData = res.data.data
            targetFormOptionColumn.dicData = res.data.data
          })
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.onLoad(this.page)
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.onLoad(this.page)
    },
    refreshChange() {
      this.onLoad(this.page, this.query);
    },
    selectionChange(list){
      this.selectionList = list;
    },
    onLoad(page) {
      this.loading = true;
      const {
        datasourceId,
        tableName
      } = this.query;
      if (this.validatenull(datasourceId) || this.validatenull(tableName) ) {
        this.data = []
        this.loading = false;
        _.remove(this.option.column, column => !column.search)
        return
      }

      let values = {
        datasourceId,
        tableName
      };

      getPage(page.currentPage, page.pageSize, values).then(res => {
        const {records, total, fields} = res.data.data

        // 初始化表头(移除除搜索列之外的所有数据)
        _.remove(this.option.column, column => !column.search)
        fields.forEach(field => {
          field.width=320
          field.children=[{
            label:field.prop,
            prop: field.prop,
            width:320
          }]
          this.option.column.push(field)
        })
        this.page.total = total;
        this.data = records;
        this.loading = false;
        this.$refs.crud.refreshTable()
      });
    },

  }
};
</script>

<style lang="scss">

.column-class{
  background-color: #1e9fff;
  color: red;
}
.column-label-class{
  background-color: #cf9236;
}
</style>
<style scoped>
:deep(.el-divider--vertical){
  height: 120px!important;
}

</style>
