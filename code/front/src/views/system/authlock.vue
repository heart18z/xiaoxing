<template>
  <!-- 账号锁定配置抽屉 -->
  <el-drawer title="账号锁定配置" append-to-body v-model="authLockBox" size="60%">
    <avue-crud
      :option="authLockOption"
      :table-loading="authLockLoading"
      :data="authLockData"
      ref="authLockCrud"
      v-model="authLockForm"
      v-model:page="authLockPage"
      v-model:search="authLockSearch"
      :before-open="authLockBeforeOpen"
      @search-change="authLockSearchChange"
      @search-reset="authLockSearchReset"
      @current-change="authLockCurrentChange"
      @size-change="authLockSizeChange"
      @refresh-change="authLockRefreshChange"
      @on-load="authLockOnLoad"
    >
      <template #lockTarget="{ row }">
        <el-tag type="primary" size="small" effect="light">{{ row.lockTarget }}</el-tag>
      </template>
      <template #userName="{ row }">
        {{ row.userName || '-' }}
      </template>
      <template #remoteIp="{ row }">
        <el-tag type="primary" size="small" effect="light">{{ row.remoteIp || '暂无' }}</el-tag>
      </template>
      <template #lockType="{ row }">
        <el-tag :type="row.lockType === 'SYSTEM' ? 'info' : 'danger'" size="small">
          {{ row.lockTypeName }}
        </el-tag>
      </template>
      <template #lockStatus="{ row }">
        <el-tag
          :type="row.expired ? 'info' : row.lockStatus === 'LOCKED' ? 'danger' : row.lockStatus === 'PRE_LOCKED' ? 'warning' : 'success'"
          size="small"
        >
          {{ row.lockStatusName }}
        </el-tag>
      </template>
      <template #lockBeginTime="{ row }">
        {{ row.lockBeginTime || '立即生效' }}
      </template>
      <template #lockEndTime="{ row }">
        {{ row.lockEndTime || '永久' }}
      </template>
      <template #menu="{ row }">
        <el-button
          text
          type="primary"
          icon="el-icon-view"
          @click.stop="$refs.authLockCrud.rowView(row)"
          >查看</el-button
        >
        <el-button
          v-if="(row.lockStatus === 'LOCKED' || row.lockStatus === 'PRE_LOCKED') && !row.expired"
          text
          type="primary"
          icon="el-icon-key"
          @click.stop="handleAuthLockUnlock(row)"
          >解锁</el-button
        >
      </template>
    </avue-crud>
  </el-drawer>

  <!-- 手动锁定用户 -->
  <el-dialog title="锁定用户" append-to-body v-model="lockUserBox" width="500px">
    <el-form :model="lockUserForm" label-width="100px" :rules="lockUserRules" ref="lockUserFormRef">
      <el-form-item label="用户账号">
        <el-input :model-value="lockUserForm.account" disabled />
      </el-form-item>
      <el-form-item label="锁定原因" prop="lockReason">
        <el-input
          v-model="lockUserForm.lockReason"
          type="textarea"
          :rows="3"
          placeholder="请输入锁定原因"
        />
      </el-form-item>
      <el-form-item label="锁定开始" prop="lockBeginTime">
        <el-date-picker
          v-model="lockUserForm.lockBeginTime"
          type="datetime"
          placeholder="不填则立即生效"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="锁定结束" prop="lockEndTime">
        <el-date-picker
          v-model="lockUserForm.lockEndTime"
          type="datetime"
          placeholder="不填则永久锁定"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="lockUserBox = false">取 消</el-button>
        <el-button type="primary" :loading="lockSubmitLoading" @click="submitLockUser">确 定</el-button>
      </span>
    </template>
  </el-dialog>

  <!-- 解锁原因确认 -->
  <el-dialog title="解锁确认" append-to-body v-model="unlockReasonBox" width="450px">
    <el-form :model="unlockReasonForm" label-width="80px">
      <el-form-item label="解锁原因">
        <el-input
          v-model="unlockReasonForm.unlockReason"
          type="textarea"
          :rows="3"
          placeholder="请输入解锁原因（可选）"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="unlockReasonBox = false">取 消</el-button>
        <el-button type="primary" :loading="unlockSubmitLoading" @click="submitUnlockReason">确 定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
import {
  getAuthLockPage,
  getAuthLockDetail,
  authLockUnlock,
  lockUser,
} from '@/api/system/authlock';
import { validatenull } from '@/utils/validate';
import { authLockOption } from '@/option/system/authlock';

