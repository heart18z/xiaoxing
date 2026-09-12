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
            <el-button class="button-size" round title="新增" v-if="permission.thirdPartyTable_add" size="small" icon="el-icon-plus" @click.stop="addList()">
            </el-button>
            <el-button class="button-size" round title="删除" v-if="permission.thirdPartyTable_delete" size="small" icon="el-icon-delete"
                @click.stop="handleDelete()">
            </el-button>
            <el-button class="button-size" round title="复制" v-if="permission.thirdPartyTable_add" size="small" icon="el-icon-document-copy" @click.stop="copyList()">
            </el-button>
        </template>

        <!-- 打开附件列表 -->
        <template #uploadList="{ row }">
            <row-attach :source-id="row.id"></row-attach>
        </template>

        <template #index="{row,index}">
            <div>{{index+1}}</div>
        </template>


      <template #databaseIdType="{ item,value,label }">
        <span>{{ item.name }}</span> <span style="float: right">

              <el-tooltip effect="dark"
                          :content="item.remark"
                          placement="bottom">
                 <el-icon class="el-icon-info" />
              </el-tooltip>
      </span>
      </template>


      <template #columnForm="{row}">
        <el-row>表字段</el-row>
        <el-row>
          <third-party-table-column :del-data="delData" :data="columnData" :read-only="readOnly" @addList="columnData.push({})">
          </third-party-table-column>
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
  </basic-container>
</template>

