<template>
  <basic-container>
    <avue-crud
      ref="crudTree"
      :option="optionTree"
      :table-loading="treeLoading"
      :data="dataTree"
      @search-change="searchChangeTree"
      @search-reset="searchResetTree"
      @refresh-change="refreshChangeTree"
      @selection-change="selectionChangeTree"
      @on-load="onLoadTree"
      @tree-load="treeLoad"
    >
      <template #menu-left>
        <el-button
          class="button-size"
          round
          size="small"
          title="权限配置"
          v-if="permission.data_scope_setting"
          icon="el-icon-setting"
          @click.stop="handleDataScope"
        />
      </template>
      <template #source="{ row }">
        <div style="text-align: center">
          <i :class="row.source" />
        </div>
      </template>
    </avue-crud>

    <el-drawer
      :title="`[${scopeMenuName}] 数据权限配置`"
      v-model="drawerVisible"
      :direction="direction"
      append-to-body
      :before-close="handleDrawerClose"
      size="1200px"
    >
      <basic-container>
        <avue-crud
          :option="optionScope"
          :data="dataScope"
          v-model:page="pageScope"
          v-model:search="scopeQuery"
          v-model="formScope"
          :table-loading="scopeLoading"
          ref="crudScope"
          @row-del="rowDelScope"
          @row-update="rowUpdateScope"
          @row-save="rowSaveScope"
          :before-open="beforeOpenScope"
          @search-change="searchChangeScope"
          @search-reset="searchResetScope"
          @selection-change="selectionChangeScope"
          @current-change="currentChangeScope"
          @size-change="sizeChangeScope"
          @refresh-change="refreshChangeScope"
          @cell-click="rowView"
          @cell-dblclick="editList"
          @on-load="onLoadScope"
        >
          <template #menu-left>
            <el-button
              class="button-size"
              round
              title="新增"
              size="small"
              icon="el-icon-plus"
              @click.stop="addList"
            />
            <el-button
              class="button-size"
              round
              title="删除"
              size="small"
              icon="el-icon-delete"
              @click.stop="handleDeleteScope"
            />
            <el-button
              class="button-size"
              round
              title="复制"
              size="small"
              icon="el-icon-document-copy"
              @click.stop="copyList"
            />
          </template>
          <template #scopeType="{ row }">
            <el-tag>{{ row.scopeTypeName }}</el-tag>
          </template>
          <template #index="{ index }">
            <div>{{ index + 1 }}</div>
          </template>
          <template #menu-form="{ type }">
            <template v-if="type !== 'view'">
              <el-button
                icon="el-icon-check"
                size="small"
                circle
                title="保存"
                @click="$refs.crudScope.rowSave()"
              />
              <el-button
                circle
                icon="el-icon-close"
                size="small"
                title="取消"
                @click="$refs.crudScope.closeDialog()"
              />
            </template>
          </template>
        </avue-crud>
      </basic-container>
    </el-drawer>
  </basic-container>
</template>

<script>
import { getLazyMenuList } from '@/api/system/menu';
import {
  addDataScope,
  removeDataScope,
  updateDataScope,
  getListDataScope,
  getMenuDataScope,
} from '@/api/system/scope';
import { mapGetters } from 'vuex';
import func from '@/utils/func';

