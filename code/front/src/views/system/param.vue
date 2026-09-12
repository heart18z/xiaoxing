<template>
  <basic-container>
    <avue-crud
      :option="option"
      v-model:search="query"
      v-model:page="page"
      :table-loading="loading"
      :data="data"
      v-model="form"
      ref="crud"
      :permission="permissionList"
      :before-open="beforeOpen"
      @row-del="rowDel"
      @row-update="rowUpdate"
      @row-save="rowSave"
      @search-change="searchChange"
      @search-reset="searchReset"
      @selection-change="selectionChange"
      @current-change="currentChange"
      @size-change="sizeChange"
      @refresh-change="refreshChange"
      @cell-click="rowView"
      @cell-dblclick="editList"
      @on-load="onLoad"
    >
      <!-- 左边按钮 -->
      <template #menu-left>
        <el-button
          class="button-size"
          round
          title="新增"
          v-if="isAdmin"
          size="small"
          icon="el-icon-plus"
          @click.stop="addList()"
        />
        <el-button
          class="button-size"
          round
          title="删除"
          v-if="isAdmin"
          size="small"
          icon="el-icon-delete"
          @click.stop="handleDelete()"
        />
        <el-button
          class="button-size"
          round
          title="复制"
          v-if="isAdmin"
          size="small"
          icon="el-icon-document-copy"
          @click.stop="copyList()"
        />
      </template>

      <template #paramName="{ row }">
        <span v-if="row.paramKey === 'oauth2'">
          {{ row.paramName }}
          <el-tooltip class="item" effect="dark" :content="oauth2Tip" placement="top">
            <el-icon class="el-icon-question"><QuestionFilled /></el-icon>
          </el-tooltip>
        </span>
        <el-popover
          v-else
          :disabled="row.remark == null || row.remark == ''"
          placement="top"
          title="备注"
          width="200"
          trigger="click"
          :content="row.remark"
        >
          <template #reference>
            <span>{{ row.paramName }}</span>
          </template>
        </el-popover>
      </template>

      <template #menu-form="{ type }">
        <template v-if="type !== 'view'">
          <el-button
            icon="el-icon-check"
            size="small"
            circle
            @click="$refs.crud.rowSave()"
            title="保存"
          />
          <el-button
            circle
            icon="el-icon-close"
            size="small"
            title="取消"
            @click="$refs.crud.closeDialog()"
          />
        </template>
      </template>

      <template #index="{ index }">
        <div>{{ index + 1 }}</div>
      </template>
    </avue-crud>
    <!-- 左下按钮 -->
    <page-attach ref="pageAttach" />
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { getList, update, add, remove, getSysTemParamMap, detail } from '@/api/system/param';
import pageAttach from '@/components/attach-dialog/page-attach/main.vue';
import func from '@/utils/func';
import { QuestionFilled } from '@element-plus/icons-vue';

