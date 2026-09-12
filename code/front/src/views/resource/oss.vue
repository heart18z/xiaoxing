<template>
  <basic-container>
    <avue-crud
      :option="option"
      v-model:search="query"
      :table-loading="loading"
      :data="data"
      v-model:page="page"
      :permission="permissionList"
      v-model="form"
      ref="crud"
      @row-update="rowUpdate"
      @row-save="rowSave"
      @cell-click="rowView"
      @cell-dblclick="editList"
      @row-del="rowDel"
      :before-open="beforeOpen"
      @search-change="searchChange"
      @search-reset="searchReset"
      @selection-change="selectionChange"
      @current-change="currentChange"
      @size-change="sizeChange"
      @refresh-change="refreshChange"
      @on-load="onLoad"
    >
      <template #menu-left>
        <el-button
          class="button-size"
          round
          title="新增"
          v-if="permission.oss_add"
          size="small"
          icon="el-icon-plus"
          @click.stop="addList"
        />
        <el-button
          class="button-size"
          round
          title="删除"
          v-if="permission.oss_delete"
          size="small"
          icon="el-icon-delete"
          @click.stop="handleDelete"
        />
        <el-button
          class="button-size"
          round
          title="复制"
          v-if="permission.oss_add"
          size="small"
          icon="el-icon-document-copy"
          @click.stop="copyList"
        />
        <el-button
          class="button-size"
          round
          title="调试"
          size="small"
          icon="el-icon-video-play"
          v-if="userInfo.role_name.includes('admin')"
          @click.stop="handleDebug"
        />
      </template>

      <template #index="{ index }">
        <div>{{ index + 1 }}</div>
      </template>

      <template #status="{ row }">
        <el-switch :model-value="Number(row.status)" :active-value="2" :inactive-value="1"
          :loading="statusUpdating===String(row.id)" :disabled="!!statusUpdating"
          @change="value=>handleStatus(row,value)" />
      </template>

      <template #category="{ row }">
        <el-tag>{{ row.categoryName }}</el-tag>
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
    </avue-crud>
    <page-attach ref="pageAttach" />

    <el-dialog title="对象存储上传调试" append-to-body v-model="box" width="550px">
      <avue-form ref="form" :option="debugOption" v-model="debugForm" @submit="handleSubmit" />
    </el-dialog>
  </basic-container>
</template>

<script>
import { getList, getDetail, add, update, remove, enable } from '@/api/resource/oss';
import { mapGetters } from 'vuex';
import func from '@/utils/func';
import { sensitive } from '@/utils/sensitive';
import { introduceOssCode } from '@/const/system/common';
import pageAttach from '@/components/attach-dialog/page-attach/main.vue';

