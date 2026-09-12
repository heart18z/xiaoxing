<template>
  <div>
    <!-- 查看和附按钮 -->
    <el-dialog id="attach-dialog" :title="justRead?'查看附件':'上传附件'"
               v-model="showAttchDialog" draggable
               :append-to-body="true" :modal='false' width="70%"

               :before-close="beforeDiaClose">
        <div class="attach-toolbar">
          <div class="attach-menu-left" v-if="!justRead">
            <div class="file-upload-tip1" v-if="fileMaxSize != null">
              <span>当前文件最大为{{fileMaxSize}}（MB）</span>
            </div>
            <div class="file-upload-tip"
                 v-if="currOss!==null&&currOss!==undefined">
              当前储存桶信息：<span>储存桶编号：【{{currOss.ossCode}}】，</span><span>{{currOss.desc}}</span>
            </div>
            <div class="file-upload-tip file-upload-tip--error" v-else>当前未设置储存地址，请先设置储存地址后上传</div>
          </div>
          <div class="attach-menu-right">
            <el-tooltip placement="top" v-if="!justRead">
              <template #content>
                <div class="span-line">
                  <span @click="adminsInfoDiaOpen" style="cursor:pointer;color: #1e9fff;text-underline: #1e9fff">
                    只有pdf类型的文件才能发起签署</span>
                </div>
              </template>
              <el-button title="文件签署" icon="el-icon-document-checked" size="small" circle
                         :disabled="isNotAllPdf"
                         @click="postPdfSignClick()"
              ></el-button>
            </el-tooltip>
            <el-button title="借阅权限" icon="el-icon-unlock" size="small" circle
                       :disabled="!isSingleSel || isSingleAndPhysicalSel===false || crudStatus==='edit'"
                       v-if="userInfo.role_name.includes('fileSecrecySet') ||userInfo.account === 'adminer'"
                       @click="setPermissionUser()"
            ></el-button>
            <el-button title="刷新" :disabled="!isNoneSel || crudStatus==='edit'"
                       icon="el-icon-refresh" size="small" circle
                       @click="refreshFileSourceCrud()"
            ></el-button>
            <el-button title="保存" icon="el-icon-check" size="small" circle @click="rowSave()"
                       :disabled="!isSingleAndEdit"
                       v-if="!justRead"></el-button>
            <el-button title="更换储存地址" class="inline-block attach-button" v-if="!justRead" size="small" icon="el-icon-link"
                       @click="changeOss" circle
                       :disabled="!isNoneSel || crudStatus==='edit'"></el-button>
            <el-tooltip placement="top" v-if="!justRead">
              <template #content>
                <div class="span-line">
                  <a @click="adminsInfoDiaOpen" style="cursor:pointer;color: #1e9fff;text-underline: #1e9fff">
                    请在文件管理中替换文件，请与系统管理员联系。点击查看管理员联系详细信息</a>
                </div>
              </template>
              <el-button size="small" icon="el-icon-sort" circle title="替换文件"
                         :disabled="!isSingleSel || isSingleAndPhysicalSel===true || crudStatus==='edit'"
              ></el-button>
            </el-tooltip>
            <el-dropdown @command="handleCommand" v-if="!justRead" :disabled="!isNoneSel || crudStatus==='edit'">
              <el-button size="small" :disabled="!isNoneSel || crudStatus==='edit'"
                         icon="el-icon-plus" circle title="新增文件">
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-upload class="inline-block"
                             action
                             :http-request="uploadFileHttp"
                             :show-file-list="false" multiple>
                    <el-dropdown-item icon="el-icon-folder">
                      <span>本地</span>
                    </el-dropdown-item>
                  </el-upload>
                  <el-dropdown-item icon="el-icon-box" command="uploadByExistsFile"><span>文件库</span></el-dropdown-item>
                  <el-dropdown-item icon="el-icon-video-camera" command="uploadLinkFile"><span>互联网链接</span></el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button :disabled="isNoneSel" circle v-if="!justRead" title="删除" size="small" icon="el-icon-delete"
                       @click.stop="deleteFile()">
            </el-button>
          </div>
        </div>
        <avue-crud ref="fileCrud" @row-dblclick="editList" :data="attachList" :option="attachOption"
                   @row-save="rowSave"
                   @sort-change="sortChange"
                   :cell-class-name="attachRowClass"
                   :table-loading="showAttachLoading"
                   @selection-change="fileSelectionChange">

          <template #index="{row,index}">
            <div>{{index+1}}</div>
          </template>
          <template #diyFileName="{ row }">
            <el-tooltip  trigger="hover" :content="getSecrecyTips(row)" placement="top" >
            <span @click="showAttachPermissionLog(row)">
              <span><i v-if="row.isSecrecy === '1' &&row.permission !==true " class="el-icon-lock" style="color: red; font-size: 1rem;margin-right: 0.4rem;"></i></span>
              <span><i v-if="row.isSecrecy === '1' &&row.permission ===true " class="el-icon-unlock" style="color: green; font-size: 1rem;margin-right: 0.4rem;"></i></span>
            </span>
            </el-tooltip >
            <el-tooltip :disabled="row.isPhysicalSave!=='1'" :content="'文件全局唯一编号:'+row.fileNum" placement="top" effect="light">
              <!-- tooltip 触发器必须是单根元素；el-popover 为多根/Teleport，不能直接作为子节点 -->
              <span class="inline-block">
                <el-popover trigger="click" :disabled="row.permission !==true" placement="bottom-start"
                            popper-class="attach-file-op-popover">
                  <div class="file-op-btns">
                    <el-button size="small" v-if="row.attachExtension !== 'intenetUrl' " @click.stop="handleRowClick(row)">直接预览</el-button>
                    <el-button size="small" @click="browserPreview(row)">页签预览</el-button>
                    <el-button size="small" v-if="row.attachExtension !== 'intenetUrl'" @click="handleDownload(row)">文件下载</el-button>
                    <el-button size="small" v-clipboard:copy="row.link" v-clipboard:success="handleCopy">拷贝路径</el-button>
                  </div>
                  <template #reference>
                    <el-button type="primary" link @click="showTipsClickName(row)"
                               style="
                              display: inline-block;
                              width: 220px;
                              overflow: hidden;
                              white-space: nowrap;
                              text-overflow: ellipsis;
                              text-align: left;"
                    >{{row.diyFileName}}
                    </el-button>
                  </template>
                </el-popover>
              </span>
            </el-tooltip>
            <span :style="inclineStyle(row)" class="label">{{inclineWorld(row) }}</span>
          </template>

          <template #signStatus="{ row }">
            <el-tag @click="showSignFiles(row)">{{row.$signStatus}}</el-tag>
          </template>

          <template #isPhysicalSaveForm="{ row }">
            <avue-select v-model="row.isPhysicalSave" placeholder="请选择内容"
                         :dic="sfData"
                         :disabled="row.$isPhysicalSaveCellEdit!==true || !isLocalUpload(row)"
                         style="width: 100px"
                         :props="{
                            label: 'dictValue',
                            value: 'dictKey'
                            }"
                         @change="isPhysicalSaveChange({value:row.isPhysicalSave,row})"
                         size="small"></avue-select>

          </template>

          <template #isSecrecyForm="{ row }">
            <avue-select v-model="row.isSecrecy" placeholder="请选择内容"
                         :dic="sfData"
                         :disabled="row.$isSecrecyCellEdit!==true"
                         style="width: 100px"
                         @change="isSecrecyFormChange(row)"
                         :props="{
                            label: 'dictValue',
                            value: 'dictKey'
                            }"
                         size="small"></avue-select>
          </template>
          <template #attachExtensionForm="{ row }">
            <avue-select v-model="row.attachExtension" placeholder="请选择内容"
                         :dic="attachExtensionList"
                         :disabled="row.$attachExtensionCellEdit!==true &&
                          row.attachExtension ===  'intenetUrl' ||
                          (row.isPhysicalSave === '1')"
                         size="small"></avue-select>

          </template>

          <template #diyFileNameForm="{ row }">
            <el-input v-model="row.diyFileName" placeholder="请选择内容" size="small"></el-input>

          </template>

          <template #filePhysicalPositionForm="{ row }">
            <avue-select v-model="row.filePhysicalPosition" placeholder="请选择内容"
                         :dic="regionList"
                         :disabled="row.$filePhysicalPositionCellEdit!==true ||
                          (row.isPhysicalSave !== '1')"
                         size="small"></avue-select>

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
    <el-dialog title="输入外网链接" v-model="uploadLinkFileDia"
               draggable :append-to-body="true" :modal='false' width="40%">
      <avue-input style="width: 100%" all v-model="linkFile" :clearable="false"  placeholder="请输入外网链接" ></avue-input>
      <template #footer><span
            class="dialog-footer">
              <el-button icon="el-icon-check" size="small" circle  title="保存"  @click="saveLinkFile"></el-button>
      <el-button circle icon="el-icon-close" size="small" title="取消" @click="uploadLinkFileDia = false" ></el-button>
      </span></template>
    </el-dialog>
    <el-dialog title="设置借阅权限" v-model="permissionUserSelectDialog"
               draggable :append-to-body="true" :modal='false' :close-on-click-modal="false" width="40%">
      <avue-form ref="permissionUserForm" v-model="permissionUserForm" :option="permissionUserOption">
        <template #permissionUserAccountType="{ item }">
          <span>{{ item.key }}</span>
        </template>
      </avue-form>
      <span>&middot;红⾊框代表⽂件的创建⼈</span>
      <template #footer><span
            class="dialog-footer">
              <el-button icon="el-icon-check" size="small" circle  title="保存"  @click="savePermissionUser"></el-button>
      <el-button circle icon="el-icon-close" size="small" title="取消" @click="permissionUserSelectDialog = false" ></el-button>
      </span></template>
    </el-dialog>
    <el-dialog title="授权日志" v-model="attachPermissionLogDialog"
               draggable :append-to-body="true" :modal='false' :close-on-click-modal="false" width="40%">
      <div style="height: 400px;overflow-y: scroll">
        <el-timeline>
          <el-timeline-item v-for="(item,index) in attachPermissionLogList" :timestamp="item.createTime" :key="index" placement="top">
            <el-card>
              <h4>保密文件可查看用户为： {{item.accountName}}</h4>
              <p>{{item.userName}} 更新于 {{item.createTime}}</p>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>

      <template #footer><span
            class="dialog-footer">
      <el-button circle icon="el-icon-close" size="small" title="取消" @click="attachPermissionLogDialog = false" ></el-button>
      </span></template>
    </el-dialog>
    <Userinfo ref="userinfo"></Userinfo>
    <sign-attach ref="signAttach"></sign-attach>
    <pickAttach ref="pickAttach" @submitAttach="submitAttach"></pickAttach>
  </div>
