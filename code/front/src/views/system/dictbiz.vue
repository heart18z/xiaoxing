<template>
  <basic-container>
    <avue-crud
      :option="optionParent"
      v-model:search="query"
      :table-loading="loading"
      :data="dataParent"
      :page="pageParent"
      ref="crud"
      v-model="formParent"
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
      @on-load="onLoadParent"
      @cell-click="rowView"
      @cell-dblclick="handleDbClick"
    >
      <!-- 左边按钮 -->
      <template #menu-left>
        <el-button
          class="button-size"
          round
          title="新增"
          v-if="permission.dictbiz_add"
          size="small"
          icon="el-icon-plus"
          @click.stop="addParentList()"
        />
        <el-button
          class="button-size"
          round
          title="删除"
          v-if="permission.dictbiz_delete"
          size="small"
          icon="el-icon-delete"
          @click.stop="handleDelete()"
        />
        <el-button
          class="button-size"
          round
          title="复制"
          v-if="permission.dictbiz_add"
          size="small"
          icon="el-icon-document-copy"
          @click.stop="copyParentList()"
        />
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

      <template #code="{ row }">
        <el-tag @click="handleOneRowClick(row)" style="cursor: pointer">{{ row.code }}</el-tag>
      </template>
      <template #isSealed="{ row }">
        <el-tag>{{ row.isSealed === 0 ? '否' : '是' }}</el-tag>
      </template>
    </avue-crud>
    <!-- 左下按钮 -->
    <page-attach ref="pageAttach" />
    <el-dialog
      :title="`[${dictValue}]字典配置`"
      draggable
      append-to-body
      v-model="box"
      width="1000px"
    >
      <avue-crud
        :option="optionChild"
        v-model:search="queryChild"
        :table-loading="loadingChild"
        :data="dataChild"
        ref="crudChild"
        v-model="formChild"
        :permission="permissionList"
        :before-open="beforeOpenChild"
        :before-close="beforeCloseChild"
        @row-del="rowDelChild"
        @row-update="rowUpdateChild"
        @row-save="rowSaveChild"
        @search-change="searchChangeChild"
        @search-reset="searchResetChild"
        @selection-change="selectionChangeChild"
        @current-change="currentChangeChild"
        @size-change="sizeChangeChild"
        @refresh-change="refreshChangeChild"
        @on-load="onLoadChild"
        @cell-click="rowViewChild"
        @cell-dblclick="editChildClick"
      >
        <!-- 左边按钮 -->
        <template #menu-left>
          <el-button
            class="button-size"
            round
            title="新增"
            v-if="permission.dictbiz_add"
            size="small"
            icon="el-icon-plus"
            @click.stop="addChildList()"
          />
          <el-button
            class="button-size"
            round
            title="删除"
            v-if="permission.dictbiz_delete"
            size="small"
            icon="el-icon-delete"
            @click.stop="handleDeleteChild()"
          />
          <el-button
            class="button-size"
            round
            title="复制"
            v-if="permission.dictbiz_add"
            size="small"
            icon="el-icon-document-copy"
            @click.stop="copyChildList()"
          />
        </template>

        <template #no="{ row }">
          <span>{{ row.no }}</span>
        </template>

        <template #isSealed="{ row }">
          <el-tag>{{ row.isSealed === 0 ? '否' : '是' }}</el-tag>
        </template>

        <template #menu-form="{ type }">
          <template v-if="type !== 'view'">
            <el-button
              icon="el-icon-check"
              size="small"
              circle
              @click="$refs.crudChild.rowSave()"
              title="保存"
            />
            <el-button
              circle
              icon="el-icon-close"
              size="small"
              title="取消"
              @click="$refs.crudChild.closeDialog()"
            />
          </template>
        </template>
      </avue-crud>
    </el-dialog>
  </basic-container>
</template>

<script>
import {
  getParentList,
  getChildList,
  remove,
  update,
  add,
  getDict,
  getDictTree,
} from '@/api/system/dictbiz';
import { optionParent, optionChild } from '@/option/system/dictbiz';
import { mapGetters } from 'vuex';
import pageAttach from '@/components/attach-dialog/page-attach/main.vue';

