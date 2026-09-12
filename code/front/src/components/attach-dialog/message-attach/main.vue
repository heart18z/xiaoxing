<template>
  <div>
    <!-- 查看和附按钮 -->
    <el-dialog :title="justRead?'查看附件':'上传附件'"
               v-model="showAttchDialog" draggable
               :append-to-body="true" :modal='false' width="50%"
               :before-close="beforeDiaClose">
      <avue-crud ref="fileCrud" @row-dblclick="editList" :data="attachList" :option="attachOption"
                 @row-save="rowSave"
                 :table-loading="showAttachLoading"
                 @selection-change="fileSelectionChange">
        <template #menu-left="{ row,index }" v-if="!justRead">
        <span class="file-upload-tip"
              v-if="currOss!=null&&currOss!=undefined">当前储存桶信息：<span>储存桶编号：【{{currOss.ossCode}}】，</span><span>{{currOss.desc}}</span></span>
          <span class="file-upload-tip" style="color: red" v-else>当前未设置储存地址，请先设置储存地址后上传</span>
          <el-row class="file-upload-tip1" v-if="fileMaxSize !=null "><span>当前文件最大为{{fileMaxSize}}（MB）</span>
          </el-row>
        </template>
        <template #menu-right="{ row,index }">
          <!--          <el-button icon="el-icon-refresh" size="small" circle @click="refreshFileSourceCrud()"-->
          <!--                     title="刷新"></el-button>-->
          <el-dropdown @command="handleCommand" v-if="!justRead">
            <el-button size="small" icon="el-icon-plus" circle title="新增文件">
            </el-button>
            <el-dropdown-menu slot="dropdown">
              <el-upload class="inline-block"
                         action
                         :http-request="uploadFileHttp"
                         :show-file-list="false" multiple>
                <el-dropdown-item icon="el-icon-folder">
                  <span>本地</span>
                </el-dropdown-item>
              </el-upload>
              <el-dropdown-item icon="el-icon-box" command="uploadByExistsFile"><span>文件库</span></el-dropdown-item>
              <el-dropdown-item icon="el-icon-box" command="uploadByRowAttach"><span>所选数据附件</span></el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
          <el-button class="inline-block" v-if="!justRead" size="small" icon="el-icon-link" @click="changeOss" circle
                     title="更换储存地址"></el-button>
          <el-button class="button-size" round v-if="!justRead" title="删除" size="small" icon="el-icon-delete"
                     @click.stop="deleteFile()">
          </el-button>

        </template>
        <template #diyFileName="{ row }">
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
            <template #reference><el-button type="primary" link>{{row.diyFileName}}</el-button></template>
          </el-popover>
        </template>
        <template #uploadProcess="{ row }">
          <el-progress :text-inside="true" :stroke-width="18"
                       :format="percentageFmt"
                       :color="customColors" :percentage="row.uploadProcess"></el-progress>
        </template>
        <template #attachSize="{ row }">
          <el-tooltip class="item" effect="dark" :content="formatterAttachSize(row.attachSize,'KB')" placement="top-start">
            <span>{{formatterAttachSize(row.attachSize,"MB")}}</span>
          </el-tooltip>
        </template>
      </avue-crud>
    </el-dialog>
    <el-dialog title="视频播放" class="video-dialog" draggable custom-class="my-video-dialog"
               v-model="isShowVideo" :modal='false' width="80%" :append-to-body="true">
      <div class="play-video-box">
        <video id="video" :src="playUrl" controls="controls"></video>
      </div>
    </el-dialog>
    <file-preview ref="previewArea"/>
    <el-dialog title="更换储存地址" v-model="changeOssDia"
               draggable :append-to-body="true" :modal='false' width="40%">
      <avue-select style="width: 100%" all v-model="currOssId" :clearable="false" placeholder="请选择oss"
                   @change="ossChange"
                   :dic="ossList"></avue-select>
    </el-dialog>
    <userinfo ref="userinfo"></userinfo>
    <pickAttach ref="pickAttach" @submitAttach="submitAttach"></pickAttach>
  </div>
</template>

