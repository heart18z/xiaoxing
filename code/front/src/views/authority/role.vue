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
      @cell-click="rowView"
      @cell-dblclick="editList"
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
    >
      <template #menu-left>
        <el-button
          class="button-size"
          round
          title="新增"
          v-if="permission.role_add"
          size="small"
          icon="el-icon-plus"
          @click.stop="addList"
        />
        <el-button
          class="button-size"
          round
          title="删除"
          v-if="permission.role_delete"
          size="small"
          icon="el-icon-delete"
          @click.stop="handleDelete"
        />
        <el-button
          class="button-size"
          round
          title="复制"
          v-if="permission.role_add"
          size="small"
          icon="el-icon-document-copy"
          @click.stop="copyList"
        />
        <el-button
          class="button-size"
          round
          title="权限设置"
          size="small"
          icon="el-icon-setting"
          v-if="userInfo.role_name.includes('admin')"
          @click="handleRole"
        />
      </template>

      <template #roleType="{ row }">
        <span>{{ row.$roleType || row.roleType }}</span>
      </template>

      <template #roleTypeType="{ item }">
        <span>{{ item?.dictValue }}</span>
        <span v-if="item?.remark" style="float: right">
          <el-tooltip effect="dark" :content="item.remark" placement="bottom">
            <el-icon class="el-icon-info"><InfoFilled /></el-icon>
          </el-tooltip>
        </span>
      </template>

      <template #menu-form="{ type }">
        <template v-if="type !== 'view'">
          <el-button
            icon="el-icon-check"
            size="small"
            circle
            title="保存"
            @click="$refs.crud.rowSave()"
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

    <page-attach  ref="pageAttach" />

    <el-dialog
      title="角色权限配置"
      append-to-body
      draggable
      :modal="false"
      v-model="box"
      width="345px"
    >
      <el-tabs type="border-card">
        <el-tab-pane label="菜单权限">
          <el-tree
            :data="menuGrantList"
            show-checkbox
            check-strictly
            node-key="id"
            ref="treeMenu"
            :default-checked-keys="menuTreeObj"
            :props="props"
            @check="checkChange"
          >
            <template #default="{ node, data }">
              <span class="custom-tree-node">
                <span>{{ node.label }}</span>
                <el-tooltip
                  v-if="func.notEmpty(data.remark)"
                  effect="dark"
                  :content="data.remark"
                  placement="bottom"
                >
                  <el-icon class="el-icon-info"><InfoFilled /></el-icon>
                </el-tooltip>
              </span>
            </template>
          </el-tree>
        </el-tab-pane>
        <el-tab-pane label="数据权限">
          <el-tree
            :data="dataScopeGrantList"
            show-checkbox
            node-key="id"
            ref="treeDataScope"
            :default-checked-keys="dataScopeTreeObj"
            :props="props"
          />
        </el-tab-pane>
        <el-tab-pane label="接口权限">
          <el-tree
            :data="apiScopeGrantList"
            show-checkbox
            node-key="id"
            ref="treeApiScope"
            :default-checked-keys="apiScopeTreeObj"
            :props="props"
          />
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <span class="dialog-footer">
          <el-button circle icon="el-icon-close" size="small" title="取 消" @click="box = false" />
          <el-button
            icon="el-icon-check"
            size="small"
            circle
            title="确 定"
            @click="submit"
          />
        </span>
      </template>
    </el-dialog>
  </basic-container>
</template>

<script>
import { InfoFilled } from '@element-plus/icons-vue';
import {
  add,
  getList,
  getRole,
  getRoleTreeById,
  grant,
  grantTree,
  remove,
  update,
} from '@/api/system/role';
import { mapGetters } from 'vuex';
import pageAttach from '@/components/attach-dialog/page-attach/main.vue';
import func from '@/utils/func';
import { DEFAULT_TENANT_ID } from '@/utils/tenant';

