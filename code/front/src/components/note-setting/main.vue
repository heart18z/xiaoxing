<template>
  <div>
    <el-dialog :title="justRead?'查看配置':'注释配置'"
               v-model="showNoteSettingDialog" draggable
               :append-to-body="true" :modal='false' width="70%"
               :before-close="beforeDiaClose">
      <avue-crud :option="option"
                 v-model:search="query"
                 :table-loading="loading"
                 :class="forbidClassName"
                 :data="data"
                 v-model:page="page"
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
          <el-button class="button-size" round title="新增" v-if="!justRead" size="small" icon="el-icon-plus" @click.stop="addList()">
          </el-button>
          <el-button class="button-size" round title="删除" v-if="!justRead" size="small" icon="el-icon-delete"
                     @click.stop="handleDelete()">
          </el-button>
          <el-button class="button-size" round title="复制" v-if="!justRead" size="small" icon="el-icon-document-copy" @click.stop="copyList()">
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

    </el-dialog>
  </div>


</template>

<script>
import {getList, getDetail, add, update, remove, selectDomNote} from "@/api/system/noteSetting";
import {getMenuByPath} from "@/api/components-common/api";
import func from "@/utils/func";
import { nextTick, createVNode, render } from 'vue';
import popover from '@/components/note-setting/popover.vue';
// import {mapGetters, mapState} from "vuex";