<script>
import pickAttach from "@/components/attach-dialog/pick-attach/main.vue"
import func from "@/utils/func";
import {Base64} from "js-base64";
import filePreview from "@/components/file-previewer/main.vue";
import {getListBySource, removeFile,
  // submit
} from "@/api/system/attachSource"
import {
  putFileAttach
} from "@/api/resource/oss";
import {getList as ossList} from "@/api/resource/oss";
import {getValueByKey} from "@/api/system/param";
import {introduceOssCode} from "@/const/system/common";
// import func from "@/utils/func";
// import axios from 'axios'

export default {
  name: 'MessageAttach',
  components: {
    filePreview,  pickAttach
  },
  props: {
    containData: {
      type: Array,
      default: () => {
        return [];
      }
    },
    defaultSelect: {
      type: String,
      default: "read"
    },
    sourceId: {
      type: String,
      default: ""
    },
    // 列表主键名称
    keyName: {
      type: String,
      default: "id"
    },
    justRead: {
      type: Boolean,
      default: false
    },
    fileTypeDicUrl: {
      type: String,
      default: "/api/blade-system/dict-biz/dictionary?code=faid"
    }
  },
  data() {
    return {
      sourceIds:[],
      regionList: [],
      fileMaxSize: null,
      customColors: [
        {color: '#e6a23c', percentage: 40},
        {color: '#1989fa', percentage: 99},
        {color: '#5cb87a', percentage: 100},
      ],
      pickExistFileDialog: false,
      //isChanged: false,
      defaultOssId: "",
      adminName: "",
      ossList: [],
      currOssId: "",
      currOss: null,
      changeOssDia: false,
      isShowVideo: false,
      playUrl: "",
      fileSelectionList: [],
      showAttachLoading: false,
      showAttchDialog: false,
      attachList: [],
      attachOption: {
        selectionFixed: false,
        selection: true,
        border: true,
        addBtn: false,
        viewBtn: true,
        searchBtn: false,
        tree: true,
        columnBtn: false,
        tip: false,
        menu: false,
        dialogDrag: true,
        refreshBtn: false,
        searchShowBtn: false,
        emptyBtn: false,
        searchMenuSpan: 4,
        height: 400,
        column: [{
          label: '文件名称',
          prop: 'diyFileName',
          slot: true,
          width: 270,
          cell: true,
          rules: [{
            required: true,
            message: "请输入文件名称",
            trigger: "blur"
          }],
        },
          // {
          //   label: '文件类型',
          //   prop: 'attachExtension',
          //   cell: true,
          //   dicMethod: "post",
          // dicUrl: this.fileTypeDicUrl,
          //   props: {
          //     label: "paramName",
          //     value: "paramValue"
          //   },
          //   type: 'select',
          //   //"file_crosswise_budget"
          //
          //   rules: [{
          //     required: true,
          //     message: "请选择文件类型",
          //     trigger: "change"
          //   }],
          //
          // },

          //   {
          //   label: '文件原名',
          //   prop: 'originalName',
          //   width: 150,
          //   slot: true
          // },

          {
            label: '文件格式',
            prop: 'extension',
            hide:true
          },
          {
            label: '文件大小',
            prop: 'attachSize'
          },
          {
            label: '上传状态',
            prop: 'uploadProcess',
            slot: true,
          }
        ]
      },
      data: [],
      progress: 0
    };
  },
  mounted() {
    // this.showAttachDialog()
  },

  computed: {
    sourceCurrId() {
      return this.sourceId
    }
    // fileIds() {
    //   let ids = [];
    //   this.fileSelectionList.forEach(ele => {
    //     ids.push(ele.attachSourceId);
    //   });
    //   return ids.join(",");
    // },
    // originalFileIds() {
    //   let ids = [];
    //   this.fileSelectionList.forEach(ele => {
    //     ids.push(ele.id);
    //   });
    //   return ids.join(",");
    // },

  },
  watch: {
    isShowVideo(newValue) {
      let video = document.getElementById('video')
      if (!newValue) {
        video.pause()
        this.playUrl = ''
      }
    },
    // progress(newValue) {
    //   console.log("val", newValue)
    // }
  },
  methods: {
    getAttachList(){
      return this.attachList
    },
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
    getPhysicalPosition(row) {
      const  data = this.regionList.find(item=>item.id == row.filePhysicalPosition)
      if (func.notEmpty(data) && func.notEmpty(data.desc)) {
        return  "物业号：【" + data.propertyCode + "】"+"\n"+
            "区域号：【"+data.regionCode+"】"+"\n"+
            "位置号：【"+data.positionCode+"】"+"\n"+
            "管理人：【" + data.admin + "】"
      }
      return "实物⽆需保存"
    },
    percentageFmt(percentage) {
      if (percentage === 0) {
        return "准备中（0%）"
      } else if (percentage > 0 && percentage < 80) {
        return "上传中（" + percentage + "%）"
      } else if (percentage >= 80 && percentage < 100) {
        return "解析中（" + percentage + "%）"
      } else if (percentage === 100) {
        return "上传成功（" + percentage + "%）"
      } else if (percentage === -1) {
        return "上传失败！请删除后重试"
      }
    },
    toUploadFile(file, rowData) {
      // file为文件信息
      const that = this
      let formDate = new FormData()
      formDate.append("file", file)
      formDate.append("ossCode", this.currOss.ossCode)
      // const token = 'token值'  //若后端不需要token验证，则不需传入
      const url = '/api/blade-resource/oss/endpoint/put-file-attach'
      // eslint-disable-next-line
      axios({
        method: 'post',
        url,
        data: formDate,
        timeout: 600000,
        headers: {'Content-Type': 'multipart/form-data'},
        onUploadProgress: function (progressEvent) {
          const complete = parseInt((progressEvent.loaded / progressEvent.total * 80 | 0))
          // 这里为上传的进度
          that.progress = complete
          rowData.uploadProcess = complete
        }
      }).then(res => {
        // 上传成功后续处理
        const data = res.data.data
        rowData.id = data.attachId
        rowData.link  = data.link
        rowData.uploadProcess = 100
        if (res.data.success && this.attachList.find(item => item.uid === rowData.uid) != undefined) {
          that.$message.success('上传完成！')
        }
      }).catch(err => {
        rowData.uploadProcess = -1
        // 捕获异常并处理
        console.log(err)
        //that.$message.success('上传失败！')
      })
    },
    submitAttach(files) {
      // console.log(files)
      if (func.notEmpty(files)) {
        for (let fileData of files) {
          const rowData = {
            originalName: fileData.originalName,
            diyFileName: fileData.originalName,
            attachSize: fileData.attachSize,
            link: fileData.link,
            domain: fileData.domain,
            extension: fileData.extension,
            $cellEdit: true,
            attachExtension: this.defaultSelect,
            sourceId: this.sourceId,
            id: fileData.id,
            sourceType: 2,
            filePhysicalPosition: fileData.filePhysicalPosition,
            uploadProcess: 100
          }
          this.attachList.push(rowData)
        }

      }


    },
    handleCommand(command) {
      if (command == 'uploadByExistsFile') {
        this.uploadByExistsFile()
      } else if (command == 'uploadByRowAttach') {
        this.uploadByRowAttach()
      }
    },
    adminsInfoDiaOpen() {

    },
    notEmpty: func.notEmpty,
    initOss() {
      // console.log("init---oss")
      this.currOss = null
      this.currOssId = ''
      ossList().then(res => {
        const data = res.data.data.records
        const selectData = []
        for (let d of data) {
          if (this.introduceOss !== true && d.ossCode === introduceOssCode) {
            continue;
          }
          d.value = d.id
          d.label = d.ossCode
          d.desc = "资源地址：【" + d.endpoint + "】,空间名：【" + d.bucketName + "】"
          if (this.introduceOss === true) {
            if(d.status == 2 && d.ossCode === introduceOssCode){
              this.currOss = d
              this.currOssId = d.id
              this.defaultOssId = d.id
            }
          }else if (d.status == 2 && d.ossCode !== introduceOssCode) {
            this.currOss = d
            this.currOssId = d.id
            this.defaultOssId = d.id
          }
          selectData.push(d)
        }
        this.ossList = selectData
        //console.log("data",data)
      })

    },
    initMaxFileSize() {
      getValueByKey("miniofile").then(res => {
        if (func.notEmpty(res.data.data)) {
          this.fileMaxSize = res.data.data
        }
      })
    },
    ossChange({item}) {
      this.currOss = item
    },
    changeOss() {
      this.changeOssDia = true
    },
    beforeDiaClose(done) {
      if ((!this.justRead) && (this.defaultOssId != this.currOssId)) {
        this.$message.warning("下次上传将继续保持系统默认存储桶地址！")
      }
      this.attachList = []
      done()
    },
    //双击编辑列表
    editList(row) {
      if (!this.justRead) {
        row.$cellEdit = true
      }
    },
    // 保存列表
    async rowSave() {
      if (func.isEmpty(this.attachList) ||this.attachList.length ==0) {
        this.$message.warning("请先上传文件后再保存！")
        return false;

        // return;
      }
      for (let item of this.attachList) {
        if (func.isEmpty(item.id)) {
          this.$message.warning("请等待文件上传完成后保存！");
          return false;
        }
      }
      return  await this.$refs.fileCrud.validateCellForm().then(res => {
        //console.log("res",res)
        if (res == null || res == undefined || JSON.stringify(res) == "{}") {
          return true
        } else {
          console.log('error submit!!');
          this.$message.warning("请完善附件信息！");
          return false;
          //   return false;
        }
      })
    },
    uploadByExistsFile() {
      // this.pickExistFileDialog = true
      this.$refs.pickAttach.show()
    },
    uploadByRowAttach() {
      this.$refs.pickAttach.show(
         "ids",
     this.sourceIds
      )
    },
    //上传文件
    uploadFileHttp(req) {
      if (func.isEmpty(this.currOss)) {
        this.$message.warning("请先配置好对象存储信息！")
        return
      }
      const fileData = req.file
      let fileName = fileData.name
      if (func.notEmpty(this.fileMaxSize)) {
        const fileSize = req.file.size
        const maxSize = parseInt(this.fileMaxSize) * 1024 * 1024
        if (fileSize > maxSize) {
          this.$message.warning("文件大小超出上限！")
          return;
        }
      }

      // if (func.notEmpty(fileName)) {
      //   fileName = fileName.substring(0, fileName.lastIndexOf("."))
      // }
      const rowData = {
        uid: fileData.uid,
        originalName: fileName,
        diyFileName: fileName,
        attachSize: req.file.size,
        extension: req.file.type,
        $cellEdit: true,
        attachExtension: this.defaultSelect,
        sourceId: this.sourceId,
        link: fileData.link,
        domain: fileData.domain,
        id: "",
        filePhysicalPosition: "",
        uploadProcess: 0,
        sourceType: 2,
        $filePhysicalPositionCellEdit: true
      }

      // console.log("attachList",this.attachList)
      this.attachList.push(rowData)
      this.toUploadFile(req.file, rowData)

    },
    // 替换文件按钮点击触发，模拟点击真正的上传按钮，这样设计是为了给upload 设置一个校验
    beforeReplace() {
      return;
      // if (this.fileSelectionList == null || this.fileSelectionList.length != 1) {
      //   this.$message.warning("请至少选择一条数据进行文件替换！");
      //   return
      // } else {
      //   let e = document.createEvent("MouseEvents");
      //   e.initEvent("click", true, true);
      //   document.getElementById("replaceFileHttpBt").dispatchEvent(e);
      // }
    },
    // 替换文件
    replaceFileHttp(req) {
      let formDate = new FormData()
      formDate.append("file", req.file)
      putFileAttach(formDate).then(res => {
        if (res.data.code == 200) {
          const fileData = res.data.data
          this.fileSelectionList[0].attachSize = req.file.size;
          this.fileSelectionList[0].extension = req.file.type;
          this.fileSelectionList[0].$cellEdit = true;
          this.fileSelectionList[0].id = fileData.attachId
          this.$message.success("文件替换成功，保存之后生效！")
        }
      })
    },
    // 文件选择框
    fileSelectionChange(list) {
      this.fileSelectionList = list
    },
    // 刷新文件列表
    refreshFileSourceCrud() {
      this.getFileData()
    },
    // 点击复制按钮
    handleCopy() {
      this.$message.success("复制成功");
    },
    // 删除文件
    deleteFile() {
      if (this.fileSelectionList.length === 0) {
        this.$message.warning("请选择至少一条数据");
        return;
      }
      let notice = "确定将选择数据删除?";
      for (let f of this.fileSelectionList) {
        if (func.isEmpty(f.attachSourceId)) {
          notice = "当前选中数据包含未保存文件，确定删除吗？"
        }
      }
      this.$confirm(notice, {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        return removeFile({attachSourceList: this.fileSelectionList});
        // this.attachList.
      }).then(() => {
        this.attachList = this.attachList.filter(item=>
            !this.fileSelectionList.includes(item)
        )
        this.refreshFileSourceCrud()
        this.$message({
          type: "success",
          message: "操作成功!"
        });
        this.$refs.fileCrud.toggleSelection();
      });
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
        // window.open(this.$store.getters.systemParam["fview"]+this.website.filePreviewUrl + encodeURIComponent(Base64.encode(row.link)));
        // if (!isInnerIPFn(row.link)) {
        //   // eslint-disable-next-line
        //   window.open(this.$store.getters.systemParam["fview"]+this.website.filePreviewUrl+ encodeURIComponent(Base64.encode(row.link)));
        //   //window.open('https://view.xdocin.com/view?src=' + row.link)
        // } else {
        //   this.$message.warning("文件地址内网不可预览，请下载后预览！")
        // }
      }
    },
    // 下载
    handleDownload(row) {
      window.open(`${row.link}`);
    },
    getFileData() {
      if (func.isEmpty(this.sourceCurrId)) {
        return
      }
      this.showAttachLoading = true
      const params = {}
      params.sourceId = this.sourceCurrId
      const uploadingData = this.attachList.filter((item)=>{return func.isEmpty(item.attachSourceId)})
      getListBySource(params).then(res => {
        for (let item of res.data.data) {
          item.uploadProcess = 100
          if (func.isEmpty(item.filePhysicalPosition)) {
            item.filePhysicalPosition = ""
          }
        }
        // res.data.data.concat(uploadingData)
        this.attachList = res.data.data.concat(uploadingData)
        // if (uploadingData)
        // this.attachList.concat(uploadingData)
        this.showAttachLoading = false
      })
    },
    // 展示附件列表
    showAttachDialog(data,sourceIds) {
      this.attachList = data
      this.sourceIds = sourceIds
      this.initOss()
      this.initMaxFileSize()
      this.showAttchDialog = true
      this.getFileData()
    },

  }
}
</script>
<style>
.my-video-dialog {
  height: 80%;
}
.attach-popper-class{
  white-space: pre-line;
}
</style>

<style scoped>
.attach-popper-class{
  white-space: pre-line;
}

:deep(.el-dropdown + .el-button) {
  margin-left: 0px !important;
}

.span-line:hover {
  text-decoration: underline;
  text-decoration-color: #1e9fff
}

.file-upload-tip {
  line-height: 14px;
  font-size: 12px;
  color: #8f8686;
  position: absolute;
  bottom: 2px
}

.file-upload-tip1 {
  line-height: 14px;
  font-size: 12px;
  color: #8f8686;
  position: absolute;
  bottom: 20px
}

.footer-right-bottom {
  position: relative;
  float: right;
  bottom: 0px;
}

.inline-block {
  display: inline-block;
}

:deep(.video-dialog .el-dialog) {
  margin-left: 260px;
  height: 70vh;

}

:deep(.video-dialog .el-dialog .play-video-box) {
  width: 100%;
  height: 60vh;
  position: relative;
}

video {
  position: absolute;
  top: 50px;
  left: 0;
  width: 100%;
  height: 90%;
}

</style>