</template>

<script>
import signAttach from "@/components/attach-dialog/sign-attach/main.vue"
import pickAttach from "@/components/attach-dialog/pick-attach/main.vue"
import Userinfo from "@/components/user-info/main.vue";
import func from "@/utils/func";
import {Base64} from "js-base64";
import filePreview from "@/components/file-previewer/main.vue";
import {
  getListBySource,
  removeFile,
  submit,
  setPermission,
  attachPermissionLogList,
  postPdfSign
} from "@/api/system/attachSource"
import {dict as getRegionList} from "@/api/system/propertyRegion"
import {getList as ossList, putFileAttach} from "@/api/resource/oss";
import {getValueByKey} from "@/api/system/param";
import axios from 'axios'
import {mapGetters} from "vuex";
import {dateFormat} from "@/utils/date";
import {introduceOssCode} from "@/const/system/common";
import {getDictionary} from "@/api/system/dictbiz"
import {add as linkFileSave} from "@/api/resource/attach"
import {userAccountKvList} from "@/api/system/user";
import {queryCronExpressionByInvokeTarget} from "@/api/quartz/job";
import {downloadFileBlob} from "@/utils/util";
const DEFAULT_SAFEKEEP_STATUS = ""

const CancelToken = axios.CancelToken
export default {
  name: 'AttachDialog',
  components: {
    filePreview, Userinfo, pickAttach,signAttach
  },
  props: {
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
    introduceOss:{
      type: Boolean,
      default: false
    }
  },
  data() {
    return {

      orderProp: "",
      orderType: "",
      permissionUserForm:{},
      permissionUserOption:  {
        labelWidth: 0,
        submitBtn: false,
        emptyBtn: false,
        column: [
        {
          label: '',
          prop: 'permissionUserAccount',
          type: 'select',
          props: {
            value: 'value',
            label:'key'
          },
          dicMethod: "post",
            dicUrl: '/api/blade-system/user/user-account-kv-list',
          span: 24,
          filterable: true,
          multiple:true,
          dicFormatter: (res) => {
            const data = res.data
            for (let item of data) {
              item.label = item.key+"("+item.value+")"
            }
            return data
          },
          change:(val)=>{
            this.setSelectImg(val)
          },
        },]},
      attachPermissionLogDialog:false,
      attachPermissionLogList:[],
      permissionUserAccount: [],
      userSelect:[],
      setUserRow: {
        permissionUserAccount:[]
      },
      permissionUserSelectDialog:false,
      sfData:[],
      attachExtensionList: [],
      linkFile: "",
      uploadLinkFileDia: false,
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
        header: false,
        dialogDrag: true,
        refreshBtn: false,
        searchShowBtn: false,
        emptyBtn: false,
        emptyText: '暂无数据',
        searchMenuSpan: 4,
        height: 400,
        fit: false,
        column: [
          {
            label:'序号',
            prop:'index',
            fixed:false,
            width: 70,
            display: false,
            align: "center"
          },{
          label: '文件名称',
          prop: 'diyFileName',
          slot: true,
          width: 260,
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
            width: 105,
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
            label: '实物保存',
            prop: 'isPhysicalSave',
            width: 92,
            type: "select",
            slot: true,
            cell: true,
            clearable:false,
            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=biz_yes_no",
            change: this.isPhysicalSaveChange,
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },
          },
          {
            label: '是否保密',
            prop: 'isSecrecy',
            width: 92,
            type: "select",
            slot: true,
            cell: true,
            clearable:false,
            // disabled: true,
            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=biz_yes_no",
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },
          },
          {
            label: '保管状态',
            prop: 'safekeepStatus',
            sortable:true,
            width: 108,
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=file_safekeepStatus",
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },
          },

          {
            label: '保存地址',
            prop: 'filePhysicalPosition',
            type: "select",
            cell: true,
            slot: true,
            width: 105,
            dicData:[],
            props: {
              label: 'propertyName',
              value: 'propertyCode'
            },
          },
          {
            label: '签署状态',
            prop: 'signStatus',
            width:"120",
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
            width: 88,
          },
          {
            label: '上传人',
            span: 24,
            prop: 'createUser',
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-system/user/user-kv-list",
            width: 88,
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
            label: '上传时间',
            sortable:true,
            width: 108,
            prop: 'createTime'
          },
          // {
          //   label: '文件格式',
          //   prop: 'extension'
          // },
          {
            label: '文件大小',
            prop: 'attachSize',
            width: 108,
            sortable:true,
          },
          {
            label: '上传状态',
            prop: 'uploadProcess',
            slot: true,
            width: 100,
          }
        ]
      },
      data: [],
      progress: 0
    };
  },
  mounted() {

  },
  computed: {
    ...mapGetters(["permission","userInfo"]),
    isSingleSel(){
      return this.fileSelectionList.length===1
    },
    isNotAllPdf() {
      if (this.fileSelectionList.length ===0) {return true}
      for ( let item of this.fileSelectionList) {
        if (item.extension !== "pdf") {
          return true
        }
      }
      return false
    },
    //单选，并且选中的数据是实物保存数据
    isSingleAndPhysicalSel(){
      return this.fileSelectionList.length===1 &&this.fileSelectionList[0].isPhysicalSave==='1'
    },
    isSingleAndEdit(){
      return this.fileSelectionList.length===1 &&this.fileSelectionList[0].$cellEdit === true
    },
    isNoneSel(){
      return this.fileSelectionList.length===0
    },
    crudStatus() {
      for (let i = 0; i < this.attachList.length; i++) {
        const row =this.attachList[i]
        if (row.$cellEdit === true) {
          return "edit"
        }
      }
      return "view"
    }

  },
  watch: {
    isShowVideo(newValue) {
      let video = document.getElementById('video')
      if (!newValue) {
        video.pause()
        this.playUrl = ''
      }
    },
    'permissionUserForm.permissionUserAccount' (newValue,oldValue) {
      const roles =func.split(this.userInfo.role_name)
      if (oldValue &&
        !roles.includes("admin")&&
        !roles.includes("administrator")) {
        let minus = oldValue.filter(x => !newValue.includes(x));
        if (minus.includes(this.setUserRow.createUserAccount)) {
          this.permissionUserForm.permissionUserAccount = JSON.parse(JSON.stringify(oldValue))
          this.$message.warning("非管理员用户不能取消创建者作为权限用户")
        }
      }

    }
  },
  methods: {
    showSignFiles(row) {
      if (row.signStatus === 'signStatus_11' || row.signStatus === 'signStatus_12') {
        this.$refs.signAttach.show(row.attachSourceId)
      }

    },
    inclineStyle(row) {
      if (row.uploadType === "1") {
        return "background-color: #67C23A;"
      }else if (row.uploadType === "2"){
        return "background-color: #409EFF;"
      }else if (row.uploadType === "3"){
        return "background-color: #909399;"
      }
    },
    inclineWorld(row) {
      if (row.uploadType === "1") {
        return "本地"
      }else if (row.uploadType === "2"){
        return "引用"
      }else if (row.uploadType === "3"){
        return "链接"
      }
    },
    attachRowClass({ column }){
      if (column.columnKey === 'diyFileName') {
        return 'table-span-overflow'
      }
    },
    sortChange(sort) {
      this.orderProp = sort.prop
      this.orderType = sort.order
    },
    //这里利用修改dom元素去加图标
    setSelectImg(val){
      let selected= this.$refs.permissionUserForm.getPropRef('permissionUserAccount').$refs.temp.$el.children[0].children[0].children;
        this.$nextTick(()=>{
          if (selected!==null&&selected!==undefined&&
              val.value!=null &&val.value.length>0
              &&Array.from(selected).length === val.value.length
            ) {
              for (let i = 0; i < val.value.length; i++) {
                if (val.value[i] === this.setUserRow.createUserAccount) {
                  Array.from(selected)[i].setAttribute("style",`border: 2px solid red;`);
                }
              }
            }
        })

    },
    test(data){
      console.log(data)
    },
    getSecrecyTips(){
      if (this.userInfo.role_name.includes('fileSecrecySet') ||this.userInfo.account === 'admin') {
        return "该文件为保密文件，单击查看权限用户修改日志"
      } else {
        return "该文件为保密文件,您无权查看或者编辑！"
      }
    },
    showAttachPermissionLog(row) {
      if (this.userInfo.role_name.includes('fileSecrecySet') ||this.userInfo.account === 'admin') {
        this.attachPermissionLogDialog= true
        this.attachPermissionLogList = []
        attachPermissionLogList(row.attachSourceId).then(res=>{
          this.attachPermissionLogList =res.data.data
        })
      }

    },
    isLocalUpload(row) {
      return !(row.uploadType === '2' || row.uploadType === '3');
    },
    isSecrecyFormChange(row) {
      if (func.isEmpty(row.permissionUserAccount)) {
        row.permissionUserAccount = []
      }
    },
    showTipsClickName(row){
      if (row.permission !==true) {
        this.$message.warning("保密文件，您无权查看!")
      }
    },
    savePermissionUser() {
      if (func.isEmpty(this.setUserRow)) {
        this.$message.warning("数据错误请刷新后重新选择!")
        return
      }
      const accountArr = this.permissionUserForm.permissionUserAccount
      let account = ""
      if (func.notEmpty(accountArr)) {
        account = func.join(accountArr)
      }
      setPermission({id:this.setUserRow.attachSourceId,
        permissionUserAccount:account}).then(()=>{
        this.$message.success("操作成功")
        this.permissionUserSelectDialog = false
        this.attachList = []
        this.refreshFileSourceCrud()
      }).catch(()=>{
        this.$message.warning("操作失败")
        this.attachList = []
        this.refreshFileSourceCrud()
      })
    },
    postPdfSignClick() {
      for (let item of this.fileSelectionList) {
        if ("signStatus_10"!==item.signStatus) {
          this.$message.warning("已经发起签署的文件不要重复发起！")
          return
        }
      }

      postPdfSign({attachSourceList:this.fileSelectionList}).then(()=>{
        this.$message.success("操作成功")
        this.attachList = []
        this.refreshFileSourceCrud()
      }).catch(()=>{
        this.attachList = []
        this.refreshFileSourceCrud()
      })

    },
    setPermissionUser(){
      this.setUserRow = {permissionUserAccount:[]}
      this.permissionUserForm.permissionUserAccount =[]
      if (this.fileSelectionList.length===0) {
        this.$message.warning("请选择一条数据之后设置借阅权限！")
        return;
      }
      if (this.fileSelectionList.length>1) {
        this.$message.warning("只能选择一条数据之后设置借阅权限！")
        return;
      }
      const data =this.fileSelectionList[0]
      if (func.isEmpty(data.attachSourceId)) {
        this.$message.warning("请保存文件之后之后设置借阅权限！")
        return;
      }

      // if (data.permission!==true) {
      //   this.$message.warning("保密文件，您无权操作!")
      //   return;
      // }

      if (data.$cellEdit === true) {
        this.$message.warning("当前数据正在编辑，请保存之后设置用户权限!")
        return;
      }

      if (data.isPhysicalSave !== '1') {
        this.$message.warning("设置实物保存之后才能设置借阅权限！")
        return;
      }
      this.setUserRow = data
      if ( func.isEmpty(this.setUserRow.permissionUserAccount)) {
        this.permissionUserAccount = []
      } else if (this.setUserRow.permissionUserAccount.constructor !== Array){
        this.permissionUserForm.permissionUserAccount = func.split(this.setUserRow.permissionUserAccount)
      }
      this.permissionUserSelectDialog = true
    },
    // physicalSaveClick(row) {
    //
    // },
    initSfDic(){
      getDictionary({code:"biz_yes_no"}).then(res=>{
        this.sfData = res.data.data
      })
    },
    initAccountKvDic(){
      userAccountKvList().then(res=>{
        this.userSelect = res.data.data
      })
    },

    isPhysicalSaveChange({row}) {
      this.$nextTick(()=>{
        if (row.isPhysicalSave==='1') {
          if (row.copyIsPhysicalSave !==row.isPhysicalSave) {
            this.$message.warning("实物保存选择为“是”，保存后不能更改请谨慎选择")
          }
          row["$isSecrecyCellEdit"] = true
          row["$filePhysicalPositionCellEdit"] = true
          if (func.isEmpty(row.isSecrecy)) {
            row.isSecrecy = '0'

          }
          //是否保密只能第一次保存事物为是保存时编辑
          if (row.copyIsPhysicalSave==='1') {
            row["$isSecrecyCellEdit"] = false
          }

        } else {
          row.filePhysicalPosition = ""
          row["$isSecrecyCellEdit"] = false
          row["$filePhysicalPositionCellEdit"] = false
          row.isSecrecy = '0'
        }
      })


    },
    initAttachExtension(){
      getDictionary({'code': 'faid'}).then(res=>{
        const data = res.data.data
        for (let d of data) {
          d.value = d.dictKey
          d.label = d.dictValue
          // if (d.value === "intenetUrl") {
          //   d.disabled = true
          // }
        }
        this.attachExtensionList=data
      })
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
      const  data = this.regionList.find(item=>item.propertyCode == row.filePhysicalPosition)
      if (func.notEmpty(data) && func.notEmpty(data.propertyName)) {
        return data.propertyName
      }
      if (row.isPhysicalSave === '1') {
        return "实物暂无保存"
      } else {
        return "实物⽆需保存"
      }

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
    cancelUpload(fileList){
      for (let file of fileList) {
        if (file.uploadFileCancel) {
          try {
            file.uploadFileCancel()
          } catch (e) {
            console.log(e)
          }

        }
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
        },
        cancelToken: new CancelToken(function executor(c) {
          // executor 函数接收一个 cancel 函数作为参数
          rowData.uploadFileCancel = c;
        }),
      }).then(res => {
        // 上传成功后续处理
        const data = res.data.data
        rowData.id = data.attachId
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
    saveLinkFile() {
      if(this.linkFile == ''){
        this.$message({
          type: "warning",
          message: "请输入互联网视频资源地址"
        });
        return
      }

      const rowData = {
        link: this.linkFile,
        domainUrl: this.linkFile,
        name: '未命名',
        originalName: '未命名',
        diyFileName: '未命名',
        attachSize: 0,
        $cellEdit: true,
        attachExtension: "intenetUrl",
        sourceId: this.sourceId,
        uploadProcess: 100,
        sourceType: 2,
        filePhysicalPosition:"",
        isPhysicalSave: "0",
        // isOpen: this.row.isOpen,
        $filePhysicalPositionCellEdit: false,
        uploadType: "3",
        $isPhysicalSaveCellEdit: true,
        safekeepStatus: DEFAULT_SAFEKEEP_STATUS,
        $attachExtensionCellEdit: false,
        sort: 1000,
      }
      linkFileSave(rowData).then(res => {
        const data = res.data.data
        rowData.id = data.id
        // rowData.createTime = data.createTime
        // rowData.createUser = data.

        this.attachList.push(rowData)
        this.uploadLinkFileDia = false
      })
    },
    submitAttach(files) {

      if (func.notEmpty(files)) {
        for (let fileData of files) {
          const rowData = {
            originalName: fileData.originalName,
            diyFileName: fileData.originalName,
            attachSize: fileData.attachSize,
            extension: fileData.extension,
            $cellEdit: true,
            attachExtension: "",
            sourceId: this.sourceId,
            createTime: fileData.createTime,
            createUser: fileData.createUser,
            id: fileData.id,
            isPhysicalSave: "0",
            sourceType: 2,
            filePhysicalPosition: fileData.filePhysicalPosition,
            $filePhysicalPositionCellEdit: false,
            uploadType: "2",
            $isPhysicalSaveCellEdit: true,
            safekeepStatus: DEFAULT_SAFEKEEP_STATUS,
            uploadProcess: 100,
            sort: 1000,
          }
          this.attachList.push(rowData)
        }
      }


    },
    handleCommand(command) {
      if (command == 'uploadByExistsFile') {
        this.uploadByExistsFile()
      } else if (command === 'uploadLinkFile') {
        this.uploadLinkFileDia = true
        this.linkFile = ""
      }
    },
    adminsInfoDiaOpen() {
      this.$refs.userinfo.show()
    },
    notEmpty: func.notEmpty,
    initOss() {
      this.currOss = null
      this.currOssId = ''
      ossList().then(res => {
        const data = res.data.data.records
        const selectData = []
        for (let d of data) {
          // if (this.introduceOss !== true && d.ossCode === introduceOssCode) {
          //   continue;
          // }
          d.value = d.id
          d.label = d.ossCode
          d.desc = "资源地址：【" + d.endpoint + "】,空间名：【" + d.bucketName + "】"
          if (this.introduceOss === true) {
            if( d.ossCode === introduceOssCode){
              this.currOss = d
              this.currOssId = d.id
              this.defaultOssId = d.id
            }
          }else if (d.status == 2 ) {
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
    initRegion(){
      getRegionList().then(res=>{
        // console.log("res.data.data",res.data.data)
        const data = res.data.data
        for (let d of data) {
          d.value = d.propertyCode
          d.label = d.propertyName
        }
        this.regionList= data
        const column = this.findObject(this.attachOption.column, "filePhysicalPosition");
        column.dicData = data
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
        if (row.permission !==true) {
          this.$message.warning("保密文件，您无权编辑!")
          return;
        }
        this.closeAllEdit()
        row.$cellEdit = true
        row.$isPhysicalSaveCellEdit = row.isPhysicalSave === '0';
        row.$isSecrecyCellEdit = row.$isPhysicalSaveCellEdit
      }
    },
    closeAllEdit(){
      let isClosed = false
      for (let attach of this.attachList) {
        if (func.notEmpty(attach.attachSourceId)) {
          if (attach.$cellEdit ===true) {
            isClosed = true
          }
          attach.$cellEdit = false
        }
      }
      if (isClosed) {
        this.getFileData()
      }
    },
    // 保存列表
    async rowSave() {
      if (this.fileSelectionList.length <= 0) {
        this.$message.warning("请选择文件后保存");
        return;
      }
      if (this.fileSelectionList.length >1) {
        this.$message.warning("只能选择一条数据后保存");
        return;
      }
      for (let item of this.fileSelectionList) {
        if (func.isEmpty(item.diyFileName)) {
          this.$message.warning("文件名称不能为空！");
          return;
        }

        if (func.isEmpty(item.attachExtension)) {
          this.$message.warning("附件类型不能为空！");
          return;
        }

        if (func.isEmpty(item.id)) {
          this.$message.warning("请等待文件上传完成后保存！");
          return;
        }
        if (item.isPhysicalSave === '1' && func.isEmpty(item.filePhysicalPosition)) {
          this.$message.warning("请完善保存地址之后上传！");
          return;
        }
        if (item.isPhysicalSave === '1' && item.copyIsPhysicalSave !==item.isPhysicalSave ) {

          const res = await queryCronExpressionByInvokeTarget("SyncDataTask.postAttach")
          const time = res.data.data
          const msg = res.data.msg||"推送实物保存信息1"
          let tips = "";
          if (func.notEmpty(time)) {
            tips='实物保存的文件一旦保存，则这条数据不允许编辑修改，' +
              '只能文件设定为销毁后才能被删除，需要谨慎操作,确定后将于【 '+time+'】推送至《实物档案收纳系统》  ';
          } else {
            tips='实物保存的文件一旦保存，则这条数据不允许编辑修改，' +
              '只能文件设定为销毁后才能被删除，需要谨慎操作,确定后文件不会发生实质推送，' +
              '如果需要请先在定时任务中将【'+msg+'】开启 ';
          }

          const confirm =  await this.$confirm(tips, '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          })
          if (confirm !=="confirm") {
            return
          }

        }
      }

      this.doSaveData()
    },
    doSaveData() {
      for (let item of this.fileSelectionList) {
        if (func.notEmpty(item.permissionUserAccount)) {
          item.permissionUserAccount = func.join(item.permissionUserAccount)
        }
      }
      const data = {
        attachSourceList: []
      }
      data.attachSourceList = this.fileSelectionList
      submit(data).then(r => {
        if (r.data.code === 200) {
          this.$message.success("保存成功");
          // console.log(this.fileSelectionList)
          this.attachList=this.attachList.filter(i=>!this.fileSelectionList.includes(i))
          this.refreshFileSourceCrud()

        }
      })
    },
    uploadByExistsFile() {
      // this.pickExistFileDialog = true
      this.$refs.pickAttach.show()
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
      // console.log("userInfo",this.userInfo)
      const rowData = {
        uid: fileData.uid,
        originalName: fileName,
        diyFileName: fileName,
        attachSize: req.file.size,
        extension: req.file.type,
        sort: 1000,
        $cellEdit: true,
        attachExtension: "",
        sourceId: this.sourceId,
        id: "",
        createUser: this.userInfo.user_id,
        createTime: dateFormat(new Date()),
        filePhysicalPosition: "",
        uploadProcess: 0,
        sourceType: 2,
        isPhysicalSave: "0",
        temFile: true,
        $filePhysicalPositionCellEdit: false,
        $isPhysicalSaveCellEdit: true,
        uploadType: "1",
        safekeepStatus: DEFAULT_SAFEKEEP_STATUS,
      }
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
        this.cancelUpload(this.fileSelectionList)
        const data = JSON.parse(JSON.stringify(this.fileSelectionList))
        for (let i = 0; i < data.length; i++) {
          if (data[i]&&data[i].permissionUserAccount) {
            //console.log("data[i].permissionUserAccount",data[i].permissionUserAccount)
            data[i].permissionUserAccount = func.join(data[i].permissionUserAccount)
          }
        }
        return removeFile({attachSourceList: data});
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
        // this.cancelUpload()
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

        // if (!isInnerIPFn(row.link)) {
        //   // eslint-disable-next-line
        //   window.open(this.website.filePreviewUrl+ encodeURIComponent(Base64.encode(row.link)));
        //   //window.open('https://view.xdocin.com/view?src=' + row.link)
        // } else {
        //   this.$message.warning("文件地址内网不可预览，请下载后预览！")
        // }
      }
    },
    // 下载
    handleDownload(row) {
      downloadFileBlob(row.link,row.diyFileName)

    },
    getFileData() {
      this.showAttachLoading = true
      const params = {}
      params.sourceId = this.sourceId

      params.orderProp = this.orderProp
      params.orderType = this.orderType
      const uploadingData = this.attachList.filter((item)=>{return func.isEmpty(item.attachSourceId)})
      getListBySource(params).then(res => {
        for (let item of res.data.data) {
          item.uploadProcess = 100
          item.copyIsPhysicalSave= item.isPhysicalSave
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
    initCloseEditEvent(){
      let elm = this.$el.querySelector('#attach-dialog')
      if (elm) {
        let dialogElm = elm.getElementsByClassName("el-dialog")
        if (dialogElm &&dialogElm.length>0) {
          let that =this
          dialogElm[0].addEventListener('click',function (event) {
            //console.log("event.target.tagName ",event.target.tagName,event.target._prevClass ,event.target,event)
            if (event.target.tagName === 'INPUT') {
              event.stopPropagation();
            } else if (["el-icon-arrow-up","el-icon-unlock","el-icon-check",
              "el-input-number__increase",
              "el-input__icon el-icon-circle-close el-input__clear",
              "el-input-number__decrease",
              "el-icon-arrow-down"].includes( event.target._prevClass )){
              event.stopPropagation();
            } else if (event.target.tagName==='BUTTON') {
              event.stopPropagation();
            }else {
              that.closeAllEdit()
            }

          },false)
        }
      }
    },
    // 展示附件列表
    showAttachDialog() {
      this.attachList = []
      this.initOss()
      this.initMaxFileSize()
      this.initRegion()
      this.showAttchDialog = true
      this.getFileData()
      this.initAttachExtension()
      this.initAccountKvDic()
      this.initSfDic()
      this.initCloseEditEvent()
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
.table-span-overflow{overflow: hidden}

/* 文件名点击弹出的操作按钮，横向排列（popover 挂载在 body 上，样式不能加 scoped） */
.attach-file-op-popover.el-popover.el-popper {
  width: auto !important;
  min-width: unset;
}

.attach-file-op-popover .file-op-btns {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
}

.attach-file-op-popover .file-op-btns .el-button + .el-button {
  margin-left: 0;
}

#attach-dialog .attach-toolbar {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  min-height: 44px;
  margin-bottom: 8px;
}

#attach-dialog .attach-menu-left {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 2px;
}

#attach-dialog .attach-menu-right {
  flex-shrink: 0;
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 8px;
}

#attach-dialog .attach-menu-right > .el-button,
#attach-dialog .attach-menu-right > .el-tooltip,
#attach-dialog .attach-menu-right > .el-dropdown {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0 !important;
}

#attach-dialog .attach-menu-right .el-button {
  width: 28px;
  height: 28px;
  padding: 0 !important;
  margin: 0 !important;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

#attach-dialog .attach-menu-right .el-button [class*="el-icon"] {
  margin: 0;
  font-size: 14px;
}

#attach-dialog .file-upload-tip,
#attach-dialog .file-upload-tip1 {
  line-height: 16px;
  font-size: 12px;
  color: #8f8686;
}

#attach-dialog .file-upload-tip--error {
  color: #f56c6c;
}

#attach-dialog .el-table th.el-table__cell > .cell {
  white-space: nowrap;
  word-break: keep-all;
}

#attach-dialog .el-table .el-table__header-wrapper {
  overflow: visible;
}
</style>

<style scoped>

.label {

  height: 15px;
  line-height: 15px;
  text-align: center;
  float: right;
  /* right: 10px; */
  position: absolute;
  top: 4px;
  right: -10px;
  -webkit-transform: rotate(45deg);
  transform: rotate(45deg);

  color: white;
  padding: 5px;
  font-size: 12px;
  /* content: '21333'; */
  width: 40px;
}

.tags-incline{
  text-align: center;
  position: absolute;
  background-color: grey;
  color: #fff;
  width: 80%;
  height: 18px;
  line-height: 18px;
  top: 10px;
  left:40%;
  transform: rotate(45deg);
  border:1px solid #000;
}

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