export default {
  name: 'AuthLock',
  emits: ['lock-success'],
  data() {
    return {
      authLockOption,
      // 锁定日志抽屉
      authLockBox: false,
      authLockLoading: false,
      authLockData: [],
      authLockForm: {},
      authLockSearch: {},
      authLockPage: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      // 手动锁定用户
      lockUserBox: false,
      lockUserForm: {},
      lockUserRules: {
        lockReason: [{ required: true, message: '请输入锁定原因', trigger: 'blur' }],
        lockEndTime: [
          {
            validator: (rule, value, callback) => {
              if (!value) {
                callback();
              } else if (this.lockUserForm.lockBeginTime && value < this.lockUserForm.lockBeginTime) {
                callback(new Error('结束时间不能早于开始时间'));
              } else if (!this.lockUserForm.lockBeginTime && value < new Date().toISOString().slice(0, 19).replace('T', ' ')) {
                callback(new Error('结束时间不能早于当前时间'));
              } else {
                callback();
              }
            },
            trigger: 'change',
          },
        ],
      },
      // 解锁确认
      unlockReasonBox: false,
      unlockReasonForm: {},
      currentUnlockId: null,
      lockSubmitLoading: false,
      unlockSubmitLoading: false,
    };
  },
  methods: {
    // ========== 对外暴露的入口方法 ==========

    openLockLog() {
      this.authLockSearch = {};
      this.authLockPage.currentPage = 1;
      this.authLockBox = true;
      this.$nextTick(() => {
        this.authLockOnLoad(this.authLockPage);
      });
    },
    openLockUser(userId, account) {
      this.lockUserForm = {
        userId,
        account,
        lockReason: '',
        lockBeginTime: '',
        lockEndTime: '',
      };
      this.lockUserBox = true;
    },

    // ========== 锁定日志列表 ==========

    authLockBeforeOpen(done, type) {
      if (type === 'view') {
        getAuthLockDetail(this.authLockForm.id).then(res => {
          this.authLockForm = res.data.data;
        });
      }
      done();
    },
    authLockSearchChange(params, done) {
      this.authLockSearch = params;
      this.authLockPage.currentPage = 1;
      this.authLockOnLoad(this.authLockPage, params);
      done();
    },
    authLockSearchReset() {
      this.authLockSearch = {};
      this.authLockPage.currentPage = 1;
      this.authLockOnLoad(this.authLockPage);
    },
    authLockSizeChange(pageSize) {
      this.authLockPage.pageSize = pageSize;
    },
    authLockCurrentChange(currentPage) {
      this.authLockPage.currentPage = currentPage;
    },
    authLockRefreshChange() {
      this.authLockOnLoad(this.authLockPage, this.authLockSearch);
    },
    authLockOnLoad(page, params = {}) {
      this.authLockLoading = true;
      const merged = Object.assign({}, params, this.authLockSearch);
      const query = Object.fromEntries(Object.entries(merged).filter(([, val]) => !validatenull(val)));
      getAuthLockPage(page.currentPage, page.pageSize, query)
        .then(res => {
          const data = res.data.data;
          this.authLockPage.total = data.total;
          this.authLockData = data.records;
          this.authLockLoading = false;
        })
        .catch(() => {
          this.authLockLoading = false;
        });
    },

    // ========== 解锁操作 ==========

    handleAuthLockUnlock(row) {
      this.currentUnlockId = row.id;
      this.unlockReasonForm = { unlockReason: '' };
      this.unlockReasonBox = true;
    },
    submitUnlockReason() {
      this.unlockSubmitLoading = true;
      authLockUnlock(this.currentUnlockId, this.unlockReasonForm.unlockReason)
        .then(() => {
          this.unlockSubmitLoading = false;
          this.unlockReasonBox = false;
          this.$message({ type: 'success', message: '解锁成功!' });
          this.authLockOnLoad(this.authLockPage, this.authLockSearch);
        })
        .catch(() => {
          this.unlockSubmitLoading = false;
          this.$message({ type: 'error', message: '解锁失败，请重试!' });
        });
    },

    // ========== 手动锁定用户 ==========

    submitLockUser() {
      this.$refs.lockUserFormRef.validate(valid => {
        if (!valid) return;
        this.lockSubmitLoading = true;
        lockUser(
          this.lockUserForm.userId,
          this.lockUserForm.lockReason,
          this.lockUserForm.lockBeginTime,
          this.lockUserForm.lockEndTime
        )
          .then(() => {
            this.lockSubmitLoading = false;
            this.lockUserBox = false;
            this.$message({ type: 'success', message: '锁定成功!' });
            this.$emit('lock-success');
          })
          .catch(() => {
            this.lockSubmitLoading = false;
            this.$message({ type: 'error', message: '锁定失败，请重试!' });
          });
      });
    },
  },
};
</script>
