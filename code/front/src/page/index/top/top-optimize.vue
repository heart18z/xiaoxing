<template>
  <div>
    <el-dialog
      title="优化提交"
      v-model="showDialog"
      draggable
      :append-to-body="true"
      :modal="false"
      width="70%"
    >
      <avue-crud
        :option="option"
        v-model:search="query"
        :table-loading="loading"
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
      >
        <template #menu-left>
          <el-button
            class="button-size"
            round
            title="新增"
            size="small"
            icon="el-icon-plus"
            @click.stop="addList()"
          />
          <el-button
            class="button-size"
            round
            title="删除"
            size="small"
            icon="el-icon-delete"
            @click.stop="handleDelete()"
          />
          <el-button
            class="button-size"
            round
            title="复制"
            size="small"
            icon="el-icon-document-copy"
            @click.stop="copyList()"
          />
        </template>

        <template #uploadList="{ row }">
          <row-attach :source-id="row.id" />
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
      </avue-crud>
    </el-dialog>
    <span class="top-bar__icon" @click="open">
      <el-icon :size="18"><Cpu /></el-icon>
    </span>
  </div>
</template>

<script>
import rowAttach from '@/components/attach-dialog/row-attach/main.vue';
import { getList, getDetail, add, update, remove } from '@/api/system/optimize';

export default {
  components: {
    rowAttach,
  },
  data() {
    return {
      showDialog: false,
      form: {},
      query: {},
      search: {},
      loading: true,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      option: {
        searchShowBtn: false,
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
        height: 'auto',
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
            label: '序号',
            prop: 'index',
            fixed: true,
            width: 70,
            display: false,
            align: 'center',
          },
          {
            label: '编号',
            prop: 'id',
            type: 'input',
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
          },
          {
            label: '修改人',
            prop: 'updateUser',
            type: 'input',
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
          },
          {
            label: '修改时间',
            prop: 'updateTime',
            type: 'input',
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
          },
          {
            label: '状态',
            prop: 'status',
            type: 'input',
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
          },
          {
            label: '是否已删除',
            prop: 'isDeleted',
            type: 'input',
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
          },
          {
            label: '租户ID',
            prop: 'tenantId',
            type: 'input',
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
          },
          {
            label: '优化标题',
            prop: 'title',
            type: 'input',
          },
          {
            label: '处理状态',
            prop: 'handleStatus',
            type: 'input',
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
          },
          {
            label: '依赖代码编号',
            prop: 'sourceTag',
            labelWidth: 110,
            type: 'input',
          },
          {
            label: '创建人',
            prop: 'createUser',
            type: 'select',
            dicMethod: 'post',
            dicUrl: '/api/blade-system/user/user-kv-list',
            width: '100',
            dicFormatter: res => res.data,
            props: {
              label: 'key',
              value: 'value',
            },
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: false,
          },
          {
            label: '创建部门',
            prop: 'createDept',
            type: 'input',
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: true,
          },
          {
            label: '创建时间',
            prop: 'createTime',
            type: 'input',
            addDisplay: false,
            editDisplay: false,
            viewDisplay: false,
            hide: false,
          },
          {
            label: '附件列表',
            span: 24,
            width: '100',
            prop: 'uploadList',
            slot: true,
            display: false,
          },
        ],
      },
      data: [],
      timer: null,
    };
  },
  computed: {
    ids() {
      const ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
  },
  methods: {
    open() {
      this.showDialog = true;
      this.page.currentPage = 1;
      this.$nextTick(() => {
        this.onLoad(this.page);
      });
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
    editList(row, { property }) {
      if (property === 'index') {
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
          loading();
          window.console.log(error);
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
          loading();
          console.log(error);
        }
      );
    },
    rowDel(row) {
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => remove(row.id))
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
        .then(() => remove(this.ids))
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
        });
      }
      done();
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
    onLoad(page) {
      this.loading = true;
      getList(page.currentPage, page.pageSize, {}).then(res => {
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

<style scoped>
.top-bar__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  vertical-align: middle;
}
</style>
