<template>
  <div>
    <el-dialog title="已签署文件"
               v-model="signAttachDialogShow" draggable
               :append-to-body="true" :modal='false' width="50%">
      <avue-crud :option="option"
                 :table-loading="loading"
                 :data="data"
                 v-model="form"
                 ref="crud"
                 @search-change="searchChange"
                 @search-reset="searchReset"
                 @selection-change="selectionChange"
                 @current-change="currentChange"
                 @refresh-change="refreshChange">
        <!-- 左边按钮 -->
        <template #menu-right="{ row,index }">
<!--          <el-button class="inline-block" size="small" icon="el-icon-check" @click="submitFile" circle-->
<!--                     title="确定文件"></el-button>-->
        </template>
        <template #attachSize="{ row }">
          {{`${row.attachSize} KB`}}
        </template>

        <template #diyFileName="{ row }">
          <!--        <span type="info" >{{row.link }}</span>-->
          <el-popover trigger="click" placement="bottom-start">
            <div>
              <el-button size="small" v-if="row.attachExtension !== 'intenetUrl' " @click.stop="handleRowClick(row)">直接预览</el-button>
              <el-button size="small" @click="browserPreview(row)">页签预览</el-button>
              <el-button size="small" v-if="row.attachExtension !== 'intenetUrl' " @click="handleDownload(row)">文件下载</el-button>
              <el-button size="small" v-clipboard:copy="row.link" v-clipboard:success="handleCopy">拷贝路径</el-button>
            </div>
            <template #reference><el-button type="primary" link>{{ row.diyFileName }}<i v-if="row.isDeleted ===1" style="color: red" class="el-icon-document-delete"></i></el-button></template>
          </el-popover>
        </template>

        <template slot="link" slot-scope="scope">
          <span type="info" >{{scope.row.link }}</span>
        </template>
      </avue-crud>
      <el-dialog title="视频播放" class="video-dialog" draggable custom-class="my-video-dialog" v-model="isShowVideo"
                 :modal='false' width="80%"  :append-to-body="true"  >
        <div class="play-video-box">
          <video id="video" :src="playUrl" controls="controls"></video>
        </div>
      </el-dialog>
      <file-preview ref="previewArea"/>
    </el-dialog>
  </div>
</template>

<script>
import {completeRemove, remove, restore} from "@/api/base/attachRecovery";
import filePreview from "@/components/file-previewer/main.vue";
import {Base64} from "js-base64";
import {downloadFileBlob} from "@/utils/util";
import {getListByOriId} from "@/api/system/attachSource";

export default {
  name: 'SignAttach',
  components:{
    filePreview
  },
  data() {
    return {
      oriId: "",
      sourceIds:[],
      type: "id",
      signAttachDialogShow: false,
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
        menu:false,
        selectionFixed: false,
        selection: false,
        border: true,
        addBtn: false,
        viewBtn: true,
        searchBtn: false,
        tree: true,
        columnBtn: false,
        tip: false,
        head: false,
        dialogDrag: true,
        refreshBtn: false,
        searchShowBtn: false,
        emptyBtn: false,
        searchMenuSpan: 4,
        height: 400,
        column: [
         {
            label: '文件名称',
            prop: 'diyFileName',
            slot: true,
            width: 270,
            sortable:true,
            cell: true,
            rules: [{
              required: true,
              message: "请输入文件名称",
              trigger: "blur"
            }],
          },
          {
            label: '文件类型',
            prop: 'attachExtension',
            cell: true,
            width:"95",
            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=faid",
            type: 'select',
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },
            sortable:true,
            rules: [{
              required: true,
              message: "请选择文件类型",
              trigger: "change"
            }],
          },
          {
            label: '签署状态',
            prop: 'signStatus',
            width:"125",
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=signStatus",
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },
          },
          {
            label: '排序',
            type: "number",
            prop: 'sort',
            cell: true,
            sortable:true,
            width: 105,
          },

          {
            label: '上传时间',
            sortable:true,
            prop: 'createTime'
          },

          {
            label: '文件大小',
            prop: 'attachSize',
            sortable:true,
          }
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
    ids() {
      let ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(",");
    }
  },
  methods: {

    show(id){
      this.oriId = id
      this.signAttachDialogShow = true
      this.refreshChange()
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

      }
    },
    // 下载
    handleDownload(row) {
      downloadFileBlob(row.link,row.diyFileName)
    },
    // 点击复制按钮
    handleCopy(){
      this.$message.success("复制成功");
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
      this.$confirm("确定将选择数删除吗?", {
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
      try {
        this.selectionList.forEach((item) => {
          if (item.isDeleted == 0) {
            this.$message.warning("当前选中的数据包含未删除文件，请先删除后重试！");
            throw ('stop')
          }
        });
      }catch (e) {
        return;
      }
      this.$confirm("确定将选择数据彻底删除吗，删除后文件将不可恢复?", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
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
    onLoad() {
      this.loading = true;

      getListByOriId({originalId: this.oriId}).then(res => {
        this.data = res.data.data;
          this.loading = false;
          this.selectionClear();
        });
      }


  }
};
</script>