export default {
  components: {
    pageAttach,
  },
  data() {
    return {
      dictValue: '暂无',
      parentId: -1,
      parentCode: '',
      formParent: {},
      formChild: {},
      selectionList: [],
      selectionChildList: [],
      query: {},
      queryChild: {},
      box: false,
      loading: true,
      loadingChild: true,
      timer: null,
      pageParent: {
        pageSize: 30,
        pageSizes: [10, 30, 50, 100, 200],
        currentPage: 1,
        total: 0,
      },
      pageChild: {
        pageSize: 30,
        pageSizes: [10, 30, 50, 100, 200],
        currentPage: 1,
        total: 0,
      },
      dataParent: [],
      dataChild: [],
      optionParent: optionParent,
      optionChild: optionChild,
    };
  },
  computed: {
    ...mapGetters(['userInfo', 'permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.dictbiz_add, false),
        delBtn: this.validData(this.permission.dictbiz_delete, false),
        editBtn: this.validData(this.permission.dictbiz_edit, false),
        viewBtn: false,
      };
    },
    ids() {
      let ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
    childIds() {
      let ids = [];
      this.selectionChildList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
  },
  mounted() {
    this.initData();
  },
  methods: {
    rowView(row, { property }) {
      if (property === 'index') {
        clearTimeout(this.timer);
        this.timer = setTimeout(() => {
          this.$refs.crud.rowView(row, row.$index);
        }, 250);
      }
    },
    handleDbClick(row, { property }) {
      if (property === 'index') {
        clearTimeout(this.timer);
        if (this.permission.dictbiz_edit) {
          this.$refs.crud.rowEdit(row, row.$index);
        }
      }
    },
    addParentList() {
      if (this.selectionList.length === 0) {
        this.$refs.crud.rowAdd();
        this.parentId = -1;
        this.dictValue = '';
        this.parentCode = '';
        this.formParent = {};
        this.$refs.crud.dicInit();
      } else {
        this.parentSelectionAdd();
      }
    },
    handleOneRowClick(row) {
      this.query = {};
      this.formChild = {};
      this.parentId = row.id;
      this.parentCode = row.code;
      this.dictValue = row.dictValue;
      this.box = true;
      this.onLoadChild(this.pageChild);
    },
    parentSelectionAdd() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      let row = this.selectionList[0];
      this.query = {};
      this.formChild = {};
      this.parentId = row.id;
      this.parentCode = row.code;
      this.dictValue = row.dictValue;
      this.box = true;
      this.onLoadChild(this.pageChild);
    },
    addChildList() {
      if (this.selectionChildList.length === 0) {
        this.formChild = {};
        this.formChild.code = this.parentCode;
        this.formChild.parentId = this.parentId;
        this.$refs.crudChild.dicInit();
        this.$refs.crudChild.rowAdd();
      } else {
        this.childSelectionAdd();
      }
    },
    childSelectionAdd() {
      if (this.selectionChildList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.formChild = {};
      let row = this.selectionChildList[0];
      this.formChild.parentId = row.id;
      this.formChild.code = this.parentCode;
      this.$refs.crudChild.rowAdd();
    },
    copyParentList() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$refs.crud.rowAdd();
      this.formParent = this.selectionList[0];
      this.formParent.id = '';
      this.$refs.crud.dicInit();
    },
    initData() {
      getDictTree().then(res => {
        const column = this.findColumn(this.optionChild.column, 'parentId');
        column.dicData = res.data.data;
      });
    },
    rowSave(row, done, loading) {
      const form = {
        ...row,
        dictKey: -1,
      };
      add(form).then(
        () => {
          this.onLoadParent(this.pageParent);
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
          this.onLoadParent(this.pageParent);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.onLoadChild(this.pageChild);
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
          this.onLoadParent(this.pageParent);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
        });
    },
    searchReset() {
      this.query = {};
      this.onLoadParent(this.pageParent);
    },
    searchChange(params, done) {
      this.query = params;
      this.pageParent.currentPage = 1;
      this.onLoadParent(this.pageParent, params);
      done();
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    selectionClear() {
      this.selectionList = [];
      this.$refs.crud.toggleSelection();
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
          this.onLoadParent(this.pageParent);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.$refs.crud.toggleSelection();
        });
    },
    beforeOpen(done, type) {
      if (['edit', 'view'].includes(type)) {
        getDict(this.formParent.id).then(res => {
          this.formParent = res.data.data;
        });
      }
      done();
    },
    currentChange(currentPage) {
      this.pageParent.currentPage = currentPage;
    },
    sizeChange(pageSize) {
      this.pageParent.pageSize = pageSize;
    },
    refreshChange() {
      this.onLoadParent(this.pageParent, this.query);
    },
    copyChildList() {
      if (this.selectionChildList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$refs.crudChild.rowAdd();
      this.formChild = this.selectionChildList[0];
      this.formChild.id = '';
      this.$refs.crudChild.dicInit();
    },
    rowViewChild(row, { property }) {
      if (property === 'no') {
        clearTimeout(this.timer);
        this.timer = setTimeout(() => {
          this.$refs.crudChild.rowView(row, row.$index);
        }, 250);
      }
    },
    editChildClick(row, { property }) {
      if (property === 'no') {
        clearTimeout(this.timer);
        if (this.permission.dictbiz_edit) {
          const tmpRow = { ...row };
          this.$refs.crudChild.rowEdit(tmpRow);
        }
      }
    },
    rowSaveChild(row, done, loading) {
      add(row).then(
        () => {
          this.onLoadChild(this.pageChild);
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
    rowUpdateChild(row, index, done, loading) {
      update(row).then(
        () => {
          this.onLoadChild(this.pageChild);
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
    rowDelChild(row) {
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return remove(row.id);
        })
        .then(() => {
          this.onLoadChild(this.pageChild);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
        });
    },
    searchResetChild() {
      this.query = {};
      this.onLoadChild(this.pageChild);
    },
    searchChangeChild(params, done) {
      this.query = params;
      this.pageChild.currentPage = 1;
      this.onLoadChild(this.pageChild, params);
      done();
    },
    selectionChangeChild(list) {
      this.selectionChildList = list;
    },
    selectionClearChild() {
      this.selectionChildList = [];
      this.$refs.crudChild.toggleSelection();
    },
    handleDeleteChild() {
      if (this.selectionChildList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return remove(this.childIds);
        })
        .then(() => {
          this.onLoadChild(this.pageChild);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.$refs.crudChild.toggleSelection();
        });
    },
    beforeOpenChild(done, type) {
      if (['add', 'edit'].includes(type)) {
        this.initData();
      }
      if (['edit', 'view'].includes(type)) {
        getDict(this.formChild.id).then(res => {
          this.formChild = res.data.data;
        });
      }
      done();
    },
    beforeCloseChild(done) {
      this.$refs.crudChild.modelValue.parentId = this.parentId;
      this.$refs.crudChild.option.column.filter(item => {
        if (item.prop === 'parentId') {
          item.value = this.parentId;
        }
      });
      done();
    },
    currentChangeChild(currentPage) {
      this.pageChild.currentPage = currentPage;
    },
    sizeChangeChild(pageSize) {
      this.pageChild.pageSize = pageSize;
    },
    refreshChangeChild() {
      this.onLoadChild(this.pageChild, this.queryChild);
    },
    onLoadParent(page, params = {}) {
      this.loading = true;
      getParentList(page.currentPage, page.pageSize, Object.assign(params, this.query)).then(
        res => {
          const data = res.data.data;
          this.pageParent.total = data.total;
          this.dataParent = data.records;
          this.loading = false;
          this.selectionClear();
        }
      );
    },
    onLoadChild(page, params = {}) {
      this.loadingChild = true;
      getChildList(
        page.currentPage,
        page.pageSize,
        this.parentId,
        Object.assign(params, this.queryChild)
      ).then(res => {
        this.dataChild = res.data.data;
        this.loadingChild = false;
        this.selectionClear();
        const tf = data => {
          let i = 1;
          for (let child of data) {
            child.no = i++;
            if (child.children && child.children.length) {
              tf(child.children);
            }
          }
        };
        tf(this.dataChild);
      });
    },
  },
};
</script>


