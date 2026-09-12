<template>
  <el-dialog
    :customClass="showExcel == true || showDoc == true ? 'customWidth' :''"
    :model-value="showExcel === true || showPdf === true || showImg === true || showDoc === true || showPPt === true || showEmpty === true"
    @update:model-value="closePreviewClick"
    class="abow_dialog"
    draggable
    :before-close="closePreviewClick"
    :close-on-click-modal="false"
    :append-to-body="true"
    style="text-align: center"
    v-if="iframeState">
    <div class="content-body">
      <div style="position: absolute;left: 2%;font-weight: bold;

      color: #8a979e;text-align: left;z-index: 2000">
        <el-row> {{ archiveNo }}</el-row>
        <el-row>{{ filePhysicalPosition }}</el-row>
        <el-row>{{ keepUser }}</el-row>
      </div>
      <el-row style="z-index: 2000">
        <el-col :span="8"></el-col>
        <el-col :span="6" >
          <div v-if="showPdf == true" style="z-index: 100">
            <el-button-group>
              <el-button type="primary" icon="el-icon-arrow-left" size="small" @click="prePage">上一页</el-button>
              <el-button type="primary" size="small" @click="nextPage">下一页
                <i class="el-icon-arrow-right el-icon--right"></i></el-button>
            </el-button-group>
            <div style="margin-top: 10px; margin-left:60px;color: #409EFF">{{pageNum}} / {{pageTotalNum}}</div>
          </div>
        </el-col>
        <el-col :span="10" style="float: right" v-if="!showDoc&&!showEmpty">
          <el-button @click="enlarge">放大</el-button>
          <el-button @click="narrow">缩小</el-button>
        </el-col>
      </el-row>
      <div v-if="showEmpty===true" style="line-height: 400px">
        当前文件暂不支持预览，请使用页签预览功能或下载查看！
      </div>
      <div ref="imgCont" v-if="showExcel||showDoc||showPdf||showImg||showPPt" @mousewheel.prevent="rollImg($event)"
           style="text-align: center;
          vertical-align: middle;
          position: relative;
          margin-top: 36px;
          width: 100%;
          height: 500px;">

        <div v-if="showExcel === true" class="dialog-body-content-base-style" ref="imgDiv" id="img"
             style="width:100%;height: auto;display: flex;justify-content: center;">
          <div id="result">
            <el-table :data="excelData" style="width: 100%">
              <el-table-column
                v-for="(value, key, index) in excelData[2]"
                :key="index"
                :prop="key"
                :label="key"
              ></el-table-column>
            </el-table>
          </div>
        </div>
        <div v-else-if="showDoc === true"
             style="margin-bottom: 20px;display: flex;justify-content: center;height: auto;width: 100%;padding: 15px;">
          <div class="hemldoc" v-loading="docVisible"
               style="width: 100%"
               element-loading-text="加载中"
               element-loading-spinner="el-icon-loading" v-html="vHtml"/>
        </div>
        <div v-else-if="showPdf === true" style="width: 100%" class="dialog-body-content-base-style" ref="imgDiv"
             id="img">
          <iframe
            :src="pdfUrl"
            style="width: 70%; margin: 0 auto; height: 850px; border: none"
          />
        </div>
        <div v-else-if="showImg === true" ref="imgDiv" class="dialog-body-content-base-style" id="img">
          <img :src="imgUrl" style="max-width: 300px" />
        </div>
        <div v-else-if="showPPt === true" class="dialog-body-content-base-style" ref="imgDiv" id="img"
             style="margin-bottom: 20px;display: flex;justify-content: center;width: 800px;height: 800px">
          <iframe
            :src="pptUrl"
            scrolling="no"
            frameborder="0"
            style=" z-index: 1000;height: 100%;width: 100%"
          ></iframe>
        </div>

      </div>
    </div>


  </el-dialog>
</template>

