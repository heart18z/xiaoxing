<template>
  <!-- 认证日志抽屉 -->
  <el-drawer :title="authLogTitle" append-to-body v-model="authLogBox" size="60%">
    <avue-crud
      :option="authLogOption"
      :table-loading="authLogLoading"
      :data="authLogData"
      ref="authLogCrud"
      v-model="authLogForm"
      v-model:page="authLogPage"
      v-model:search="authLogSearch"
      :before-open="authLogBeforeOpen"
      @search-change="authLogSearchChange"
      @search-reset="authLogSearchReset"
      @current-change="authLogCurrentChange"
      @size-change="authLogSizeChange"
      @refresh-change="authLogRefreshChange"
      @on-load="authLogOnLoad"
    >
      <template #menu="{ row }">
        <el-button
          text
          type="primary"
          icon="el-icon-view"
          @click.stop="$refs.authLogCrud.rowView(row)"
          >查看</el-button
        >
      </template>
      <template #account="{ row }">
        <el-tag size="small" effect="light">{{ row.account }}</el-tag>
      </template>
      <template #grantType="{ row }">
        <el-tag size="small" effect="light">{{ getGrantTypeName(row.grantType) }}</el-tag>
      </template>
      <template #remoteIp="{ row }">
        <el-tag size="small" effect="light">{{ row.remoteIp || '-' }}</el-tag>
      </template>
      <template #env="{ row }">
        <el-tag
          :type="row.env === 'prod' ? 'danger' : row.env === 'test' ? 'warning' : row.env === 'dev' ? 'success' : 'info'"
          size="small"
        >{{ row.env || '-' }}</el-tag>
      </template>
    </avue-crud>
  </el-drawer>
</template>

<script>
import { getAuthLogPage, getAuthLogDetail } from '@/api/system/authlog';
import { validatenull } from '@/utils/validate';
import { authLogOption, GRANT_TYPE_DIC } from '@/option/system/authlog';

export default {
  name: 'AuthLog',
  data() {
    return {
      authLogOption,
      authLogTitle: '认证日志',
      // 抽屉状态
      authLogBox: false,
      authLogLoading: false,
      authLogData: [],
      authLogForm: {},
      authLogSearch: {},
      authLogPage: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      // 用户ID过滤
      filterUserId: null,
    };
  },
  methods: {
    // ========== 对外暴露的入口方法 ==========

    openAuthLog() {
      this.filterUserId = null;
      this.authLogTitle = '认证日志';
      this.authLogSearch = {};
      this.authLogPage.currentPage = 1;
      this.authLogBox = true;
      this.$nextTick(() => {
        this.authLogOnLoad(this.authLogPage);
      });
    },
    openUserAuthLog(userId, userName) {
      this.filterUserId = userId;
      this.authLogTitle = userName ? `认证日志 - ${userName}` : '认证日志';
      this.authLogSearch = {};
      this.authLogPage.currentPage = 1;
      this.authLogBox = true;
      this.$nextTick(() => {
        this.authLogOnLoad(this.authLogPage);
      });
    },

    // ========== 授权类型转译 ==========

    getGrantTypeName(grantType) {
      const item = GRANT_TYPE_DIC.find(d => d.value === grantType);
      return item ? item.label : grantType || '-';
    },

    // ========== 认证日志列表 ==========

    authLogBeforeOpen(done, type) {
      if (type === 'view') {
        getAuthLogDetail(this.authLogForm.id).then(res => {
          this.authLogForm = res.data.data;
        });
      }
      done();
    },
    authLogSearchChange(params, done) {
      this.authLogSearch = params;
      this.authLogPage.currentPage = 1;
      this.authLogOnLoad(this.authLogPage, params);
      done();
    },
    authLogSearchReset() {
      this.authLogSearch = {};
      this.authLogPage.currentPage = 1;
      this.authLogOnLoad(this.authLogPage);
    },
    authLogSizeChange(pageSize) {
      this.authLogPage.pageSize = pageSize;
    },
    authLogCurrentChange(currentPage) {
      this.authLogPage.currentPage = currentPage;
    },
    authLogRefreshChange() {
      this.authLogOnLoad(this.authLogPage, this.authLogSearch);
    },
    authLogOnLoad(page, params = {}) {
      this.authLogLoading = true;
      const merged = Object.assign({}, params, this.authLogSearch);
      if (this.filterUserId) {
        merged.userId = this.filterUserId;
      }
      const query = Object.fromEntries(Object.entries(merged).filter(([, val]) => !validatenull(val)));
      getAuthLogPage(page.currentPage, page.pageSize, query)
        .then(res => {
          const data = res.data.data;
          this.authLogPage.total = data.total;
          this.authLogData = data.records;
          this.authLogLoading = false;
        })
        .catch(() => {
          this.authLogLoading = false;
        });
    },
  },
};
</script>