export default {
  components: {
    pageAttach,
    InfoFilled,
  },
  data() {
    return {
      form: {},
      box: false,
      props: {
        label: 'title',
        value: 'key',
      },
      menuGrantList: [],
      dataScopeGrantList: [],
      apiScopeGrantList: [],
      menuTreeObj: [],
      dataScopeTreeObj: [],
      apiScopeTreeObj: [],
      selectionList: [],
      query: {},
      loading: true,
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
        height: 'auto',
        calcHeight: 30,
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
            fixed: true,
            width: 90,
            display: false,
          },
          {
            label: '角色名称',
            prop: 'roleName',
            search: true,
            rules: [
              {
                required: true,
                message: '请输入角色名称',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '角色类别',
            prop: 'roleType',
            type: 'select',
            width: 100,
            dicMethod: 'post',
            dicUrl: '/api/blade-system/dict/dictionary?code=role_type',
            props: {
              label: 'dictValue',
              value: 'dictKey',
            },
            slot: true,
            rules: [
              {
                required: true,
                message: '请选择角色类别',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '角色别名',
            prop: 'roleAlias',
            search: true,
            rules: [
              {
                required: true,
                message: '请输入角色别名',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '上级角色',
            prop: 'parentId',
            dicData: [],
            type: 'tree',
            hide: true,
            props: {
              label: 'title',
            },
            rules: [
              {
                required: false,
                message: '请选择上级角色',
                trigger: 'click',
              },
            ],
          },
          {
            label: '角色排序',
            prop: 'sort',
            type: 'number',
            width: 90,
            rules: [
              {
                required: true,
                message: '请输入角色排序',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '角色用途',
            prop: 'purpose',
            type: 'input',
          },
          {
            label: '备注说明',
            prop: 'remark',
            type: 'input',
            row: true,
            span: 24,
          },
        ],
      },
      data: [],
    };
  },
  computed: {
    func() {
      return func;
    },
    ...mapGetters(['userInfo', 'permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.role_add, false),
        viewBtn: this.validData(this.permission.role_view, false),
        delBtn: this.validData(this.permission.role_delete, false),
        editBtn: this.validData(this.permission.role_edit, false),
      };
    },
    ids() {
      const ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
    idsArray() {
      const ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids;
    },
  },
  methods: {
    withDefaultTenant(row) {
      return {
        ...row,
        tenantId: DEFAULT_TENANT_ID,
      };
    },
    editList(row, { property }) {
      if (this.permission.role_edit && property === 'no') {
        clearTimeout(this.timer);
        this.$refs.crud.dicInit();
        this.$refs.crud.rowEdit(row, row.$index);
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
      this.form = { tenantId: DEFAULT_TENANT_ID };
      this.$refs.crud.dicInit();
    },
    copyList() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$refs.crud.rowAdd();
      this.form = {
        ...this.selectionList[0],
        id: '',
        tenantId: DEFAULT_TENANT_ID,
      };
      this.$refs.crud.dicInit();
    },
    initData(roleId) {
      getRoleTreeById(roleId).then(res => {
        const column = this.findColumn(this.option.column, 'parentId');
        column.dicData = res.data.data;
      });
    },
    submit() {
      const menuList = this.$refs.treeMenu.getCheckedKeys();
      const dataScopeList = this.$refs.treeDataScope.getCheckedKeys();
      const apiScopeList = this.$refs.treeApiScope.getCheckedKeys();
      grant(this.idsArray, menuList, dataScopeList, apiScopeList).then(() => {
        this.box = false;
        this.$message({
          type: 'success',
          message: '操作成功!',
        });
        this.onLoad(this.page);
      });
    },
    rowSave(row, done, loading) {
      add(this.withDefaultTenant(row)).then(
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
      update(this.withDefaultTenant(row)).then(
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
    beforeOpen(done, type) {
      if (['add', 'edit'].includes(type)) {
        this.initData(this.form.id);
      }
      if (type === 'add') {
        this.form.tenantId = DEFAULT_TENANT_ID;
      }
      done();
    },
    checkChange(data) {
      const node = this.$refs.treeMenu.getNode(data.id);
      this.setNode(node);
    },
    setNode(node) {
      if (node.checked) {
        this.setParentNode(node);
        this.setChildenNode(node, true);
      } else {
        this.setChildenNode(node, false);
      }
    },
    setParentNode(node) {
      if (node.parent) {
        for (const key in node) {
          if (key === 'parent') {
            node[key].checked = true;
            this.setParentNode(node[key]);
          }
        }
      }
    },
    setChildenNode(node, check) {
      const len = node.childNodes.length;
      for (let i = 0; i < len; i++) {
        node.childNodes[i].checked = check;
        this.setChildenNode(node.childNodes[i], check);
      }
    },
    handleRole() {
      if (this.selectionList.length !== 1) {
        this.$message.warning('只能选择一条数据');
        return;
      }

      if (this.selectionList[0].roleType === 'func') {
        this.$message.warning('功能角色可以不用配置菜单，如果功能需要配置菜单，请慎用!');
      }

      this.menuTreeObj = [];
      this.dataScopeTreeObj = [];
      this.apiScopeTreeObj = [];
      grantTree().then(res => {
        const grantData = res.data.data;
        this.menuGrantList = grantData.menuVOS || grantData.menu || [];
        this.dataScopeGrantList = grantData.dataScope;
        this.apiScopeGrantList = grantData.apiScope;
        getRole(this.ids).then(roleRes => {
          this.menuTreeObj = roleRes.data.data.menu;
          this.dataScopeTreeObj = roleRes.data.data.dataScope;
          this.apiScopeTreeObj = roleRes.data.data.apiScope;
          this.box = true;
        });
      });
    },
    handleDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      const funcList = this.selectionList.filter(ele => ele.roleType === 'func');
      if (funcList.length > 0) {
        this.$message.warning('所选角色包含功能角色不能删除！');
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
    onLoad(page, params = {}) {
      this.loading = true;
      getList(page.currentPage, page.pageSize, Object.assign(params, this.query)).then(res => {
        this.data = res.data.data;
        const tf = data => {
          let i = 1;
          for (const child of data) {
            child.no = i++;
            if (child.children && child.children.length) {
              tf(child.children);
            }
          }
        };
        tf(this.data);
        this.loading = false;
        this.selectionClear();
      });
    },
  },
};
</script>

<style scoped>
.custom-tree-node {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
