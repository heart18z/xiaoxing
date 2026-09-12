<template>
  <basic-container>
    <avue-crud :option="option"
               :table-loading="loading"
               :data="data"
               v-model:search="query"
               v-model:page="page"
               :permission="permissionList"
               v-model="form"
               ref="crud"
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
        <el-tooltip class="item" effect="dark" style="color: green" content="恢复文件：只能恢复最近一条被删除的关联数据" placement="top-start">
          <el-button class="button-size" round title="恢复"  size="small" icon="el-icon-connection" @click.stop="restore()">

          </el-button>
        </el-tooltip>
        <el-button class="button-size" round title="逻辑删除" size="small" icon="el-icon-delete"
                   @click.stop="handleDelete()"> </el-button>
<!--        <el-button class="button-size" round title="数据清除" style="color: #ffbf2b" size="small" icon="el-icon-delete"-->
<!--                   @click.stop="handleDataDelete()"> </el-button>-->
        <el-button class="button-size" round title="彻底删除" style="color: #fd6458"  size="small" icon="el-icon-delete"
                   @click.stop="handleCompleteDelete()">
        </el-button>

      </template>
      <template #menu-right="{ row,index }" >

        <el-upload class="inline-bloc"
                   action
                   :http-request="replaceFileHttp"
                   :show-file-list="false" multiple >
          <el-button id="replaceFileHttpBt"  size="small" v-show="false" icon="el-icon-sort" circle title="替换文件"></el-button>
        </el-upload>
        <el-button  size="small" icon="el-icon-sort" @click="beforeReplace" circle title="替换文件"></el-button>

      </template>
      <template #attachSize="{ row }">
        <el-tooltip class="item" effect="dark" :content="formatterAttachSize(row.attachSize,'KB')" placement="top-start">
          <span>{{formatterAttachSize(row.attachSize,"MB")}}</span>
        </el-tooltip>
      </template>

      <template #originalName="{ row }">
        <el-popover trigger="click" placement="bottom-start">
          <div>
            <el-button size="small" v-if="row.attachExtension !== 'intenetUrl' " @click.stop="handleRowClick(row)">直接预览</el-button>
            <el-button size="small" @click="browserPreview(row)">页签预览</el-button>
            <el-button size="small" v-if="row.attachExtension !== 'intenetUrl' " @click="handleDownload(row)">文件下载</el-button>
            <el-button size="small" v-clipboard:copy="row.link" v-clipboard:success="handleCopy">拷贝路径</el-button>
            <el-popover
                v-if="row.attachExtension !== 'intenetUrl' "
                style="margin-left: 10px;"
                placement="bottom"
                width="200"
                trigger="click"
                popper-class="attach-popper-class"
                :content="getPhysicalPosition(row)">
              <template #reference><el-button size="small">实物存放</el-button></template>
            </el-popover>
          </div>
          <template #reference><el-button type="primary" link>{{ row.originalName }}
            <el-tooltip class="item" effect="dark" content="当前数据所在存储桶未配置未启用！彻底删除将会清除数据！" placement="top-end">
              <i v-show="currDomain != row.domainUrl"  style="color: #ffbf2b" class="el-icon-warning-outline tip-warning"></i>
            </el-tooltip>
            <el-tooltip class="item" effect="dark" content="当前数据已经逻辑删除！可恢复或彻底删除" placement="top-start">
            <i v-show="row.isDeleted ===1" style="color: #fd6458" class="el-icon-document-delete  tip-warning"></i>
            </el-tooltip>
          </el-button></template>
        </el-popover>
      </template>

      <template slot="link" slot-scope="scope">
        <span type="info" >{{scope.row.link }}</span>
      </template>
    </avue-crud>
    <!-- 左下按钮 -->
    <page-attach ref="pageAttach"></page-attach>
    <el-dialog title="视频播放" class="video-dialog" draggable custom-class="my-video-dialog"
               v-model="isShowVideo" :modal='false' width="80%"  :append-to-body="true"  >
      <div class="play-video-box">
        <video id="video" :src="playUrl" controls="controls"></video>
      </div>
    </el-dialog>
    <file-preview ref="previewArea"/>
  </basic-container>
</template>