<script>
import XLSX from 'xlsx';
import mammoth from 'mammoth';
import func from '@/utils/func';
import { Base64 } from 'js-base64';
const Mode = {
  CONTAIN: {
    name: "contain",
    icon: "el-icon-full-screen"
  },
  ORIGINAL: {
    name: "original",
    icon: "el-icon-c-scale-to-original"
  }
};
export default {
  // 可拖动指令
  // directives: {
  //   elDragDialog,
  // },
  /* eslint-disable */
  name: 'previewArea',
  data() {
    return {
      filePhysicalPosition:"",
      keepUser:"",
      archiveNo:"",
      mode: Mode.CONTAIN,
      // 图片参数
      params: {
        zoomVal: 1,
        left: 0,
        top: 0,
        currentX: 0,
        currentY: 0,
      },
      deg: 0,
      scal: 1,
      showPPt: false,
      docVisible: false,
      vHtml: '',
      pageNum: 1,
      pageTotalNum: 1, // 总页数
      loadedRatio: 0, // 当前页面的加载进度，范围是0-1 ，等于1的时候代表当前页已经完全加载完成了
      showImg: false,
      showExcel: false,
      showDoc: false,
      showPdf: false,
      iframeState: false,
      showEmpty:false,
      imgUrl: '',
      url: '',
      pdfUrl: '',
      pptUrl: '',
      currentPage: 0, // pdf文件页码
      pageCount: 0, // pdf文件总页数
      excelData: [],
      workbook: {},
      excelURL: "", //文件地址，看你对应怎末获取、赋值
    }
  },
  components: {},
  created() {
    this.restImg();
  },
  methods: {

    // mode==original 默认放大图片
    originalFunc() {
      this.params.zoomVal = 2;
      this.restFunc();
    },
    // 初始化数据,重置数据
    restImg() {
      this.params.zoomVal = 1;
      this.restFunc();
      this.mode = Mode['CONTAIN'];
    },
    restFunc() {
      this.params.left = 0;
      this.params.top = 0;
      this.params.currentX = 0;
      this.params.currentY = 0;
      this.deg = 0;
      if (this.$refs.imgDiv) {
        let img = this.$refs.imgDiv;
        img.style.transform = `translate(-50%, -50%) scale(${this.params.zoomVal}) rotate(${this.deg}deg)`;
        img.style.left = '50%';
        img.style.top = '50%';
      }
    },
    // 图片滚动放大
    rollImg(event) {
      this.params.zoomVal += event.wheelDelta / 1200;
      this.rollFunc()
    },
    outImg(flag) {
      if (flag == 'out') {
        this.params.zoomVal -= 0.2;
      } else {
        this.params.zoomVal += 0.2;
      }
      this.rollFunc()
    },
    rollFunc() {
      let e = this.$refs.imgDiv;
      if (this.params.zoomVal >= 0.2) {
        e.style.transform = `translate(-50%, -50%) scale(${this.params.zoomVal}) rotate(${this.deg}deg)`;
      } else {
        this.params.zoomVal = 0.2;
        e.style.transform = `translate(-50%, -50%) scale(${this.params.zoomVal}) rotate(${this.deg}deg)`;
        return false;
      }
    },
    // 放大
    enlarge() {
      this.$nextTick(() => {
        this.params.zoomVal += 0.2
        this.rollFunc()
      })
    },
    // 缩小
    narrow() {
      this.$nextTick(() => {
        this.params.zoomVal -= 0.2
        this.rollFunc()
      })
    },
    uploading(file) {
      const xhr = new XMLHttpRequest()
      xhr.open('post', file, true)
      xhr.responseType = 'arraybuffer'
      xhr.onload = () => {
        if (xhr.status === 200) {
          mammoth.convertToHtml({arrayBuffer: new Uint8Array(xhr.response)}).then((resultObject) => {
            this.$nextTick(() => {
              this.vHtml = resultObject.value
              this.docVisible = false
            })
          })
        }
      }
      xhr.send()
    },
    // 上一页
    prePage() {
      let page = this.pageNum
      page = page > 1 ? page - 1 : this.pageTotalNum
      this.pageNum = page
    },
    // 下一页
    nextPage() {
      let page = this.pageNum
      page = page < this.pageTotalNum ? page + 1 : 1
      this.pageNum = page
    },
    closePreviewClick() {
      if (this.showExcel === true) {
        this.showExcel = false
        this.iframeState = false
      } else if (this.showPdf === true) {
        this.showPdf = false
        this.iframeState = false
      } else if (this.showImg === true) {
        this.showImg = false
        this.iframeState = false
      } else if (this.showDoc === true) {
        this.showDoc = false
        this.iframeState = false
      } else if (this.showPPt === true) {
        this.showPPt = false
        this.iframeState = false
      } else if (this.showEmpty ===true) {
        this.iframeState = false
        this.showEmpty =false
      }
    },
    /**
     * 预览
     */
    coverUrl(row) {
      //console.log(row)
      if (row.isPhysicalSave==='1') {
        if (func.notEmpty(row.physicalPositionInfo))  {
          this.filePhysicalPosition = "存档位置："+ row.physicalPositionInfo
          this.keepUser="保管人："+row.keeper
          this.archiveNo="存档编号："+row.archiveNo

        } else {
          this.filePhysicalPosition = "存档位置："
          this.keepUser="保管人："
          this.archiveNo="存档编号："

        }
      } else {
        this.filePhysicalPosition = ""
        this.keepUser=""
        this.archiveNo=""
      }




      let type = this.iconByType(row)
      var fileUrl = row.link
      if (type.indexOf('xsl') !== -1 || type.indexOf('xlsx') !== -1 || type.indexOf('xls') !== -1) {
        this.iframeState = true
        this.url = fileUrl
        this.previewExcel(this.url)
        this.showExcel = true
      } else if (type.indexOf('docx') !== -1) {
        this.iframeState = true
        this.docVisible = true
        this.uploading(fileUrl)
        this.showDoc = true
      } else if (type.indexOf('pdf') !== -1) {
        this.iframeState = true
        this.pdfUrl = fileUrl
        this.showPdf = true
      } else if (type.indexOf('jpg') !== -1 || type.indexOf('png') !== -1 || type.indexOf('jpeg') !== -1) {
        this.iframeState = true
        this.imgUrl = fileUrl
        this.showImg = true
      } else if (type.indexOf('pptx') !== -1 || type.indexOf('ppt') !== -1) {
        this.iframeState = true
        // this.pptUrl = 'https://view.xdocin.com/xdoc?_xdoc=' + encodeURIComponent(fileUrl)
        // eslint-disable-next-line
        this.pptUrl = this.$store.getters.systemParam["fview"]+this.website.filePreviewUrl+encodeURIComponent(Base64.encode(row.link));
        this.showPPt = true
      } else if (type.indexOf('doc') !== -1) {
        this.iframeState = true
        this.showEmpty = true
        this.$message('当前仅支持预览“.docx”文件，请下载查看')
      } else {
        this.iframeState = true
        this.showEmpty = true
        this.$message('当前文件暂不支持预览，请下载查看')
      }
    },
    // 获取下标
    iconByType(row) {
      //return row.filename.substring(row.filename.lastIndexOf('.') + 1, row.filename.length)
      return row.extension
    },
    //  读取excel文件流
    readWorkbook(workbook) {
      var sheetNames = workbook.SheetNames // 工作表名称集合
      var worksheet = workbook.Sheets[sheetNames[0]] // 这里我们只读取第一张sheet
      var csv = XLSX.utils.sheet_to_csv(worksheet)
      document.getElementById('result').innerHTML = this.csv2table(csv)
    },
    csv2table(csv) {
      var html = "<table   class='table'>"
      var rows = csv.split('\n')
      rows.pop() // 最后一行没用的
      rows.forEach(function (row, idx) {
        var columns = row.split(',')
        columns.unshift(idx + 1) // 添加行索引
        if (idx === 0) {
          // 添加列索引
          html += '<tr bgcolor="#ebeffb">'
          for (var i = 0; i < columns.length; i++) {
            html +=
              '<th >' +
              (i === 0 ? '' : String.fromCharCode(65 + i - 1)) +
              '</th>'
          }
          html += '</tr>'
        }
        html += '<tr>'
        columns.forEach(function (column) {
          html += '<td>' + column + '</td>'
        })
        html += '</tr>'
      })
      html += '</table>'
      return html
    },
    // url为文件地址， 需要预览时调用previewExcel方法传入文件地址即可
    previewExcel(url) {
      var xhr = new XMLHttpRequest();
      xhr.open("get", url, true);
      xhr.responseType = "arraybuffer";
      let _this = this;
      xhr.onload = function () {
        if (xhr.status === 200) {
          var data = new Uint8Array(xhr.response);
          var workbook = XLSX.read(data, {type: "array"});
          var sheetNames = workbook.SheetNames; // 工作表名称集合
          _this.workbook = workbook;
          _this.getTable(sheetNames[0]);
        }
      };
      xhr.send();
    },
    getTable(sheetName) {
      var worksheet = this.workbook.Sheets[sheetName];
      this.excelData = XLSX.utils.sheet_to_json(worksheet);
    },
  }
}
</script>

<style scoped lang="scss">
:deep(.abow_dialog) {
  display: flex;
  justify-content: center;
  align-items: Center;
  overflow: hidden;

  .el-dialog {
    margin: 0 auto !important;
    height: 90%;
    overflow: hidden;

    .el-dialog__body {
      position: absolute;
      left: 0;
      top: 54px;
      bottom: 0;
      right: 0;
      padding: 0;
      z-index: 1;
      overflow: hidden;
      overflow-y: auto;
    }

  }
}
</style>

<style scoped>


/*:deep(.el-dialog__body) {*/
/*  height: 500px;*/
/*}*/
.dialog-body-content-base-style {

  max-width: 100%;
  max-height: 100%;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  cursor: move;
  margin: 0 auto;
}


#contentWrapper {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  /* transform: translateX(-50%);
  transform: translateY(-50%); */
  width: 300px;
  height: 300px;
}

.content-body {
  width: 100%;
  height: 500px;
  position: relative;
}

.customWidth {
  width: 80%;
  margin-top: 20vh;
}

:deep(.el-dialog) {
  width: 75%;
  height: 200%
}
</style>
