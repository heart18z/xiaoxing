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
      @cell-click="rowView"
      @cell-dblclick="editList"
      @on-load="onLoad"
      @tree-load="treeLoad"
    >
      <!-- 左边按钮 -->
      <template #menu-left>
        <el-button
          class="button-size"
          round
          title="新增"
          v-if="permission.menu_add"
          size="small"
          icon="el-icon-plus"
          @click.stop="addList()"
        />
        <el-button
          class="button-size"
          round
          title="删除"
          v-if="permission.menu_delete"
          size="small"
          icon="el-icon-delete"
          @click.stop="handleDelete()"
        />
        <el-button
          class="button-size"
          round
          title="复制"
          v-if="permission.menu_add"
          size="small"
          icon="el-icon-document-copy"
          @click.stop="copyList()"
        />
        <el-button
          class="button-size"
          round
          title="新增子项"
          v-if="isAdmin"
          size="small"
          icon="el-icon-setting"
          @click.stop="handleAdd()"
        />
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

      <template #source="{ row }">
        <div style="text-align: center">
          <i :class="row.source" />
        </div>
      </template>
    </avue-crud>
    <!-- 左下按钮 -->
    <page-attach ref="pageAttach" />
  </basic-container>
</template>

<script>
import { getLazyList, remove, update, add, getMenu, getMenuTree } from '@/api/system/menu';
import { mapGetters } from 'vuex';
import iconList from '@/config/iconList';
import func from '@/utils/func';
import pageAttach from '@/components/attach-dialog/page-attach/main.vue';

export default {
  components: {
    pageAttach,
  },
  data() {
    return {
      form: {},
      query: {},
      loading: true,
      selectionList: [],
      parentId: 0,
      timer: null,
      page: {
        pageSize: 30,
        currentPage: 1,
        total: 0,
      },
      option: {
        searchShowBtn: false,
        dialogDrag: true,
        lazy: true,
        tip: false,
        simplePage: true,
        searchShow: true,
        searchMenuSpan: 6,
        dialogWidth: '60%',
        tree: true,
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
        menuWidth: 300,
        dialogClickModal: false,
        column: [
          {
            label: '序号',
            prop: 'no',
            width: 90,
            display: false,
          },
          {
            label: '菜单名称',
            prop: 'name',
            search: true,
            rules: [
              {
                required: true,
                message: '请输入菜单名称',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '路由地址',
            prop: 'path',
            rules: [
              {
                required: true,
                message: '请输入路由地址',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '上级菜单',
            prop: 'parentId',
            type: 'tree',
            dicData: [],
            hide: true,
            addDisabled: false,
            props: {
              label: 'title',
            },
            rules: [
              {
                required: false,
                message: '请选择上级菜单',
                trigger: 'click',
              },
            ],
          },
          {
            label: '菜单图标',
            prop: 'source',
            type: 'icon',
            slot: true,
            iconList: iconList,
            rules: [
              {
                required: true,
                message: '请输入菜单图标',
                trigger: 'click',
              },
            ],
          },
          {
            label: '菜单编号',
            prop: 'code',
            search: true,
            rules: [
              {
                required: true,
                message: '请输入菜单编号',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '菜单类型',
            prop: 'category',
            type: 'radio',
            dicData: [
              {
                label: '菜单',
                value: 1,
              },
              {
                label: '按钮',
                value: 2,
              },
            ],
            hide: true,
            rules: [
              {
                required: true,
                message: '请选择菜单类型',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '菜单别名',
            prop: 'alias',
            search: true,
            rules: [
              {
                required: true,
                message: '请输入菜单别名',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '新窗口',
            prop: 'isOpen',
            type: 'radio',
            disabled: false,
            display: false,
            dicData: [
              {
                label: '否',
                value: 1,
              },
              {
                label: '是',
                value: 2,
              },
            ],
            value: 1,
            rules: [
              {
                required: true,
                message: '请选择新窗口打开',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '菜单排序',
            prop: 'sort',
            type: 'number',
            rules: [
              {
                required: true,
                message: '请输入菜单排序',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '菜单备注',
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
  watch: {
    'form.category'() {
      const category = func.toInt(this.form.category);
      this.$refs.crud.option.column.filter(item => {
        if (item.prop === 'path') {
          item.rules[0].required = category === 1;
        }
        if (item.prop === 'isOpen') {
          item.disabled = category === 2;
        }
      });
    },
  },
  computed: {
    ...mapGetters(['userInfo', 'permission']),
    isAdmin() {
      const roleName = this.userInfo?.role_name || this.userInfo?.authority || '';
      return roleName.includes('admin');
    },
    permissionList() {
      return {
        addBtn: this.validData(this.permission.menu_add, false),
        viewBtn: this.validData(this.permission.menu_view, false),
        delBtn: this.validData(this.permission.menu_delete, false),
        editBtn: this.validData(this.permission.menu_edit, false),
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
      if (property === 'no') {
        clearTimeout(this.timer);
        if (this.permission.menu_edit) {
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
    initData() {
      getMenuTree().then(res => {
        const column = this.findColumn(this.option.column, 'parentId');
        column.dicData = res.data.data;
      });
    },
    handleAdd() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      let row = this.selectionList[0];
      if (row.category === 1) {
        this.parentId = row.id;
        const column = this.findColumn(this.option.column, 'parentId');
        column.value = row.id;
        column.addDisabled = true;
        this.$refs.crud.rowAdd();
      } else {
        this.$message.warning('当前菜单类型下不支持新增子项！');
      }
    },
    rowSave(row, done, loading) {
      add(row).then(
        res => {
          const data = res.data.data;
          row.id = data.id;
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.$store.dispatch('GetMenu');
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
          this.$store.dispatch('GetMenu');
          row.hasChildren = true;
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
          row.hasChildren = true;
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
        this.initData();
      }
      if (['edit', 'view'].includes(type)) {
        getMenu(this.form.id).then(res => {
          this.form = res.data.data;
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
      this.parentId = '';
      this.page.currentPage = 1;
      this.onLoad(this.page, this.query);
    },
    assignRowNo(rows) {
      let index = 1;
      rows.forEach(row => {
        row.no = index + '';
        index++;
      });
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getLazyList(this.parentId, Object.assign(params, this.query)).then(res => {
        this.assignRowNo(res.data.data);
        this.data = res.data.data;
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


