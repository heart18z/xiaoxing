<template>
  <basic-container>
    <avue-crud
      :option="option"
      v-model:search="query"
      :table-loading="loading"
      :data="data"
      v-model:page="page"
      :permission="permissionList"
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
      @on-load="onLoad"
    >
      <template #menu-left>
        <el-button
          class="button-size"
          round
          title="新增"
          v-if="permission.topmenu_add"
          size="small"
          icon="el-icon-plus"
          @click.stop="addList"
        />
        <el-button
          class="button-size"
          round
          title="删除"
          v-if="permission.topmenu_delete"
          size="small"
          icon="el-icon-delete"
          @click.stop="handleDelete"
        />
        <el-button
          class="button-size"
          round
          title="复制"
          v-if="permission.topmenu_add"
          size="small"
          icon="el-icon-document-copy"
          @click.stop="copyList"
        />
        <el-button
          class="button-size"
          round
          title="菜单配置"
          size="small"
          icon="el-icon-setting"
          v-if="permission.topmenu_setting"
          @click="handleMenuSetting"
        />
      </template>

      <template #source="{ row }">
        <div style="text-align: center">
          <i :class="row.source"></i>
        </div>
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

      <template #sort="{ row }">
        <el-input-number
          v-model="row.sort"
          @change="sortChange(row)"
          :min="1"
          :max="100"
        />
      </template>

      <template #isMain="{ row }">
        <el-switch
          v-model="row.isMain"
          inline-prompt
          :before-change="() => isMainChange(row)"
          active-text="是"
          inactive-text="否"
          :active-value="1"
          :inactive-value="0"
        />
      </template>
    </avue-crud>

    <el-dialog
      title="下级菜单配置"
      append-to-body
      draggable
      v-model="box"
      width="345px"
      @closed="handleDialogClose"
    >
      <el-row
        justify="space-between"
        align="middle"
        style="margin-bottom: 12px; background: #f5f7fa; padding: 6px 10px; border-radius: 4px"
      >
        <span style="display: inline-flex; align-items: center">
          <el-switch
            v-model="menuLinked"
            active-text="节点联动"
            size="small"
            @change="handleLinkedChange"
          />
          <el-tooltip
            content="开启后勾选父节点会自动勾选所有子节点，关闭则可独立勾选任意节点"
            placement="top"
          >
            <el-icon style="margin-left: 4px; color: #909399; cursor: pointer">
              <QuestionFilled />
            </el-icon>
          </el-tooltip>
        </span>
        <el-button-group>
          <el-button size="small" plain @click="handleSelectAll">全选</el-button>
          <el-button size="small" plain @click="handleInvertSelect">反选</el-button>
        </el-button-group>
      </el-row>
      <el-tree
        :data="menuGrantList"
        show-checkbox
        :check-strictly="!menuLinked"
        node-key="id"
        ref="treeMenu"
        :default-checked-keys="menuTreeObj"
        :props="props"
      />
      <template #footer>
        <span class="dialog-footer">
          <el-button circle icon="el-icon-close" size="small" title="取消" @click="box = false" />
          <el-button icon="el-icon-check" size="small" circle title="保存" @click="submit" />
        </span>
      </template>
    </el-dialog>

    <page-attach ref="pageAttach" />
  </basic-container>
</template>

<script>
import { QuestionFilled } from '@element-plus/icons-vue';
import {
  getList,
  getDetail,
  add,
  update,
  remove,
  grant,
  grantTree,
  getTopTree,
  enable,
} from '@/api/system/topmenu';
import { mapGetters } from 'vuex';
import iconList from '@/config/iconList';
import pageAttach from '@/components/attach-dialog/page-attach/main.vue';

