<template>
  <div>
    <el-dialog title="用户信息"  v-model="showUserDialog" draggable
               :append-to-body="true" :modal='false' width="30%">
      <el-tabs v-model="active"
               type="card">
        <el-tab-pane label="系统管理人"
                     name="1">
        </el-tab-pane>
        <el-tab-pane label="技术支持"
                     name="2">
        </el-tab-pane>
        <el-tab-pane label="功能需求"
                     name="3">
        </el-tab-pane>
      </el-tabs>
      <avue-crud :option="option"
                 :table-loading="loading"
                 :data="actData"
                 ref="crud">
        <template #realName="{ row }">
          {{ row.realName }}
          <el-tooltip class="item" effect="dark" style="color: green" content="该用户为主要负责人" placement="top-start">
            <i v-show="row.identity.indexOf('main')>-1" class="el-icon-medal"></i>
          </el-tooltip>
        </template>
      </avue-crud>
    </el-dialog>
  </div>

</template>

<script>

import {
  getUserContactList as getList
} from "@/api/system/user"
export default {
  name: 'UserInfo',
  data(){
    return{
      active: "1",
      //data:[],
      loading:false,
      showUserDialog:false,
      option:{
        height: 'auto',
        calcHeight: 80,
        tip: false,
        menuBtn:false,
        refreshBtn:false,
        searchShowBtn:false,
        columnBtn:false,
        searchShow: false,
        searchMenuSpan: 6,
        border: true,
        saveBtn: false,
        updateBtn: false,
        cancelBtn: false,
        menu: false,
        addBtn: false,
        delBtn: false,
        editBtn: false,
        searchBtn: false,
        emptyBtn: false,
        // index: true,
        selection: false,
        viewBtn: false,
        dialogType: 'drawer',
        dialogClickModal: false,
        column: [
          {
            label: "用户姓名",
            prop: "realName",
            search: true,
            display: false
          },

          {
            label: "电话",
            prop: "phone",
            display: false,
          },
          {
            label: "微信",
            prop: "wechat",
            display: false,
          },
          {
            label: "邮箱",
            prop: "email",
            display: false,
          }
        ]
      },
      tableData: {
        adminUser: [],
        funcUser: [],
        tecUser: []
      }
    }
  },
  computed: {
    actData(){
      if (this.active == "1") {
        return this.tableData.adminUser
      }else if(this.active == "2") {
        return this.tableData.tecUser
      } else if (this.active == "3") {
        return this.tableData.funcUser
      }

    }
  },
  // mounted() {
  //   this.onLoad()
  // },
  methods:{
    show() {
      this.showUserDialog = true
      this.onLoad()
    },
    // openTag(val) {
    //  console.log(val, this.active)
    //
    // },

    onLoad() {
      this.loading = true;
      getList({isMain: false}).then(res => {
        const resData = res.data.data
        //console.log("resdata",resData)
        if (resData!=null) {
          let  func = function(a){
            return (-0.5-a.identity.indexOf("main"))
          }
          this.tableData.adminUser = resData.adminUser.sort(func)
          this.tableData.tecUser = resData.tecUser.sort(func)
          this.tableData.funcUser = resData.funcUser.sort(func)
        }
        this.loading = false;
      });
    },
  }
}
</script>

<style scoped>
:deep(.avue-crud__menu){
  min-height: 0px!important;
}
</style>