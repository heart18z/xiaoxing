<template>
  <basic-container>
    <avue-crud
      :option="option"
      v-model:search="query"
      :table-loading="loading"
      :data="data"
      ref="crud"
      v-model="form"
      :permission="permissionList"
      :before-open="beforeOpen"
      :before-close="beforeClose"
      @row-del="rowDel"
      @row-update="rowUpdate"
      @row-save="rowSave"
      @search-change="searchChange"
      @search-reset="searchReset"
      @selection-change="selectionChange"
      @current-change="currentChange"
      @size-change="sizeChange"
      @refresh-change="refreshChange"
      @on-load="onLoad"
      @cell-click="rowView"
      @cell-dblclick="editList"
      @tree-load="treeLoad"
    >
      <template #menu-left>
        <el-button
          class="button-size"
          round
          title="新增"
          v-if="permission.dept_add"
          size="small"
          icon="el-icon-plus"
          @click.stop="addList"
        />
        <el-button
          class="button-size"
          round
          title="删除"
          v-if="permission.dept_delete"
          size="small"
          icon="el-icon-delete"
          @click.stop="handleDelete"
        />
        <el-button
          class="button-size"
          round
          title="复制"
          v-if="permission.dept_add"
          size="small"
          icon="el-icon-document-copy"
          @click.stop="copyList"
        />
        <el-button
          class="button-size"
          round
          title="新增子项"
          v-if="userInfo.role_name.includes('admin')"
          icon="el-icon-setting"
          size="small"
          @click.stop="handleRowClick"
        />
      </template>

      <template #deptCategory="{ row }">
        <el-tag>{{ row.deptCategoryName }}</el-tag>
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
  </basic-container>
</template>

<script>
import { getLazyList, remove, update, add, getDept, getDeptTree } from '@/api/system/dept';
import { mapGetters } from 'vuex';
import website from '@/config/website';
import func from '@/utils/func';
import pageAttach from '@/components/attach-dialog/page-attach/main.vue';