export default {
  components: {
    pageAttach,
  },
  data() {
    return {
      introduceOssCode,
      dic: [
        { label: '', value: 1 },
        { label: '', value: 2 },
      ],
      form: {},
      query: {
        status: '2',
      },
      sensitiveManager: null,
        loading: true,
        statusUpdating: '',
      box: false,
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
        saveBtn: false,
        updateBtn: false,
        cancelBtn: false,
        menu: false,
        addBtn: false,
        delBtn: false,
        editBtn: false,
        searchBtn: false,
        emptyBtn: false,
        height: 'auto',
        calcHeight: 30,
        tip: false,
        searchShow: true,
        searchMenuSpan: 6,
        border: true,
        viewBtn: true,
        selection: true,
        dialogClickModal: false,
        menuWidth: 350,
        labelWidth: 100,
        dialogWidth: 880,
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
            label: '分类',
            type: 'radio',
            value: 1,
            span: 24,
            width: 120,
            searchLabelWidth: 50,
            row: true,
            dicMethod: 'post',
            dicUrl: '/blade-system/dict/dictionary?code=oss',
            props: {
              label: 'dictValue',
              value: 'dictKey',
            },
            dataType: 'number',
            slot: true,
            prop: 'category',
            search: true,
            rules: [
              {
                required: true,
                message: '请选择分类',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '资源编号',
            prop: 'ossCode',
            span: 24,
            width: 120,
            search: true,
            rules: [
              {
                required: true,
                message: '请输入资源编号',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '资源地址',
            labelTip: '对象存储通用资源地址，可以是内网也可以是外网',
            prop: 'endpoint',
            span: 24,
            rules: [
              {
                required: true,
                message: '请输入资源地址',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '外网地址',
            labelTip: '资源地址设置为内网上传，则外部访问需要配置外网映射地址',
            prop: 'transformEndpoint',
            span: 24,
          },
          {
            label: '空间名',
            prop: 'bucketName',
            span: 24,
            width: 120,
            rules: [
              {
                required: true,
                message: '请输入空间名',
                trigger: 'blur',
              },
            ],
          },
          {
            label: 'accessKey',
            prop: 'accessKey',
            span: 24,
            search: true,
            width: 200,
            overHidden: true,
            rules: [
              {
                required: true,
                message: '请输入accessKey',
                trigger: 'blur',
              },
            ],
          },
          {
            label: 'secretKey',
            prop: 'secretKey',
            span: 24,
            width: 200,
            hide: true,
            overHidden: true,
            rules: [
              {
                required: true,
                message: '请输入secretKey',
                trigger: 'blur',
              },
            ],
          },
          {
            label: 'appId',
            prop: 'appId',
            span: 24,
            hide: true,
            display: false,
          },
          {
            label: 'region',
            prop: 'region',
            span: 24,
            hide: true,
            display: false,
          },
          {
            label: '是否启用',
            type: 'select',
            prop: 'status',
            span: 24,
            width: 180,
            align: 'center',
            slot: true,
            search: true,
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            dicMethod: 'post',
            dicUrl: '/blade-system/dict/dictionary?code=yes_no',
            props: {
              label: 'dictValue',
              value: 'dictKey',
            },
            searchValue: '2',
          },
          {
            label: '备注',
            prop: 'remark',
            span: 24,
            hide: true,
          },
        ],
      },
      data: [],
      debugForm: {
        code: '',
      },
      debugOption: {
        submitText: '提交',
        column: [
          {
            label: '资源编号',
            prop: 'code',
            disabled: true,
            span: 24,
          },
          {
            label: '上传背景',
            prop: 'backgroundUrl',
            type: 'upload',
            listType: 'picture-img',
            dataType: 'string',
            action: '/blade-resource/oss/endpoint/put-file',
            propsHttp: {
              res: 'data',
              url: 'link',
            },
            span: 24,
          },
        ],
      },
    };
  },
  watch: {
    'form.category'() {
      const category = func.toInt(this.form.category);
      this.$refs.crud.option.column.filter(item => {
        if (item.prop === 'endpoint') {
          item.labelTip = category === 7 ? '本地服务器上传路径使用Nginx代理后的服务地址' : '';
        }
        if (item.prop === 'accessKey') {
          item.labelTip =
            category === 7
              ? '本地服务器文件上传基础路径，注意必须提前创建好并分配目录读写权限'
              : '';
        }
        if (item.prop === 'secretKey') {
          item.display = category !== 7;
        }
        if (item.prop === 'appId') {
          item.display = category === 4;
        }
        if (item.prop === 'region') {
          item.display = category === 4 || category === 5;
        }
      });
    },
    'debugForm.code'() {
      const column = this.findColumn(this.debugOption.column, 'backgroundUrl');
      column.action = `/blade-resource/oss/endpoint/put-file?code=${this.debugForm.code}`;
    },
  },
  computed: {
    ...mapGetters(['userInfo', 'permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.oss_add),
        viewBtn: this.validData(this.permission.oss_view),
        delBtn: this.validData(this.permission.oss_delete),
        editBtn: this.validData(this.permission.oss_edit),
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
  created() {
    this.sensitiveManager = sensitive.create({
      fields: ['accessKey', 'secretKey'],
    });
  },
  methods: {
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
      this.form = { ...this.selectionList[0], id: '', status: '1' };
      this.$refs.crud.dicInit();
    },
    editList(row, { property }) {
      if (this.permission.oss_edit && property === 'index') {
        if (row.ossCode === introduceOssCode && !this.userInfo.role_name.includes('admin')) {
          this.$message.warning('该数据只有管理员可以编辑');
          return;
        }
        clearTimeout(this.timer);
        this.$refs.crud.dicInit();
        this.$refs.crud.rowEdit(row, row.$index);
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
    rowSave(row, done, loading) {
      add(row).then(
        () => {
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          done();
        },
        error => {
          window.console.log(error);
          loading();
        }
      );
    },
    rowUpdate(row, index, done, loading) {
      const submitData = this.sensitiveManager.getSubmitData(row);
      update(submitData).then(
        () => {
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
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
    async handleStatus(row,value) {
      if(this.statusUpdating)return;
      this.statusUpdating=String(row.id);
      try {
        const response=value===2?await enable(row.id):await update({id:row.id,status:1});
        if(response.data.success===false)throw new Error(response.data.msg||'存储配置未更新');
        row.status=value;
        await this.onLoad(this.page);
        this.$message.success(value===2?'已启用该对象存储':'已停用该对象存储');
      } catch(error) {
        this.$message.error(error.message||'更新失败，请重试');
      } finally {this.statusUpdating='';}
    },
    enableRow(row) {
      enable(row.id).then(() => {
        this.onLoad(this.page);
        this.$message({
          type: 'success',
          message: '操作成功!',
        });
        this.$refs.crud.toggleSelection();
      });
    },
    disableRow(row) {
      add({
        id: row.id,
        status: 1,
      }).then(() => {
        this.onLoad(this.page);
        this.$message({
          type: 'success',
          message: '操作成功!',
        });
        this.$refs.crud.toggleSelection();
      });
    },
    handleDebug() {
      if (this.selectionList.length !== 1) {
        this.$message.warning('只能选择一条数据');
        return;
      }
      const row = this.selectionList[0];
      this.box = true;
      this.debugForm.code = row.ossCode;
      this.debugForm.backgroundUrl = '';
    },
    handleSubmit(form, done) {
      this.$message({
        type: 'success',
        message: `获取到图片地址:[${form.backgroundUrl}]`,
      });
      done();
    },
    handleDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
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
    beforeOpen(done, type) {
      if (['edit', 'view'].includes(type)) {
        getDetail(this.form.id).then(res => {
          this.form = res.data.data;
          this.sensitiveManager.saveInitialData(this.form);
        });
      }
      done();
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
      const { category, ossCode, status } = Object.assign(params, this.query);
      const values = {
        ossCode_like: ossCode,
        category_equal: category,
        status_equal: status,
      };
      this.loading = true;
      getList(page.currentPage, page.pageSize, values).then(res => {
        const data = res.data.data;
        this.page.total = data.total;
        this.data = data.records;
        this.loading = false;
        this.selectionClear();
      });
    },
  },
};
</script>