<script>
  import pageAttach from "@/components/attach-dialog/page-attach/main.vue"
  import rowAttach from "@/components/attach-dialog/row-attach/main.vue"
  import {getList, getDetail, add, update, remove} from "@/api/thirdPartyTable/thirdPartyTable";
  import {mapGetters} from "vuex";
  import thirdPartyTableColumn from "@/views/thirdPartyTable/thirdPartyTableColumn/thirdPartyTableColumn.vue";
  import func from "@/utils/func";

  export default {
    components: {
     pageAttach, rowAttach ,thirdPartyTableColumn
    },
    data() {
      const validateTableName = (rule, value, callback) => {
        if (func.isEmpty(value)) {
          callback(new Error("表名称不能为空！"));
        } else if (!/^\w+$/.test(value)){
          callback(new Error("表名称只能由下划线、数字、字母组成！"));
        } else {
          callback()
        }
      };

      return {
        readOnly: false,
        columnData:[],
        form: {},
        query: {},
        delData:[],
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
          labelWidth: 110,
          searchLabelWidth: 120,
          dialogWidth:"80%",
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
            // {
            //   label: "备注",
            //   prop: "remark",
            //   type: "input",
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
              label: "是否已删除",
              prop: "isDeleted",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "中台衍生库",
              prop: "databaseId",
              editDisabled: true,
              type: "select",
              rules: [{
                required: true,
                message: "请选择中台衍生库",
                trigger: "blur"
              }],
              dicMethod: "post",
            dicUrl: "/api/blade-develop/datasource/select",
              props: {
                label: "name",
                value: "id"
              },
              search:true,
              dicFormatter: (res) => {
                let data = res.data
                for (let i = 0; i < data.length; i++) {
                  data[i].disabled = data[i].linkType !== "10"
                }
                //console.log("data...",data)
                return data
              },
              row:true
            },
            {
              label: "表名称",
              prop: "tableName",
              search:true,
              editDisabled: true,
              type: "input",
              maxlength:64,
              blur: this.tableNameChange,
              rules: [{
                required: true,
                // message: "请输入表名称",
                trigger: "blur",
                validator: validateTableName
              }]
            },
            {
              label: "表注释",
              search:true,
              prop: "tableComment",
              // editDisabled: true,
              type: "input",
              rules: [{
                required: true,
                message: "请输入表注释",
                trigger: "blur"
              }]
            },
            {
              label: "包含字段数量",
              prop: "columnCount",
              type: "input",
              display: false,
            },
            {
              label: "来源系统",
              prop: "sourceSystem",
              type: "input",
              span:12,
              rules: [{
                required: true,
                message: "请输入来源系统",
                trigger: "blur"
              }]
            },{
              label: "系统开发人",
              prop: "sourceSystemDeveloper",
              type: "input",
              rules: [{
                required: true,
                message: "请输入系统开发人",
                trigger: "blur"
              }]
            },
            {
              label: "状态",
              prop: "status",
              type: "switch",
              align: "center",
              width: '80px',
              dicData: [{
                label: "关闭",
                value: 2,
              },
                {
                  label: "开启",
                  value: 1,
                },
              ],
              value: 1,
              slot: true,
              rules: [{
                required: true,
                message: "请选择是否启用",
                trigger: "blur",
              },],
              // addDisplay: false,
              editDisabled: false,
              // viewDisplay: false,
              hide: true,
            },
            {
              label: "数据写入",
              prop: "isExistData",
              type: "select",
              disabled: true,
              hide: true,
              dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=dataStatus",
              placeholder: ' ',
              props: {
                label: 'dictValue',
                value: 'dictKey'
              },
            },
            {
              label: "用途说明",
              prop: "purpose",
              type: "input",
              span:24
            },

            {
              label: "",
              prop: "column",
              type: "input",
              labelWidth:0,
              row:true,
              hide: true,
              span:24
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
        },
        data: [],
        timer: null
      };
    },
    computed: {
      ...mapGetters(["permission"]),
      permissionList() {
        return {
          addBtn: this.vaildData(this.permission.thirdPartyTable_add, false),
          viewBtn: this.vaildData(this.permission.thirdPartyTable_view, false),
          delBtn: this.vaildData(this.permission.thirdPartyTable_delete, false),
          editBtn: this.vaildData(this.permission.thirdPartyTable_edit, false)
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
      tableNameChange() {
        const t = this.form.tableName
        if (t!==undefined && func.notEmpty(t) && /[A-Z]/.test(t)) {
          this.$nextTick(()=>{
            this.form.tableName = t.toLowerCase()
          })

        }
      },
      addList() {
        this.$refs.crud.rowAdd()
        this.form = {isExistData:"2"}
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
        if (this.permission.thirdPartyTable_edit && property === "index") {
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
        for (let item of this.columnData) {
          if (func.isEmpty(item.columnName)) {
            this.$message.warning("字段名称不能为空！");
            loading()
            return;
          }

          if (func.isEmpty(item.columnType)) {
            this.$message.warning("字段类型不能为空！");
            loading()
            return;
          }
        }
        let tips = "是否确认保存？"
        if (row.status === 1) {
          tips = "衍生数据库：《"+ this.form.$databaseId + '》将会创建表名称为：《' + row.tableName +"》的表结构，其中包含" +
            this.columnData.length+"个字段，是否确认保存？"
        }

        this.$confirm(tips, {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        })
          .then(() => {
            this.columnData.forEach(i=>{
              i.columnNotNull = i.columnNotNullF[0]==="1"?1:0
              i.columnIsPrimaryKey = i.columnIsPrimaryKeyF[0]==="1"?1:0
            })
            const data = {
              ...row,
              columnList : this.columnData
            }
            add(data).then(() => {
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
          })



      },
      rowUpdate(row, index, done, loading) {
        console.log("this.columnData",this.columnData,this.delData)
        this.columnData.forEach(i=>{
          if (i.columnNotNullF) {
            i.columnNotNull = i.columnNotNullF[0]==="1"?1:0
          }
          if (i.columnIsPrimaryKeyF) {
            i.columnIsPrimaryKey = i.columnIsPrimaryKeyF[0]==="1"?1:0
          }

        })
        //console.log("this.columnData",this.columnData,this.delData)
        const data = {
          ...row,
          columnList : this.columnData.concat(this.delData)
        }
        update(data).then(() => {
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
        this.$confirm("确定将选择数据删除?同时会销毁已创建的表结构！", {
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
        this.columnData = []
        this.delData = []
        if (["edit", "view"].includes(type)) {
          this.readOnly = false
          getDetail(this.form.id).then(res => {

            this.form = res.data.data;
            this.columnData = res.data.data.columnVoList
            if (this.form.status === 1) {
              const statusColumn = this.findObject(this.option.column, "status");
              statusColumn.editDisabled = true

              const tableNameColumn = this.findObject(this.option.column, "tableName");
              tableNameColumn.editDisabled = true


            } else {
              const statusColumn = this.findObject(this.option.column, "status");
              statusColumn.editDisabled = false

              const tableNameColumn = this.findObject(this.option.column, "tableName");
              tableNameColumn.editDisabled = false
            }
            this.columnData.forEach(i=>{
              if (this.form.status === 1 && "1" === i.isExistData) {
                i.$cellEdit = false
              }else {
                i.$cellEdit = true
              }

              if (i.columnNotNull ==="1") {
                i.columnNotNullF = ["1"]
              }
              if (i.columnIsPrimaryKey === "1") {
                i.columnIsPrimaryKeyF = ["1"]
              }
              if (i.columnLong ===null) {
                i.columnLong = undefined
              }

              if (i.columnDecimal ===null) {
                i.columnDecimal = undefined
              }
              // i.columnNotNullF = [i.columnNotNull]
              // i.columnIsPrimaryKeyF = [i.columnIsPrimaryKey]
            })
          });
        } else {
          this.readOnly = false
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
          databaseId,tableName,tableComment
        } = this.query;

        let values = {
          databaseId,tableName,tableComment
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