export default {
  components: {
    pageAttach,
  },
  data() {
    return {
      form: {},
      selectionList: [],
      query: {},
      loading: true,
      parentId: 0,
      timer: null,
      page: {
        pageSize: 30,
        currentPage: 1,
        total: 0,
      },
      option: {
        searchShowBtn: false,
        lazy: true,
        tip: false,
        simplePage: true,
        searchShow: true,
        searchMenuSpan: 6,
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
        tree: true,
        border: true,
        selection: true,
        viewBtn: true,
        menuWidth: 300,
        dialogWidth: 800,
        dialogClickModal: false,
        column: [
          {
            label: '序号',
            prop: 'no',
            width: 90,
            display: false,
          },
          {
            label: '机构名称',
            prop: 'deptName',
            search: true,
            rules: [
              {
                required: true,
                message: '请输入机构名称',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '机构全称',
            prop: 'fullName',
            search: true,
            rules: [
              {
                required: true,
                message: '请输入机构全称',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '上级机构',
            prop: 'parentId',
            dicData: [],
            type: 'tree',
            hide: true,
            addDisabled: false,
            props: {
              label: 'title',
            },
            rules: [
              {
                required: false,
                message: '请选择上级机构',
                trigger: 'click',
              },
            ],
          },
          {
            label: '机构类型',
            type: 'select',
            dicMethod: 'post',
            dicUrl: '/blade-system/dict/dictionary?code=org_category',
            props: {
              label: 'dictValue',
              value: 'dictKey',
            },
            dataType: 'number',
            width: 120,
            prop: 'deptCategory',
            slot: true,
            rules: [
              {
                required: true,
                message: '请输入机构类型',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '排序',
            prop: 'sort',
            type: 'number',
            align: 'center',
            width: 80,
            rules: [
              {
                required: true,
                message: '请输入排序',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '备注',
            prop: 'remark',
            type: 'textarea',
            span: 24,
            minRows: 2,
            hide: true,
          },
        ],
      },
      data: [],
    };
  },
  computed: {
    ...mapGetters(['userInfo', 'permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.dept_add, false),
        viewBtn: this.validData(this.permission.dept_view, false),
        delBtn: this.validData(this.permission.dept_delete, false),
        editBtn: this.validData(this.permission.dept_edit, false),
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
    addList() {
      this.$refs.crud.rowAdd();
      this.form = {};
      if (this.selectionList.length > 0) {
        this.form.parentId = this.selectionList[0].id;
      }
      this.$refs.crud.dicInit();
    },
    copyList() {
      this.$refs.crud.rowAdd();
      this.form = {};
      if (this.selectionList.length > 0) {
        this.form = { ...this.selectionList[0], id: '' };
      }
      this.$refs.crud.dicInit();
    },
    editList(row, { property }) {
      if (property === 'no') {
        clearTimeout(this.timer);
        if (this.permission.dept_edit) {
          this.$refs.crud.dicInit();
          this.$refs.crud.rowEdit(row, row.$index);
        }
      }
    },
    rowView(row, { property }) {
      if (property === 'no') {
        clearTimeout(this.timer);
        this.timer = setTimeout(() => {
          this.$refs.crud.rowView(row, row.$index);
        }, 250);
      }
    },
    initData(tenantId) {
      getDeptTree(tenantId).then(res => {
        const column = this.findColumn(this.option.column, 'parentId');
        column.dicData = res.data.data;
      });
    },
    handleRowClick() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      const row = this.selectionList[0];
      const column = this.findColumn(this.option.column, 'parentId');
      column.value = row.id;
      column.addDisabled = true;
      this.$refs.crud.rowAdd();
    },
    handleAdd(row) {
      this.parentId = row.id;
      const column = this.findColumn(this.option.column, 'parentId');
      column.value = row.id;
      column.addDisabled = true;
      this.$refs.crud.rowAdd();
    },
    rowSave(row, done, loading) {
      if (func.isEmpty(row.parentId)) {
        row.parentId = 0;
      }
      if (func.isEmpty(row.tenantId)) {
        row.tenantId = website.tenantId;
      }
      add(row).then(
        res => {
          const data = res.data.data;
          row.id = data.id;
          row.deptCategoryName = data.deptCategoryName;
          row.tenantId = data.tenantId;
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.$refs.crud.refreshTable();
          done(row);
        },
        error => {
          window.console.log(error);
          loading();
        }
      );
    },
    rowUpdate(row, index, done, loading) {
      update(row).then(
        () => {
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          done(row);
        },
        error => {
          window.console.log(error);
          loading();
        }
      );
    },
    rowDel(row, index, done) {
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return remove(row.id);
        })
        .then(() => {
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          done(row);
        });
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
          this.data = [];
          this.parentId = 0;
          this.$refs.crud.refreshTable();
          this.$refs.crud.toggleSelection();
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
        });
    },
    searchReset() {
      this.query = {};
      this.parentId = 0;
      this.onLoad(this.page);
    },
    searchChange(params, done) {
      this.query = params;
      this.parentId = '';
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
    beforeOpen(done, type) {
      if (['add', 'edit'].includes(type)) {
        this.initData(this.form.tenantId || website.tenantId);
      }
      if (['edit', 'view'].includes(type)) {
        getDept(this.form.id).then(res => {
          this.form = Object.assign(res.data.data, {
            hasChildren: this.form.hasChildren,
          });
          if (this.form.parentId === '0') {
            this.form.parentId = '';
          }
        });
      }
      done();
    },
    beforeClose(done) {
      this.parentId = '';
      const column = this.findColumn(this.option.column, 'parentId');
      column.value = '';
      column.addDisabled = false;
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
    assignRowNo(rows) {
      let index = 1;
      rows.forEach(row => {
        row.no = `${index}`;
        index += 1;
      });
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getLazyList(this.parentId, Object.assign(params, this.query)).then(res => {
        this.data = res.data.data;
        this.assignRowNo(this.data);
        this.loading = false;
        this.selectionClear();
      });
    },
    treeLoad(tree, treeNode, resolve) {
      const parentId = tree.id;
      getLazyList(parentId).then(res => {
        this.assignRowNo(res.data.data);
        resolve(res.data.data);
      });
    },
  },
};
</script>
