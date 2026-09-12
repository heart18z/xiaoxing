<template>
  <div>
    <el-dialog title="授权登录状态选择" append-to-body v-model="box" width="30%">
      <el-form>
        <el-form-item>
          <template #label>
            <span>单点登录集成状态</span>
          </template>
          <el-tooltip
            class="item"
            effect="dark"
            :content="
              '修改人：' +
              oauth2Setting.updateUserName +
              '；更新时间：' +
              oauth2Setting.updateTime +
              '。'
            "
            placement="top"
          >
            <i class="el-icon-question" />
          </el-tooltip>

          <el-select
            style="margin-left: 10px"
            :disabled="oauth2SettingDisabled"
            v-model="oauth2Setting.paramValue"
            placeholder="请选择"
          >
            <el-option
              v-for="item in options"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <el-row>
        <el-col :span="24">
          <el-button
            style="float: right"
            icon="el-icon-check"
            size="small"
            circle
            @click="saveOauth2Setting"
            title="保存"
          />
        </el-col>
      </el-row>
    </el-dialog>
    <span class="top-bar__icon" @click="open">
      <el-icon :size="18"><Guide /></el-icon>
    </span>
  </div>
</template>

<script>
import { detail, update } from '@/api/system/param';
import func from '@/utils/func';

export default {
  data() {
    return {
      oauth2SettingDisabled: false,
      oauth2Setting: {},
      box: false,
      oldSetting: '',
      options: [
        {
          label: '尚未配置',
          value: 'unset',
        },
        {
          label: '尚未启用',
          value: 'disable',
        },
        {
          label: '已经启用',
          value: 'enable',
        },
      ],
    };
  },
  methods: {
    saveOauth2Setting() {
      if (this.oldSetting === this.oauth2Setting.paramValue) {
        this.$message.warning('请修改配置后保存');
        return;
      }
      let text;
      if (this.oauth2Setting.paramValue === 'unset') {
        text =
          '如果本应用的代码已经配置了与授权系统的接口调用关系，' +
          '您不能设置为尚未配置状态，如果确认本应用代码沒有配置授权系统的接口调用关系，' +
          '当设置为“尚未配置”保存成功后，在个人信息页面上，不会出现登录设置的子页签，是否确定继续。';
      } else if (this.oauth2Setting.paramValue === 'disable') {
        text =
          '如果本应用的代码已经配置了与授权系统的接口调用关系，' +
          '当设置为“尚未启用”状态后，在“个人信息”页面的“登录设置”子页签中，会禁用所有个人账户的登录设置' +
          '功能。';
      } else if (this.oauth2Setting.paramValue === 'enable') {
        text =
          '一旦设置为“已经启用”，将不能再次便改为其他类型的“授权登录状态”，' +
          '用户可以自由配置自己的授权账户和登录方式。';
      }

      this.$confirm(text, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        this.updateForm();
      });
    },
    updateForm() {
      update(this.oauth2Setting).then(
        () => {
          this.box = false;
          this.$message.success('保存成功');
          window.location.reload();
        },
        error => {
          window.console.log(error);
        }
      );
    },
    open() {
      this.box = true;
      this.oauth2Setting = {};
      this.oldSetting = '';
      detail({
        paramKey: 'oauth2',
      }).then(res => {
        this.oauth2Setting = res.data.data;
        this.oldSetting = this.oauth2Setting.paramValue;
        let status = this.oauth2Setting.paramValue || '';
        status = func.trimAll(status);

        if (func.isEmpty(status)) {
          this.oauth2SettingDisabled = false;
          this.options = [
            {
              label: '尚未配置',
              value: 'unset',
            },
          ];
        } else if (status === 'unset') {
          this.oauth2SettingDisabled = false;
          this.options = [
            {
              label: '尚未启用',
              value: 'disable',
            },
          ];
        } else if (status === 'disable') {
          this.oauth2SettingDisabled = false;
          this.options = [
            {
              label: '已经启用',
              value: 'enable',
            },
          ];
        } else if (status === 'enable') {
          this.oauth2SettingDisabled = true;
          this.options = [
            {
              label: '已经启用',
              value: 'enable',
            },
          ];
        }
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.list {
  width: 100%;
}

.top-bar__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  vertical-align: middle;
}
</style>