export default {
  data() {
    return {
      dataTree: [],
      treeLoading: true,
      parentId: 0,
      queryTree: {},
      selectionListTree: [],
      optionTree: {
        lazy: true,
        tip: false,
        simplePage: true,
        searchShow: true,
        searchMenuSpan: 6,
        searchShowBtn: false,
        searchBtn: false,
        emptyBtn: false,
        tree: true,
        border: true,
        index: true,
        selection: true,
        menu: false,
        viewBtn: false,
        editBtn: false,
        addBtn: false,
        delBtn: false,
        dialogClickModal: false,
        column: [
          {
            label: '菜单名称',
            prop: 'name',
            search: true,
          },
          {
            label: '路由地址',
            prop: 'path',
          },
          {
            label: '菜单图标',
            prop: 'source',
            slot: true,
            width: 85,
          },
          {
            label: '菜单编号',
            prop: 'code',
            search: true,
          },
          {
            label: '菜单别名',
            prop: 'alias',
          },
          {
            label: '菜单排序',
            prop: 'sort',
            width: 85,
          },
        ],
      },
      drawerVisible: false,
      direction: 'rtl',
      scopeMenuId: 0,
      scopeMenuCode: '',
      scopeMenuName: '菜单',
      scopeLoading: false,
      watchMode: true,
      scopeQuery: {},
      timer: null,
      formScope: {},
      selectionListScope: [],
      pageScope: {
        pageSize: 30,
        currentPage: 1,
        total: 0,
      },
      optionScope: {
        saveBtn: false,
        updateBtn: false,
        cancelBtn: false,
        searchShowBtn: false,
        addBtn: false,
        delBtn: false,
        editBtn: false,
        searchBtn: false,
        emptyBtn: false,
        menu: false,
        tip: false,
        searchShow: true,
        searchMenuSpan: 6,
        border: true,
        index: false,
        viewBtn: true,
        selection: true,
        menuWidth: 200,
        dialogWidth: 900,
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
            label: '所属菜单',
            labelTip: '仅用于权限规则的分组归类，不参与实际鉴权与数据过滤逻辑',
            prop: 'menuId',
            type: 'tree',
            dicMethod: 'post',
            dicUrl: '/blade-system/menu/tree',
            props: {
              label: 'title',
            },
            span: 24,
            disabled: true,
            hide: true,
            rules: [
              {
                required: true,
                message: '请选择所属菜单',
                trigger: 'click',
              },
            ],
          },
          {
            label: '权限名称',
            prop: 'scopeName',
            search: true,
            searchSpan: 12,
            value: '',
            rules: [
              {
                required: true,
                message: '请输入数据权限名称',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '权限编号',
            prop: 'resourceCode',
            search: true,
            searchSpan: 12,
            width: 100,
            rules: [
              {
                required: true,
                message: '请输入数据权限编号',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '权限字段',
            prop: 'scopeColumn',
            width: 130,
            rules: [
              {
                required: true,
                message: '请输入数据权限编号',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '规则类型',
            type: 'select',
            dicMethod: 'post',
            dicUrl: '/blade-system/dict/dictionary?code=data_scope_type',
            props: {
              label: 'dictValue',
              value: 'dictKey',
            },
            dataType: 'number',
            slot: true,
            width: 140,
            prop: 'scopeType',
            rules: [
              {
                required: true,
                message: '请输入通知类型',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '可见字段',
            prop: 'scopeField',
            span: 24,
            hide: true,
            value: '*',
            rules: [
              {
                required: true,
                message: '请输入数据权限可见的字段',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '权限类名',
            prop: 'scopeClass',
            span: 24,
            hide: true,
            rules: [
              {
                required: true,
                message: '请输入MybatisMapper对应方法的完整类名路径',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '规则值',
            prop: 'scopeValue',
            span: 24,
            minRows: 5,
            type: 'textarea',
            display: true,
            hide: true,
          },
          {
            label: '备注',
            prop: 'remark',
            span: 24,
            hide: true,
          },
        ],
      },
      dataScope: [],
    };
  },
  watch: {
    'formScope.scopeType'() {
      this.initScope();
    },
  },
  computed: {
    ...mapGetters(['permission']),
    scopeIds() {
      let ids = [];
      this.selectionListScope.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
  },
  methods: {
    onLoadTree(page, params = {}) {
      this.treeLoading = true;
      getLazyMenuList(this.parentId, Object.assign(params, this.queryTree)).then(res => {
        this.dataTree = res.data.data;
        this.treeLoading = false;
        this.selectionListTree = [];
      });
    },
    treeLoad(tree, treeNode, resolve) {
      const parentId = tree.id;
      getLazyMenuList(parentId).then(res => {
        resolve(res.data.data);
      });
    },
    searchResetTree() {
      this.queryTree = {};
      this.parentId = 0;
      this.onLoadTree();
    },
    searchChangeTree(params, done) {
      this.queryTree = params;
      this.parentId = '';
      this.onLoadTree({}, params);
      done();
    },
    refreshChangeTree() {
      this.onLoadTree({}, this.queryTree);
    },
    selectionChangeTree(list) {
      this.selectionListTree = list;
    },
    handleDataScope() {
      if (this.selectionListTree.length !== 1) {
        this.$message.warning('必须选择一条数据');
        return;
      }
      const row = this.selectionListTree[0];
      this.drawerVisible = true;
      this.scopeMenuId = row.id;
      this.scopeMenuCode = row.code;
      this.scopeMenuName = row.name;
      this.onLoadScope(this.pageScope);
    },
    handleDrawerClose(hide) {
      hide();
    },
    addList() {
      this.formScope = {};
      this.$refs.crudScope.rowAdd();
      this.$refs.crudScope.dicInit();
    },
    copyList() {
      if (this.selectionListScope.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$refs.crudScope.rowAdd();
      this.formScope = { ...this.selectionListScope[0], id: '' };
      this.$refs.crudScope.dicInit();
    },
    editList(row, { property }) {
      if (property === 'index') {
        clearTimeout(this.timer);
        this.$refs.crudScope.dicInit();
        this.$refs.crudScope.rowEdit(row, row.$index);
      }
    },
    rowView(row, { property }) {
      if (property === 'index') {
        clearTimeout(this.timer);
        this.timer = setTimeout(() => {
          this.$refs.crudScope.rowView(row, row.$index);
        }, 250);
      }
    },

    initScope() {
      const scopeType = func.toInt(this.formScope.scopeType);
      const watchMode = this.watchMode;
      let column = '-',
        name = '暂无';
      if (scopeType === 1) {
        column = '-';
        name = '全部可见';
      } else if (scopeType === 2) {
        column = 'create_user';
        name = '本人可见';
      } else if (scopeType === 3) {
        column = 'create_dept';
        name = '所在机构可见';
      } else if (scopeType === 4) {
        column = 'create_dept';
        name = '所在机构可见及子级可见';
      } else if (scopeType === 5) {
        column = '';
        name = '自定义';
      }
      this.$refs.crudScope.option.column.filter(item => {
        if (watchMode) {
          if (item.prop === 'scopeName') {
            this.formScope.scopeName = `${this.scopeMenuName} [${name}]`;
          }
          if (item.prop === 'resourceCode') {
            this.formScope.resourceCode = this.scopeMenuCode;
          }
          if (item.prop === 'scopeColumn') {
            this.formScope.scopeColumn = column;
          }
        }
        if (item.prop === 'scopeValue') {
          item.display = scopeType === 5;
        }
      });
    },
    rowSaveScope(row, done, loading) {
      row = {
        ...row,
        menuId: this.scopeMenuId,
      };
      addDataScope(row).then(
        () => {
          this.onLoadScope(this.pageScope);
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
    rowUpdateScope(row, index, done, loading) {
      row = {
        ...row,
        menuId: this.scopeMenuId,
      };
      updateDataScope(row).then(
        () => {
          this.onLoadScope(this.pageScope);
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
    rowDelScope(row) {
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return removeDataScope(row.id);
        })
        .then(() => {
          this.onLoadScope(this.pageScope);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
        });
    },
    handleDeleteScope() {
      if (this.selectionListScope.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return removeDataScope(this.scopeIds);
        })
        .then(() => {
          this.onLoadScope(this.pageScope);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.$refs.crudScope.toggleSelection();
        });
    },
    beforeOpenScope(done, type) {
      if (['add'].includes(type)) {
        this.formScope.menuId = this.scopeMenuId;
        this.watchMode = true;
        this.initScope();
      }
      if (['edit', 'view'].includes(type)) {
        this.watchMode = false;
        getMenuDataScope(this.formScope.id).then(res => {
          this.formScope = res.data.data;
        });
      }
      done();
    },
    searchResetScope() {
      this.scopeQuery = {};
      this.onLoadScope(this.pageScope);
    },
    refreshChangeScope() {
      this.onLoadScope(this.pageScope);
    },
    searchChangeScope(params, done) {
      this.onLoadScope(this.pageScope, params);
      done();
    },
    selectionChangeScope(list) {
      this.selectionListScope = list;
    },
    currentChangeScope(currentPage) {
      this.pageScope.currentPage = currentPage;
    },
    sizeChangeScope(pageSize) {
      this.pageScope.pageSize = pageSize;
    },
    onLoadScope(page, params = {}) {
      this.scopeLoading = true;
      const values = {
        ...params,
        menuId: this.scopeMenuId,
      };
      getListDataScope(page.currentPage, page.pageSize, values).then(res => {
        const data = res.data.data;
        this.pageScope.total = data.total;
        this.dataScope = data.records;
        this.selectionListScope = [];
        this.scopeLoading = false;
      });
    },
  },
};
</script>
