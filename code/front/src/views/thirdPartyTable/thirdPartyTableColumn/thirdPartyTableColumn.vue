<template>
  <avue-crud :option="option"
             :data="data"
             :table-loading="loading"
             :before-open="beforeOpen"
             @sortable-change="sortChange"
             @selection-change="selectionChange"
             @filter-method="filterMethod"
             v-model="form"
             ref="crud">
    <!-- 左边按钮 -->
    <template #menu-left="{ row,index }">
      <el-button class="button-size" :disabled="readOnly" round title="新增" size="small" icon="el-icon-plus" @click.stop="addList()">
      </el-button>
      <el-button class="button-size" :disabled="readOnly" round title="删除" size="small" icon="el-icon-delete"
                 @click.stop="handleDelete()">
      </el-button>
      <el-button class="button-size" :disabled="readOnly" round title="复制" size="small" icon="el-icon-document-copy" @click.stop="copyList()">
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

</template>

<script>

  import { getDetail} from "@/api/thirdPartyTableColumn/thirdPartyTableColumn";
  import 'nprogress/nprogress.css';
  import Pinyin from 'pinyin';
  import func from "@/utils/func";

  export default {
    props :{
      data: {
        type: Array,
        default: () => {
          return [];
        }
      },
      delData: {
        type: Array,
        default: () => {
          return [];
        }
      },
      readOnly: {
        type: Boolean,
        default: false,
      }
    },
    data() {
      return {
        form: {},
        query: {},
        search: {},
        loading: false,
        selectionList: [],
        option: {

          selectionFixed: false,
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
          searchShow: false,
          refreshBtn: false,
          gridBtn:false,
          filterBtn:false,
          columnBtn:false,
          searchMenuSpan: 6,
          border: true,
          viewBtn: true,
          sortable:true,
          selection: true,
          dialogClickModal: false,
          column: [

            {
              label:'序号',
              prop:'index',

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
              filters: true,
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            // {
            //   label: "第三方表id",
            //   prop: "tableId",
            //   type: "input",
            // },
            {
              label: "字段中文名",
              prop: "columnInfoName",
              type: "input",
              cell: true,
              rules: [{
                required: true,
                message: "字段中文名",
                trigger: "blur"
              }],
              change: this.columnInfoNameChange
            },
            {
              label: "字段名称",
              prop: "columnName",
              type: "input",
              cell: true,
              rules: [{
                required: true,
                message: "请输入字段名称",
                trigger: "blur"
              }]
            },
            {
              label: "类型",
              prop: "columnType",
              type: "select",
              filterable:true,
              cell: true,
              width: 140,
              rules: [{
                required: true,
                message: "请选择字段类型",
                trigger: "blur"
              }],
              change: this.columnTypeChange,
              dicData: [
                {label:'tinyint', value:'tinyint'},{label:'smallint', value:'smallint'},
                {label:'mediumint', value:'mediumint'},
                {label:'int', value:'int'},
                {label:'bigint', value:'bigint'},
                {label:'float', value:'float'},
                {label:'double', value:'double'},
                {label:'decimal', value:'decimal'},
                {label:'bit', value:'bit'},
                {label:'char', value:'char'},
                {label:'varchar', value:'varchar'},
                {label:'text', value:'text'},
                {label:'tinytext', value:'tinytext'},
                {label:'mediumtext', value:'mediumtext'},
                {label:'longtext', value:'longtext'},
                {label:'blob', value:'blob'},
                {label:'tinyblob', value:'tinyblob'},
                {label:'mediumblob', value:'mediumblob'},
                {label:'longblob', value:'longblob'},
                {label:'date', value:'date'},
                {label:'datetime', value:'datetime'},
                {label:'timestamp', value:'timestamp'},
                {label:'time', value:'time'},
                {label:'year', value:'year'}
              ]
            },
            {
              label: "长度",
              width: 100,
              prop: "columnLong",
              type: "number",
              controls:false,
              cell: true,
            },
            {
              label: "小数点",
              width: 100,
              controls:false,
              prop: "columnDecimal",
              type: "number",
              cell: true,
            },
            {
              label: "是否必填",
              prop: "columnNotNullF",
              type: 'checkbox',
              width: 80,
              align: "center",
              headerAlign: "center",
              dicData:[{
                label:'',
                value: "1"
              }],
              cell: true,
            },
            {
              label: "是否主键",
              prop: "columnIsPrimaryKeyF",
              type: 'checkbox',
              width: 80,
              align: "center",
              headerAlign: "center",
              dicData:[{
                label:'',
                value: "1"
              }],
              cell: true,
            },
            {
              label: "字段注释",
              prop: "columnInfoDesc",
              type: "input",
              cell: true,
            },
            {
              label: "数据写入说明",
              prop: "columnInfoDataStandard",
              type: "input",
              cell: true,
            },
            {
              label: "数据写入",
              prop: "isExistData",
              type: "select",
              disabled: true,
              width: "90",
              // hide: true,
              dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=dataStatus",
              placeholder: ' ',
              props: {
                label: 'dictValue',
                value: 'dictKey'
              },
            },
            {
              label: "创建人",
              prop: "createUser",
              type: "select",
              dicMethod: "post",
            dicUrl: "/api/blade-system/user/user-kv-list",
              width: "80",
              dicFormatter: (res) => {
                return res.data
              },
              props: {
                label: 'key',
                value: 'value',
              },
              // addDisplay: false,
              // editDisplay: false,
              // viewDisplay: false,
              // hide: true,
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
              width: "140"
              // addDisplay: false,
              // editDisplay: false,
              // viewDisplay: false,
              // hide: true,
            },
            // {columnInfoDesc
            //   label: '附件列表',
            //   span: 24,
            //   width: "100",
            //   prop: 'uploadList',
            //   slot: true,
            //   display: false,
            // },
          ]
        },

        timer: null,
      };
    },
    computed: {
      ids() {
        let ids = [];
        this.selectionList.forEach(ele => {
          ids.push(ele.id);
        });
        return ids.join(",");
      },
      // tableShowData() {
      //   return this.data.filter(i=>i.isDeleted !==1)
      // }
    },
    methods: {
      controlSingel({event}) {
        console.log("event.target.checked",event.target.checked)
        let that = this;
        window.setTimeout(() => {
          if (!this.changed) {
            event.target.checked = false;
          }
          that.changed = false;
        }, 0);
      },
      //改变change的状态
      retainRecord() {
        // 可以写些单选框选中的代码处理
        this.changed = true;
      },
      columnTypeChange(data) {
        //console.log("dat111a",data)
        if (data.value === 'varchar') {
          data.row.columnLong = 255
        }
      },
      columnInfoNameChange({row,value}) {
        if(func.isEmpty(value)) {
          return
        }
        // 将汉字转换为拼音，获取拼音的首字母
        const pinyinResult = Pinyin(value, { style: Pinyin.STYLE_FIRST_LETTER });
        // 拼音首字母可能是数组，所以取第一个字的首字母并返回
        const pinyin =  pinyinResult.map(item => item[0].toUpperCase()).join('');
        //row.columnName = pinyin
        row["columnName"] = pinyin
        //console.log(" this.form.columnName===>change")
      },
      addList() {
        this.data.push({ $cellEdit:true})
       // this.$emit("addList")
        console.log("data",this.data)
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
        if (property === "index") {
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
      selectionChange(list) {
        this.selectionList = list;
      },
      filterMethod({row}) {
        console.log("ffff")
        return row.isDeleted !== 1;
      },
      handleDelete() {
        if (this.selectionList.length === 0) {
          this.$message.warning("请选择至少一条数据");
          return;
        }
        for (let i = 0; i < this.selectionList.length; i++) {
          if (this.selectionList[0].isExistData === "1") {
            this.$message.warning("已经写入数据的列不允许删除！");
            return;
          }
        }
        //this.selectionList[0].isDeleted = 1
        const delIndex = this.selectionList.map(i=>i.$index)
        const that = this
        this.data.forEach(function (item,index,arr){
          if (delIndex.indexOf(item.$index)>-1) {
            item.isDeleted = 1
            that.delData.push(item)
            arr.splice(index,1);
          }
        });

      },
      // sortChange(e) {
      //   //console.log("chang...",e,this.tableShowData,this.data)
      // },
      beforeOpen(done, type) {
        if (["edit", "view"].includes(type)) {
          getDetail(this.form.id).then(res => {
            this.form = res.data.data;
          });
        }
        done();
      },

    }
  };
</script>