export default {
  components: {
    pageAttach,
    QuestionFilled,
  },
  data() {
    return {
      oauth2Tip:
        '当前参数为系统初始化设置参数，记录顶部按钮“授权系统登录”所控制的状态，不可编辑或删除！',
      form: {},
      query: {},
      loading: true,
      timer: null,
      page: {
        pageSize: 30,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      option: {
        searchShowBtn: false,
        dialogDrag: true,
        height: 'auto',
        calcHeight: 30,
        tip: false,
        searchShow: true,
        searchMenuSpan: 6,
        border: true,
        index: false,
        saveBtn: false,
        updateBtn: false,
        cancelBtn: false,
        menu: false,
        addBtn: false,
        delBtn: false,
        editBtn: false,
        searchBtn: false,
        emptyBtn: false,
        selection: true,
        viewBtn: true,
        dialogClickModal: false,
        column: [
          {
            label: '序号',
            prop: 'index',
            fixed: true,
            width: 70,
            display: false,
            align: 'center',
          },
          {
            label: '参数名称',
            prop: 'paramName',
            search: true,
            slot: true,
            span: 24,
            rules: [
              {
                required: true,
                message: '请输入参数名称',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '参数阶段',
            prop: 'paramParameterSearch',
            search: true,
            span: 24,
            dicMethod: 'post',
            dicUrl: '/api/blade-system/dict-biz/dictionary?code=parameter',
            type: 'select',
            display: false,
            hide: true,
            props: {
              label: 'dictValue',
              value: 'dictKey',
            },
          },
          {
            label: '参数阶段',
            prop: 'paramParameter',
            search: true,
            type: 'select',
            span: 24,
            dicMethod: 'post',
            dicUrl: '/api/blade-system/dict-biz/dictionary?code=parameter',
            multiple: true,
            props: {
              label: 'dictValue',
              value: 'dictKey',
            },
            rules: [
              {
                required: true,
                message: '请选择用途类型',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '参数键名',
            prop: 'paramKey',
            search: true,
            span: 24,
            rules: [
              {
                required: true,
                message: '请输入参数键名',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '参数键值',
            prop: 'paramValue',
            type: 'textarea',
            span: 24,
            minRows: 6,
            rules: [
              {
                required: true,
                message: '请输入参数键值',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '更新人',
            span: 24,
            prop: 'updateUser',
            type: 'select',
            dicMethod: 'post',
            dicUrl: '/api/blade-system/user/user-kv-list',
            width: '100',
            dicFormatter: res => {
              return res.data;
            },
            props: {
              label: 'key',
              value: 'value',
            },
            display: false,
          },
          {
            label: '更新时间',
            span: 24,
            width: '200',
            prop: 'updateTime',
            display: false,
          },
          {
            label: '备注',
            prop: 'remark',
            type: 'textarea',
            span: 24,
            minRows: 6,
            hide: true,
          },
        ],
      },
      data: [],
    };
  },
  computed: {
    ...mapGetters(['permission', 'userInfo']),
    isAdmin() {
      const roleName = this.userInfo?.role_name || this.userInfo?.authority || '';
      return roleName.includes('administrator') || roleName.includes('admin');
    },
    permissionList() {
      return {
        addBtn: this.validData(this.permission.param_add, false),
        viewBtn: this.validData(this.permission.param_view, false),
        delBtn: this.validData(this.permission.param_delete, false),
        editBtn: this.validData(this.permission.param_edit, false),
      };
    },
    ids() {
      let ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
  },
  methods: {
    editList(row, { property }) {
      clearTimeout(this.timer);
      if (row.paramKey === 'oauth2') {
        this.$message.warning(this.oauth2Tip);
        return;
      }
      if (this.permission.param_edit && property === 'index') {
        if (this.isAdmin) {
          this.$refs.crud.dicInit();
          this.$refs.crud.rowEdit(row, row.$index);
        } else {
          this.$message({
            type: 'warning',
            message: '没有权限!',
          });
        }
      }
    },
    rowView(row, { property }) {
      if (property === 'index') {
        clearTimeout(this.timer);
        this.timer = setTimeout(() => {
          this.$refs.crud.rowView(row, row.$index);
        }, 250);
      }
    },
    refreshSystemParam() {
      getSysTemParamMap().then(res => {
        const data = res.data.data;
        if (func.notEmpty(data)) {
          this.$store.commit('SET_SYSTEM_PARAM', res.data.data);
          let title = func.notEmpty(data['project.name']) ? data['project.name'] : 'Saber企业平台';
          this.$router.$avueRouter.setTitle(title);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
        }
      });
    },
    rowSave(row, done, loading) {
      add(row).then(
        () => {
          this.onLoad(this.page);
          this.refreshSystemParam();
          done();
        },
        error => {
          window.console.log(error);
          loading();
        }
      );
    },
    async rowUpdate(row, index, done, loading) {
      if (
        row.paramKey === 'remote.dict' &&
        row.originValue !== row.paramValue &&
        (row.originValue === 'true' || row.paramValue === 'true')
      ) {
        const tips =
          row.paramValue === 'true'
            ? '启用远程字典时，当前本地应用字典中的手动创建数据（含同步的链状父级）将被保留，其余同步数据将被清空，同时所有本地应用字典状态将被更改为“本地创建”；<br/><br/>' +
              '1、如果仍然需要在远程字典中使用本地应用字典的数据，请在远程字典系统中，手动完成创建，<br/>' +
              '2、保留在本地应用的手动创建的数据（含同步的链状父级），在开启远程字典后，将不会被系统调用；'
            : '关闭远程字典时，系统将会按如下顺序执行：<br/><br/>' +
              '1、删除当前本地应用字典中的所有数据；<br/>' +
              '2、将远程字典中的“标准字典数据”和专门为本系统创建的“个性字典数据”全部同步到本系统的应用字典中，并全部标记为“数据同步”<br/>' +
              '3、系统将在本系统的“上线设定-对外交互”菜单中，自动调整“远程调用基础字典”的状态为禁用；';
        const confirm = await this.$confirm(tips, '提示', {
          dangerouslyUseHTMLString: true,
          confirmButtonText: '确定',
          customClass: 'elmessageWidth',
          cancelButtonText: '取消',
          type: 'warning',
        }).catch(() => {});
        if (confirm !== 'confirm') {
          this.onLoad(this.page);
          done();
          return;
        }
      }

      update(row).then(
        () => {
          this.onLoad(this.page);
          this.refreshSystemParam();
          done();
        },
        error => {
          window.console.log(error);
          loading();
        }
      );
    },
    rowDel(row) {
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return remove(row.id);
        })
        .then(() => {
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
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
    addList() {
      this.$refs.crud.rowAdd();
      this.form = {};
      this.$refs.crud.dicInit();
    },
    copyList() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$refs.crud.rowAdd();
      this.form = this.selectionList[0];
      this.form.id = '';
      this.$refs.crud.dicInit();
    },
    handleDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      const oauth2 = this.selectionList.find(item => item.paramKey === 'oauth2');
      if (func.notEmpty(oauth2)) {
        this.$message.warning(this.oauth2Tip);
        return;
      }
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return remove(this.ids);
        })
        .then(() => {
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.$refs.crud.toggleSelection();
        });
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
    beforeOpen(done, type) {
      this.form.paramParameter = [];
      if (['edit', 'view'].includes(type)) {
        detail({ id: this.form.id }).then(res => {
          this.form = res.data.data;
          if (Object.prototype.hasOwnProperty.call(this.form, 'paramParameter')) {
            this.form.paramParameter = this.form.paramParameter.split(',');
          }
        });
      }
      done();
    },
    onLoad(page, params = {}) {
      this.loading = true;
      const data = Object.fromEntries(
        Object.entries(Object.assign(params, this.query)).filter(
          ([, value]) => value !== null && value !== undefined && Object.keys(value).length !== 0
        )
      );
      data.$paramParameter = undefined;
      data.paramParameter = data.paramParameterSearch;
      data.paramParameterSearch = undefined;
      data.$paramParameterSearch = undefined;
      getList(page.currentPage, page.pageSize, data).then(res => {
        const data = res.data.data;
        this.page.total = data.total;
        this.data = data.records;
        for (let i = 0; i < this.data.length; i++) {
          this.data[i].originValue = this.data[i].paramValue;
        }
        this.loading = false;
        this.selectionClear();
      });
    },
  },
};
</script>



<style>
.elmessageWidth {
  width: 650px;
}
</style>