export default {
  components: {
    pageAttach,
    QuestionFilled,
  },
  data() {
    return {
      form: {},
      box: false,
      query: {},
      loading: true,
      timer: null,
      currentMenuIds: [],
      props: {
        label: 'title',
        value: 'key',
      },
      page: {
        pageSize: 30,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      menuGrantList: [],
      menuTreeObj: [],
      menuLinked: false,
      option: {
        searchShowBtn: false,
        dialogDrag: true,
        height: 'auto',
        calcHeight: 30,
        tip: false,
        searchShow: true,
        searchMenuSpan: 6,
        border: true,
        saveBtn: false,
        updateBtn: false,
        cancelBtn: false,
        menu: false,
        addBtn: false,
        delBtn: false,
        editBtn: false,
        searchBtn: false,
        emptyBtn: false,
        viewBtn: true,
        selection: true,
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
            label: '菜单名',
            prop: 'name',
            search: true,
            rules: [
              {
                required: true,
                message: '请输入菜单名',
                trigger: 'blur',
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
            label: '菜单排序',
            prop: 'sort',
            type: 'number',
            slot: true,
            rules: [
              {
                required: true,
                message: '请输入菜单排序',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '是否主页',
            prop: 'isMain',
            slot: true,
            align: 'center',
            width: 100,
            value: 0,
            display: false,
            dicData: [
              { label: '否', value: 0 },
              { label: '是', value: 1 },
            ],
          },
          {
            label: '菜单路由',
            prop: 'path',
            span: 24,
            hide: true,
            rules: [
              {
                required: false,
                message: '请输入菜单路由',
                trigger: 'blur',
              },
            ],
          },
        ],
      },
      data: [],
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.topmenu_add, false),
        viewBtn: this.validData(this.permission.topmenu_view, false),
        delBtn: this.validData(this.permission.topmenu_delete, false),
        editBtn: this.validData(this.permission.topmenu_edit, false),
      };
    },
    ids() {
      let ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
    idsArray() {
      let ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids;
    },
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
      this.form = { ...this.selectionList[0], id: '' };
      this.$refs.crud.dicInit();
    },
    editList(row, { property }) {
      if (this.permission.topmenu_edit && property === 'index') {
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
    getAllNodeKeys(nodes) {
      let keys = [];
      nodes.forEach(node => {
        keys.push(node.id);
        if (node.children && node.children.length > 0) {
          keys = keys.concat(this.getAllNodeKeys(node.children));
        }
      });
      return keys;
    },
    getLeafKeys(nodes) {
      let keys = [];
      nodes.forEach(node => {
        if (!node.children || node.children.length === 0) {
          keys.push(node.id);
        } else {
          keys = keys.concat(this.getLeafKeys(node.children));
        }
      });
      return keys;
    },
    handleSelectAll() {
      const tree = this.$refs.treeMenu;
      if (!tree) return;
      const allKeys = this.getAllNodeKeys(this.menuGrantList);
      tree.setCheckedKeys(allKeys);
    },
    handleInvertSelect() {
      const tree = this.$refs.treeMenu;
      if (!tree) return;
      const checkedKeys = new Set(tree.getCheckedKeys());
      if (this.menuLinked) {
        const leafKeys = this.getLeafKeys(this.menuGrantList);
        const invertedKeys = leafKeys.filter(key => !checkedKeys.has(key));
        tree.setCheckedKeys(invertedKeys);
      } else {
        const allKeys = this.getAllNodeKeys(this.menuGrantList);
        const invertedKeys = allKeys.filter(key => !checkedKeys.has(key));
        tree.setCheckedKeys(invertedKeys);
      }
    },
    handleLinkedChange() {
      const tree = this.$refs.treeMenu;
      if (!tree) return;
      const checkedKeys = tree.getCheckedKeys();
      const halfCheckedKeys = tree.getHalfCheckedKeys();
      this.$nextTick(() => {
        tree.setCheckedKeys([...checkedKeys, ...halfCheckedKeys]);
      });
    },
    isMainChange(row) {
      const newValue = row.isMain === 1 ? 0 : 1;
      const request = newValue === 1 ? enable(row.id) : update({ ...row, isMain: newValue });
      return request
        .then(() => {
          this.onLoad(this.page);
          this.$message({ type: 'success', message: '操作成功!' });
          return true;
        })
        .catch(error => {
          window.console.log(error);
          return false;
        });
    },
    submit() {
      const menuList = this.$refs.treeMenu.getCheckedKeys();
      grant(this.currentMenuIds, menuList).then(() => {
        this.box = false;
        this.$message({
          type: 'success',
          message: '操作成功!',
        });
        this.onLoad(this.page);
      });
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
      update(row).then(
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
    handleMenuSetting() {
      if (this.selectionList.length !== 1) {
        this.$message.warning('只能选择一条数据');
        return;
      }
      this.currentMenuIds = this.idsArray;
      this.menuTreeObj = [];
      grantTree().then(res => {
        this.menuGrantList = res.data.data.menu;
        getTopTree(this.ids).then(res => {
          this.menuTreeObj = res.data.data.menu;
          this.box = true;
        });
      });
    },
    handleDialogClose() {
      this.currentMenuIds = [];
      this.menuLinked = false;
    },
    beforeOpen(done, type) {
      if (['edit', 'view'].includes(type)) {
        getDetail(this.form.id).then(res => {
          this.form = res.data.data;
        });
      }
      done();
    },
    sortChange(row) {
      update(row).then(
        () => {
          this.onLoad(this.page);
        },
        error => {
          window.console.log(error);
        }
      );
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
    onLoad(page, params = {}) {
      this.loading = true;
      const data = Object.fromEntries(
        Object.entries(Object.assign(params, this.query)).filter(
          ([, value]) => value !== null && value !== undefined && value !== ''
        )
      );
      getList(page.currentPage, page.pageSize, data).then(res => {
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

<style>
.none-border {
  border: 0;
  background-color: transparent !important;
}
</style>
