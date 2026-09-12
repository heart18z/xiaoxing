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
      <!-- 左边按钮 -->
      <template #menu-left>
        <el-button
          class="button-size"
          round
          title="新增"
          v-if="permission.post_add"
          size="small"
          icon="el-icon-plus"
          @click.stop="addList()"
        />
        <el-button
          class="button-size"
          round
          title="删除"
          v-if="permission.post_delete"
          size="small"
          icon="el-icon-delete"
          @click.stop="handleDelete()"
        />
        <el-button
          class="button-size"
          round
          title="复制"
          v-if="permission.post_add"
          size="small"
          icon="el-icon-document-copy"
          @click.stop="copyList()"
        />
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

      <template #index="{ index }">
        <div>{{ index + 1 }}</div>
      </template>
    </avue-crud>
    <!-- 左下按钮 -->
    <page-attach ref="pageAttach" />
  </basic-container>
</template>

<script>
import { getList, getDetail, add, update, remove } from '@/api/system/post';
import { mapGetters } from 'vuex';
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
      page: {
        pageSize: 30,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      timer: null,
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
            label: '岗位类型',
            prop: 'category',
            type: 'select',
            dicMethod: 'post',
            dicUrl: '/api/blade-system/dict/dictionary?code=post_category',
            props: {
              label: 'dictValue',
              value: 'dictKey',
            },
            dataType: 'number',
            slot: true,
            search: true,
            rules: [
              {
                required: true,
                message: '请选择岗位类型',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '岗位编号',
            prop: 'postCode',
            search: true,
            rules: [
              {
                required: true,
                message: '请输入岗位编号',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '岗位名称',
            prop: 'postName',
            search: true,
            rules: [
              {
                required: true,
                message: '请输入岗位名称',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '岗位排序',
            prop: 'sort',
            type: 'number',
            rules: [
              {
                required: true,
                message: '请输入岗位排序',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '岗位描述',
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
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.post_add, false),
        viewBtn: this.validData(this.permission.post_view, false),
        delBtn: this.validData(this.permission.post_delete, false),
        editBtn: this.validData(this.permission.post_edit, false),
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
      if (this.permission.post_edit && property === 'index') {
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


