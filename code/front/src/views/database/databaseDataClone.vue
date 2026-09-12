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
    </avue-crud>
    <!-- 左下按钮 -->
    <page-attach ref="pageAttach"></page-attach>
  </basic-container>
</template>

<script>
import {getPage,getDatasourceByMenuTable,getTableListByMenuTable as getTableList } from "@/api/database/configDatabaseDataClone";
import {mapGetters} from "vuex";
import _ from 'lodash'
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
      loadingOption: {
        lock: true,
        text: '物理表读取中',
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
        // height: 'auto',
        searchShowBtn:false,
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
            label: "数据库",
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
  },
  computed: {
    ...mapGetters(["permission"]),
    permissionList() {
      return {
        addBtn: this.vaildData(this.permission.databaseDataClone_add, false),
        viewBtn: this.vaildData(this.permission.databaseDataClone__view, false),
        delBtn: this.vaildData(this.permission.databaseDataClone__delete, false),
        editBtn: this.vaildData(this.permission.databaseDataClone__edit, false)
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
    rowView(row, {property}) {
      if (property === "index") {
        this.$refs.crud.rowView(row, row.$index)
      }
    },
    initData() {
      let datasourceDicData = this.findObject(this.option.column, "datasourceId")
      getDatasourceByMenuTable()
          .then(res => {
            datasourceDicData.dicData = res.data.data
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