// class name 为blade-forbid-note 的禁止插入注释
const forbidClassName= "blade-forbid-note"
export default {
  name: 'NoteSetting',
  props:{
    options:{
      type: Array,
      default: ()=>{return[]}
    },
    justRead: {
      type: Boolean,
      default: false
    },
    isParentMounted:{
      type: Boolean,
      default: false
    },
    parentCrud:{
      type: Object,
      default: ()=>{}
    },
  },
  data() {
    return {
      forbidClassName,
      cssMovedTag:"css-moved-tag",
      testShow: true,
      showNoteSettingDialog: false,
      form: {},
      query: {},
      search: {},
      loading: true,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0
      },
      selectionList: [],
      menuId: "",
      option: {
        dialogCustomClass:forbidClassName,
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
            label: "编号",
            prop: "id",
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
          //   label: "菜单id",
          //   prop: "menuId",
          //   type: "input",
          // },
          {
            label: "字段名称",
            prop: "propName",
            rules: [{
              required: true,
              message: "请输入字段名称",
              trigger: "blur"
            }],
            // display: false
          },

          {
            label: "字段页面",
            prop: "propLocation",
            type: "select",
            multiple:true,
            dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=note_location",
            props: {
              label: 'dictValue',
              value: 'dictKey'
            },
            rules: [{
              required: true,
              message: "请选择字段页面",
              trigger: "change"
            }],

          },
          {
            label: "注释",
            prop: "noteId",
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-note/note/select",
            props: {
              label: 'name',
              value: 'id',
              desc: 'typeName'
            },
            rules: [{
              required: true,
              message: "请选择注释",
              trigger: "change"
            }],
            change: ()=>{}
          },
          {
            label: "注释内容",
            prop: "content",
            type: "textarea",
            rules: [{
              required: true,
              message: "请输入注释内容",
              trigger: "blur"
            }],
            width: 300,
            span: 24,
            minRows: 6,
          },
          {
            label: "备注",
            prop: "remark",
            type: "input",
          },
          {
            label: "配置时间",
            prop: "createTime",
            type: "input",
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
          },
          {
            label: "配置人",
            prop: "createUser",
            type: "select",
            dicMethod: "post",
            dicUrl: "/api/blade-system/user/user-kv-list",
            props: {
              label: 'key',
              value: 'value',
            },
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
          },
        ]
      },
      data: [],
      menuIdNoteObj: {},
      timer: null,
      noteTimer: null,
    };
  },
  mounted() {
    this.refreshSystemNote()
    this.handleRouteChange()
    this.initNoteObserver()
  },
  computed: {
    ids() {
      let ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(",");
    },
    noteStarter() {
      const { menuId, menuIdNoteObj } = this
      return {
        menuId,
        menuIdNoteObj
      }
    }

  },
  watch: {
    '$route.fullPath'() {
      this.handleRouteChange();
    },
    isParentMounted(newValue)  {
      if (newValue === true) {
        this.scheduleInitNote();
      }
    },
    noteStarter: {
      handler(newVal) {
        if (func.notEmpty(newVal.menuId) && func.notEmpty(newVal.menuIdNoteObj)) {
          this.scheduleInitNote();
        }
      },
      deep: true
    },
  },
  methods: {
    initNoteObserver() {
      let box = document.querySelector('body'),
          config = { attributes: false,   // 开启监听属性
            childList: true,  };
      let observer = new MutationObserver(mutations => {
        //console.log("mutations===>",mutations);
        for ( let mut of mutations){
          if (func.notEmpty(mut.addedNodes) && mut.addedNodes.length>0) {
            if (mut.addedNodes.length>0) {
              for (let ol of mut.addedNodes) {
                if (ol.nodeType !== Node.ELEMENT_NODE) continue
                const elDialog = ol.querySelectorAll('div.el-dialog,div.el-drawer')
                const elCrud = ol.querySelector('.avue-crud')
                if (elDialog.length>0 || elCrud) {
                  this.scheduleInitNote()
                }
              }
            }

          }
        }
        // => 返回一个监听到的MutationRecord对象
        // MutationRecord对象是每修改一个就会在数组里面追加一个
      });
      observer.observe(box, config); // 监听的box元素和config配置项
    },

    initNote() {
      //console.log("menuIdNoteObj",this.menuIdNoteObj,this.menuIdNoteObj[this.menuId])
      if (func.notEmpty(this.menuIdNoteObj)&& func.notEmpty(this.menuIdNoteObj[this.menuId])) {
        nextTick(() => {
          const body =  document.querySelector("body")
          if (body) {
            // 删除原先的注释
            const oldNotedEl = body.querySelectorAll(".blade-noted");
            //console.log("oldNotedEl----->del--",oldNotedEl)
            if (oldNotedEl.length>0) {
              for (let ol of oldNotedEl) {
                ol.classList.remove("blade-noted");
                const oldNoteMounts = ol.querySelectorAll(".blade-note-mount");
                oldNoteMounts.forEach(el => {
                  render(null, el);
                  el.remove();
                });
              }
            }
            this.initNoteByType(this.menuIdNoteObj[this.menuId])
          }
        })
      }
    },
    initNoteByType(data) {
      this.initTableNote(data.table)
      this.initFormNote(data.form)
      this.initSearchNote(data.search)
    },
    //加载列表注释
    initTableNote(noteData) {
      this.baseInitNote(noteData,".avue-crud",'th.el-table__cell:not(.is-hidden) > div > span:not(.blade-noted)')
    },
    //加载表单注释
    initFormNote(noteData) {
      // li:not([style*="display:none"]):not([style*="display: none"])  div.el-drawer,div.el-dialog
      this.baseInitNote(noteData,"div.el-dialog__wrapper:not([style*='display: none;'])  >.el-dialog",'div.el-form-item',
      ".el-drawer__wrapper")

    },
    //加载搜索注释
    initSearchNote(noteData) {
      this.baseInitNote(noteData,".avue-crud__search",'div.el-form-item')
    },
    baseInitNote(noteData,baseQuerySelector,detailQuerySelector,backUpBaseQuerySelector) {
      if (func.isEmpty(noteData)||noteData.length===0) {
        return
      }
      nextTick(() => {
        // this.forbidClassName,*:not([display=none]
        const baseSel = baseQuerySelector+":not(."+this.forbidClassName+")"
        // console.log("baseSel",baseSel)
        let dlaParent = document.querySelector(baseSel)
        //console.log("baseSel",baseSel,dlaParent)
        // console.log("noteData",noteData)
        // // console.log("baseSel",baseSel,dlaParent)
        if (func.isEmpty(dlaParent)) {
          if (func.notEmpty(backUpBaseQuerySelector)) {
            dlaParent =document.querySelector(backUpBaseQuerySelector)
            // console.log("baseSelbackUpBaseQuerySelector",backUpBaseQuerySelector,dlaParent)
            if (func.isEmpty(dlaParent)){return;}
          }else {
            return
          }
        }

        const nodesParent = dlaParent.querySelectorAll(detailQuerySelector)
        //console.log("nodesParent",nodesParent)
        if (nodesParent.length > 0 && noteData.length > 0) {
          for (let i = 0; i < nodesParent.length; ++i) {
            let temDom = nodesParent[i]
            //el-form-item__content
            const content = temDom.querySelector(".el-form-item__content")
            const label = temDom.querySelector(".el-form-item__label")
            let isForm = false
            if (func.notEmpty(content) && func.notEmpty(label)) {
              isForm = true
            }
            // 判断当是form的时候 dom用label
            let dom = isForm?label:nodesParent[i]
            let innerText = dom.innerText || "";
            innerText = func.trimAll(innerText)
            for (let noteItem of noteData) {
              if (noteItem.propName + ":" === innerText || noteItem.propName === innerText){
                const mountPoint = document.createElement('span');
                mountPoint.className = 'blade-note-mount';
                dom.appendChild(mountPoint);
                const vnode = createVNode(popover, {
                  content: noteItem.tips,
                  icon: noteItem.icon,
                });
                vnode.appContext = this.$.appContext;
                render(vnode, mountPoint);
                dom.classList.add('blade-noted');
                if (isForm) {
                  this.moveCss(label,content)
                }
              }
            }
          }
        }
      })
    },
    moveCss(label,content) {
      const mL = content.style.marginLeft
      if (func.notEmpty(mL)&&func.notEmpty(label.offsetWidth) && label.classList.value.indexOf(this.cssMovedTag)===-1) {
        label.classList.add(this.cssMovedTag)
        content.style.marginLeft = (label.offsetWidth+20)+"px"
        label.style.width= (label.offsetWidth+20)+"px"
      }
    },
    handleRouteChange() {
      this.resolveMenuId().then(() => {
        this.scheduleInitNote();
      });
    },
    resolveMenuId() {
      const queryMenuId = this.$route.query.menuId;
      if (func.notEmpty(queryMenuId)) {
        this.menuId = queryMenuId;
        return Promise.resolve();
      }
      return getMenuByPath({ pagePath: this.$route.path }).then(res => {
        if (res.data.code === 200 && func.notEmpty(res.data.data?.id)) {
          this.menuId = res.data.data.id;
        }
      });
    },
    scheduleInitNote() {
      if (this.noteTimer) {
        clearTimeout(this.noteTimer);
      }
      nextTick(() => {
        this.noteTimer = setTimeout(() => {
          this.initNote();
        }, 100);
      });
    },
    sortBy(attr,rev){
    if( rev==undefined ){ rev=1 }else{ (rev)?1:-1; }
    return function (a,b){
      a=a[attr];
      b=b[attr];
      if(a<b){ return rev*-1}
      if(a>b){ return rev* 1 }
      return 0;
      }
    },
    async initMenuId() {
      await this.resolveMenuId();
    },
    beforeDiaClose(done) {
      done()
    },
    addList() {
      this.$refs.crud.rowAdd()
      this.form = {
        menuId: this.menuId
      }
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
    rowSave(row, done, loading) {
      row.propLocation = func.join(row.propLocation);
      add(row).then(() => {
        this.onLoad(this.page);
        this.$message({
          type: "success",
          message: "操作成功!"
        });
        this.refreshSystemNote()
        done();
      }, error => {
        loading();
        window.console.log(error);
      });
    },
    rowUpdate(row, index, done, loading) {
      row.propLocation = func.join(row.propLocation);
      update(row).then(() => {
        this.onLoad(this.page);
        this.$message({
          type: "success",
          message: "操作成功!"
        });
        this.refreshSystemNote()
        done();
      }, error => {
        loading();
        console.log(error);

      });
    },
    // 刷新系统参数
    refreshSystemNote() {
      this.menuIdNoteObj = {}
      selectDomNote().then(res => {
        const data = res.data.data;
        data.forEach(item=>{
          let text = item.propName || "";
          text = func.trimAll(text)
          item.propName = text
        })
        // 第一部分组
        const dataByMenuId = data.reduce((acc, person) => {
          const menuId = person.menuId;
          if (!acc[menuId]) {
            acc[menuId] = [];
          }
          acc[menuId].push(person);
          return acc;
        }, {});
        // 组装
        const finalType = {}
        for ( let key in dataByMenuId) {
          const types = dataByMenuId[key] || []
          const dataByMenuIdByType ={
            table : [],
            form:[],
            search:[]
          }
          for (let item of types) {
            if (func.notEmpty(item.propLocation)) {
              const propLocationList = item.propLocation.split(",")
              for (let location of propLocationList) {
                if (location === 'page_list') {
                  dataByMenuIdByType.table.push(item)
                }
                else if (location === 'page_search') {
                  dataByMenuIdByType.search.push(item)
                }
                else if (location === 'page_dialog') {
                  dataByMenuIdByType.form.push(item)
                }
                else if (location === 'page_default') {
                  dataByMenuIdByType.table.push(item)
                  dataByMenuIdByType.search.push(item)
                  dataByMenuIdByType.form.push(item)
                }
              }
            }
          }
          finalType[key] = dataByMenuIdByType
        }
        // console.log("selectDomNote",finalType)
        this.menuIdNoteObj = finalType
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
            this.refreshSystemNote()
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
      this.$confirm("确定将选择数据删除?", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      })
          .then(() => {
            return remove(this.ids);
          })
          .then(() => {
            this.onLoad(this.page);
            this.refreshSystemNote()
            this.$message({
              type: "success",
              message: "操作成功!"
            });
            this.$refs.crud.toggleSelection();
          });
    },
    beforeOpen(done, type) {
      const noteIdColumn = this.findObject(this.option.column, "noteId");
      noteIdColumn.change = ()=>{}
      if (["edit", "view"].includes(type)) {
        getDetail(this.form.id).then(res => {
          this.form = res.data.data;
          if (this.form.hasOwnProperty("propLocation")) {
            this.form.propLocation = this.form.propLocation.split(",");
          }
          noteIdColumn.change = this.noteIdChange
        });
      }else {
        noteIdColumn.change = this.noteIdChange
      }
      done();
    },
    noteIdChange(data){
     if (func.notEmpty(data.item)) {
       this.form.content = data.item.content
     }
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
      this.menuId = this.$route.query.menuId
      if (func.isEmpty(this.menuId)){
        getMenuByPath({pagePath: this.$route.path}).then(res=>{
          if (res.data.code == 200) {
            this.menuId = res.data.data.id
            this.loadPageByMenuId(page)
          }
        })
      }else {
        this.loadPageByMenuId(page)
      }
    },
    loadPageByMenuId(page) {
      getList(page.currentPage, page.pageSize, {menuId_equal: this.menuId}).then(res => {
        const data = res.data.data;
        this.page.total = data.total;
        this.data = data.records;
        this.loading = false;
        this.selectionClear();
      });
    },
    showNoteDialog() {
      this.data = []
      this.query={}
      this.showNoteSettingDialog = true
      this.onLoad(this.page)
    },
  }
};
</script>