<script>
import {getList, remove, restore, completeRemove} from "@/api/base/attachRecovery";
import {mapGetters} from "vuex";
import pageAttach from "@/components/attach-dialog/page-attach/main.vue"
import {Base64} from "js-base64";
import filePreview from "@/components/file-previewer/main.vue";
import {
  replaceFiles,getCurrOss
} from "@/api/resource/oss";
// import {getCurrOss} from "../../api/resource/oss";
// import {getList as ossList} from "@/api/resource/oss";
import func from "@/utils/func";
import {getRegionList} from "@/api/system/attachSource";

export default {
  components:{
    pageAttach,filePreview
  },
  data() {
    return {
      regionList:[],
      currDomain: "",
      playUrl: "",
      isShowVideo:false,
      form: {},
      query: {},
      loading: true,
      page: {
        pageSize: 30,
        currentPage: 1,
        total: 0
      },
      selectionList: [],
      option: {
        searchShowBtn:false,
        menu: false,
        height: 'auto',
        calcHeight: 30,
        tip: false,
        searchLabelWidth: 100,
        searchShow: true,
        searchBtn: false,
        emptyBtn: false,
        searchMenuSpan: 6,
        border: true,
        index: true,
        viewBtn: true,
        selection: true,
        dialogClickModal: false,
        column: [
          // {
          //   label: "id",
          //   prop: "id",
          // },
          {
            label: "附件原名",
            prop: "originalName",
            search: true,
            rules: [{
              required: true,
              message: "请输入附件原名",
              trigger: "blur"
            }]
          },
          {
            label: "附件地址",
            prop: "link",
            width: 450,
            slot: true,
            rules: [{
              required: true,
              message: "请输入附件地址",
              trigger: "blur"
            }]
          },

          {
            label: "存储桶名称",
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-resource/attach-recovery/oss-bucket-name-list",
            dicFormatter: (res) => {
              return res.data
            },
            props: {
              label: 'key',
              value: 'value',
            },
            prop: "ossBucketName",
            width: 150,
            search: true,
            rules: [{
              required: true,
              message: "请输入附件地址",
              trigger: "blur"
            }]
          },

          // {
          //   label: "附件域名",
          //   prop: "domainUrl",
          //   search: true,
          //   rules: [{
          //     required: true,
          //     message: "请输入附件域名",
          //     trigger: "blur"
          //   }]
          // },
          // {
          //   label: "附件名称",
          //   prop: "name",
          //   search: true,
          //   rules: [{
          //     required: true,
          //     message: "请输入附件名称",
          //     trigger: "blur"
          //   }]
          // },

          {
            label: '实物保存',
            prop: 'filePhysicalPosition',
            formatter: (row) => {
              if (func.isEmpty(row.filePhysicalPosition)) return "实物⽆需保存";
              const  data = this.regionList.find(item=>item.id == row.filePhysicalPosition)
              if (func.isEmpty(data)) { return "实物⽆需保存";}
              return data.propertyCode+"_"+data.regionCode
            },

          },
          {
            label: "上传来源",
            prop: "uploadSystem",
          },
          {
            label: '上传人',
            span: 24,
            prop: 'createUser',
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-system/user/user-kv-list",
            width: "100",
            dicFormatter: (res) => {
              return res.data
            },
            props: {
              label: 'key',
              value: 'value',
            },
            display: false,
          },

          {
            label: "创建时间",
            prop: "createTime",
            type: "input",
          },
          // {
          //   label: "修改人",
          //   prop: "updateUser",
          //   type: "input",
          // },
          {
            label: '更新人',
            span: 24,
            prop: 'updateUser',
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-system/user/user-kv-list",
            width: "100",
            dicFormatter: (res) => {
              return res.data
            },
            props: {
              label: 'key',
              value: 'value',
            },
            display: false,
          },
          {
            label: "修改时间",
            prop: "updateTime",
            type: "input",
          },
          {
            label: "拓展名",
            prop: "extension",
            search: true,
            width: 60,
            rules: [{
              required: true,
              message: "请输入附件拓展名",
              trigger: "blur"
            }]
          },
          {
            label: "是否删除",
            prop: "isDeleted",
            type: 'select',
            search: true,
            dicData: [
              {
                label: '是',
                value: 1,
              }, {
                label: '否',
                value: 0,
              }],
            // props: {
            //   label: "dictValue",
            //   value: "dictKey"
            // },
            hide: true,
            display: false,
          },
          {
            label: '文件大小',
            prop: "attachSize",
            slot: true,
            width: 120,
            rules: [{
              required: true,
              message: "请输入附件大小",
              trigger: "blur"
            }]
          },
        ]
      },
      data: [],
      attachForm: {},
      attachOption: {
        submitBtn: false,
        emptyBtn: false,
        column: [
          {
            label: '附件上传',
            prop: 'attachFile',
            type: 'upload',
            drag: true,
            loadText: '模板上传中，请稍等',
            span: 24,
            propsHttp: {
              res: 'data'
            },
            action: "/api/blade-resource/oss/endpoint/put-file-attach"
          }
        ]
      }
    };
  },
  computed: {
    ...mapGetters(["userInfo","permission"]),
    permissionList() {
      return {
        addBtn: false,
        editBtn: false,
        viewBtn: false,
        delBtn: false
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
  created() {
    this.initRegion()
  },
  methods: {
    formatterAttachSize(attachSize,u) {
      if (!attachSize) return "";
      if (u == "KB") {
        const num = 1024.00; //byte 1024 * 1024
        return (attachSize / num).toFixed(2) + "KB"; //mb
      } else {
        const num = 1048576.00; //byte 1024 * 1024
        return (attachSize / num).toFixed(2) + "MB"; //mb
      }
    },
    initRegion(){
      getRegionList().then(res=>{
        const data = res.data.data
        for (let d of data) {
          d.value = d.id
          d.label = d.propertyCode+"_"+d.regionCode
          d.desc = "物业号：【" + d.propertyCode + "】,管理人：【" + d.admin + "】"
        }
        data.unshift({value: "", label: "实物⽆需保存"})
        this.regionList=data
      })
    },
    getPhysicalPosition(row) {
      const  data = this.regionList.find(item=>item.regionCode == row.filePhysicalPosition)
      if (func.notEmpty(data) && func.notEmpty(data.desc)) {
        return  "物业号：【" + data.propertyCode + "】"+"\n"+
            "区域号：【"+data.value+"】"+"\n"+
            "位置号：【"+data.positionCode+"】"+"\n"+
            "管理人：【" + data.admin + "】"
      }
      return "实物⽆需保存"
    },
    // 替换文件按钮点击触发，模拟点击真正的上传按钮，这样设计是为了给upload 设置一个校验
    beforeReplace() {
      if (this.selectionList==null || this.selectionList.length !=1) {
        this.$message.warning("请选择一条数据进行文件替换！");
        return
      } else {

        try {
          this.selectionList.forEach((item) => {
            if (item.isDeleted == 1) {
              this.$message.warning("当前选中的数据包含已经删除文件，请选择其他文件后重试！");
              throw ('stop')
            }})
        } catch (e) {
          return;
        }


        if (!this.userInfo.role_name.includes('admin')) {
          this.$message.warning("没有权限替换，请联系管理员！");
          return
        }


        let e = document.createEvent("MouseEvents");
        e.initEvent("click", true, true);
        document.getElementById("replaceFileHttpBt").dispatchEvent(e);
      }
    },
    // 替换文件
    replaceFileHttp(req) {
      let formDate = new FormData()
      formDate.append("file",req.file)
      formDate.append("attachId",this.selectionList[0].id)
      replaceFiles(formDate).then(res => {
        if (res.data.code == 200) {
          // const fileData = res.data.data
          // this.fileSelectionList[0].attachSize = req.file.size;
          // this.fileSelectionList[0].extension = req.file.type;
          // this.fileSelectionList[0].$cellEdit=true;
          // this.fileSelectionList[0].id = fileData.attachId
          this.$message.success("文件替换成功！")
          this.searchReset()
        }
      })
    },
    // 播放视频
    playVideo(item) {
      this.playUrl = item.link
      this.isShowVideo = true
    },
    // 打开预览
    openAttach(item) {
      let _this = this.$refs.previewArea;
      _this.coverUrl(item)
    },
    // 直接预览按钮
    handleRowClick(row) {
      let type = row.extension
      if (type.indexOf('mp4') !== -1 || type.indexOf('avi') !== -1 || type.indexOf('mov') !== -1 ||
        type.indexOf('flv') !== -1 || type.indexOf('mkv') !== -1 || type.indexOf('m4v') !== -1) {
        this.playVideo(row)
      } else {
        this.openAttach(row)
      }
    },
    //浏览器预览
    browserPreview(row) {
      if (row.link) {
        if(row.attachExtension == 'intenetUrl') {
          window.open(row.link);
        } else {
          // eslint-disable-next-line
          window.open(this.$store.getters.systemParam["fview"]+this.website.filePreviewUrl + encodeURIComponent(Base64.encode(row.link)));
        }
        // eslint-disable-next-line
        //window.open(this.$store.getters.systemParam["fview"]+this.website.filePreviewUrl+ encodeURIComponent(Base64.encode(row.link)));
        // if(!isInnerIPFn(row.link)){
        //   window.open('https://view.xdocin.com/view?src=' + row.link)
        //   // eslint-disable-next-line
        //   window.open('http://8.142.42.114:8013/onlinePreview?url='+encodeURIComponent(Base64.encode(row.link)));
        // }else{
        //   this.$message.warning("文件地址内网不可预览，请下载后预览！")
        // }
      }
    },
    // 下载
    handleDownload(row) {
      window.open(`${row.link}`);
    },
    // 点击复制按钮
    handleCopy(){
      this.$message.success("复制成功");
    },
    rowDel(row) {
      this.$confirm("确定将选择数据删除吗?", {
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
    restore(){
      if (this.selectionList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      }
      try {
        this.selectionList.forEach((item) => {
          if (item.isDeleted == 0) {
            this.$message.warning("当前选中的数据包含未删除文件，请取消后重试！");
            throw ('stop')
          }
        });
      }catch (e) {
        return;
      }
      this.$confirm("确定将选择数据恢复吗?", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      })
        .then(() => {
          return restore(this.ids);
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
    handleDelete(){
      if (this.selectionList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      }
      try {
        this.selectionList.forEach((item) => {
          if (item.isDeleted == 1) {
            this.$message.warning("当前选中的数据包已经删除的文件，请先取消后重试！");
            throw ('stop')
          }
        });
      }catch (e) {
        return;
      }
      this.$confirm("确定将选择数据删除吗?此处删除可恢复！", {
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
    handleCompleteDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      }
      for (let item of this.selectionList) {
        if (item.ossBucketName == 'b005') {
          this.$message.warning("您没有权限删除b005桶内的数据，请与管理员联系后重试");
          return;
        }
      }
      let warnText ;
      let warnFilesName = []
      let delFilesName = []
      try {
        this.selectionList.forEach((item) => {
          if (item.isDeleted == 0) {
            this.$message.warning("当前选中的数据包含未删除文件，请先删除后重试！");
            throw ('stop')
          }

          if (item.domainUrl !== this.currDomain){
            warnFilesName.push(item.originalName)
          } else {
            delFilesName.push(item.originalName)
          }
        });
      }catch (e) {
        return;
      }
      if (warnFilesName.length>0) {
        warnText = "所选文件：\n【"+warnFilesName.join("】\n【")+"】\n将会清除数据，但不会删除文件！"
        if(delFilesName.length>0){
          warnText=warnText+"\n【"+delFilesName.join("】\n【")+"】\n将会被彻底删除，不可恢复！"
        }

      } else {
        warnText = "所选文件：\n【"+delFilesName.join("】\n【")+"】\n将会被彻底删除，不可恢复！"
      }
      this.$confirm(warnText, {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        customClass: "confirm-class",
        type: "warning"
      })
          .then(() => {
            return completeRemove(this.ids);
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
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
    },
    refreshChange() {
      this.onLoad(this.page, this.query);
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getList(page.currentPage, page.pageSize, Object.assign(params, this.query)).then(res => {
        const data = res.data.data;
        this.page.total = data.total;
        this.data = data.records;
        for(let item of this.data) {
          let str = item.domainUrl
          if (func.notEmpty(str)) {
            const arr =  str.split("/")
            item.ossBucketName = arr[arr.length-1];
          }else {
            item.ossBucketName = ""
          }

        }
        this.loading = false;
        //console.log(this.data)
        this.selectionClear();
      });
      getCurrOss(this.selectionList[0]).then(res=>{
        // console.log("res,res",res)
        const data = res.data.data
        this.currDomain= data.endpoint+"/"+data.bucketName
      })
    }
  }
};
</script>
<style>
.confirm-class{
  white-space: pre-line!important;
}
</style>
<style scoped>


.tip-warning:hover {
  font-size: 18px!important;
}
</style>
