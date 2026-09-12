<template>
  <div>
    <el-row>
      <el-col :span="24">
        <third-register></third-register>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="9">
        <basic-container>
          <el-row class="row-align-center">
            {{ indexDisplayName.tag }}
            <el-divider direction="vertical" />
            <span>
              <a class="row-link" target="_blank" rel="noopener noreferrer">
                {{ indexDisplay.tag }}
              </a>
            </span>
          </el-row>
          <el-divider content-position="right"></el-divider>

          <el-row class="row-align-center">
            {{ indexDisplayName.codeBootUrl }}
            <el-divider direction="vertical" />
            <span>
              <a
                :href="indexDisplay.codeBootUrl"
                class="row-link"
                target="_blank"
                rel="noopener noreferrer"
              >
                {{ indexDisplay.codeBootUrl }}
              </a>
            </span>
          </el-row>
          <el-divider content-position="right"></el-divider>

          <el-row class="row-align-center">
            {{ indexDisplayName.codeTag }}
            <el-divider direction="vertical" />
            <a
              target="_blank"
              :href="indexDisplay.codeBootUrl"
              v-if="func.notEmpty(indexDisplay.codeTag)"
              class="row-align-center"
            >
              <span>{{ indexDisplay.codeTag }}</span>
            </a>
          </el-row>
          <el-divider content-position="right"></el-divider>

          <el-row class="row-align-center">
            {{ indexDisplayName.currCodeVersion }}
            <el-divider direction="vertical" />
            <a
              target="_blank"
              :href="indexDisplay.codeBootUrl"
              v-if="func.notEmpty(indexDisplay.currCodeVersion)"
              class="row-align-center"
            >
              <span>{{ indexDisplay.currCodeVersion }}</span>
            </a>
          </el-row>

          <el-divider content-position="right"></el-divider>
          <el-row class="row-align-center">
            {{ indexDisplayName.testAddress }}
            <el-divider direction="vertical" />
            <span>
              <a
                :href="indexDisplay.testAddress"
                class="row-link"
                target="_blank"
                rel="noopener noreferrer"
              >
                {{ indexDisplay.testAddress }}
              </a>
            </span>
          </el-row>
        </basic-container>
      </el-col>
      <el-col :span="15">
        <basic-container>
          <el-row>
            <el-col style="margin-bottom: 0" :span="8" class="row-padding-left">
              <div>
                <el-tooltip
                  class="item bottom-left-tips"
                  effect="dark"
                  content="以上内容只展示主要身份"
                  placement="top"
                >
                  <el-icon><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
              <el-row>
                <el-tag type="info" effect="plain" size="small">功能需求</el-tag>
              </el-row>
              <el-divider content-position="right"></el-divider>
              <el-row>姓名 {{ indexDisplay.funcPeople.realName }}</el-row>
              <el-divider content-position="right"></el-divider>
              <el-row>电话 {{ indexDisplay.funcPeople.phone }}</el-row>
              <el-divider content-position="right"></el-divider>
              <el-row>微信 {{ indexDisplay.funcPeople.wechat }}</el-row>
              <el-divider content-position="right"></el-divider>
              <el-row>邮箱 {{ indexDisplay.funcPeople.email }}</el-row>
            </el-col>
            <el-col
              style="margin-bottom: 0"
              :span="8"
              class="row-padding-left"
              v-if="func.notEmpty(indexDisplay.tecPeople)"
            >
              <el-row>
                <el-tag type="success" size="small" effect="plain">技术支持</el-tag>
              </el-row>
              <el-divider content-position="right"></el-divider>
              <el-row>姓名 {{ indexDisplay.tecPeople.realName }}</el-row>
              <el-divider content-position="right"></el-divider>
              <el-row>电话 {{ indexDisplay.tecPeople.phone }}</el-row>
              <el-divider content-position="right"></el-divider>
              <el-row>微信 {{ indexDisplay.tecPeople.wechat }}</el-row>
              <el-divider content-position="right"></el-divider>
              <el-row>邮箱 {{ indexDisplay.tecPeople.email }}</el-row>
            </el-col>
            <el-col
              style="margin-bottom: 0"
              :span="8"
              class="row-padding-left"
              v-if="func.notEmpty(indexDisplay.adminPeople)"
            >
              <el-row>
                <el-tag type="warning" effect="plain" size="small">系统管理人</el-tag>
              </el-row>
              <el-divider content-position="right"></el-divider>
              <el-row>姓名 {{ indexDisplay.adminPeople.realName }}</el-row>
              <el-divider content-position="right"></el-divider>
              <el-row>电话 {{ indexDisplay.adminPeople.phone }}</el-row>
              <el-divider content-position="right"></el-divider>
              <el-row>微信 {{ indexDisplay.adminPeople.wechat }}</el-row>
              <el-divider content-position="right"></el-divider>
              <el-row>邮箱 {{ indexDisplay.adminPeople.email }}</el-row>
            </el-col>
          </el-row>
        </basic-container>
      </el-col>
    </el-row>

    <el-dialog
      :title="tips"
      append-to-body
      v-model="resetPasswordVisible"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      width="450px"
    >
      <avue-form :option="option" v-model="form" @submit="handleSubmit">
        <template #menu-form>
          <el-button type="warning" icon="el-icon-check" @click="logout">退出登录</el-button>
        </template>
      </avue-form>
    </el-dialog>
    <userInfo ref="userinfo"></userInfo>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import { QuestionFilled } from '@element-plus/icons-vue';
import { verifySetPassword } from '@/api/user';
import { updatePassword, getUserContactList } from '@/api/system/user';
import md5 from 'js-md5';
import { resetRouter } from '@/router/index';
import func from '@/utils/func';
import { getValueByKey } from '@/api/system/param';

export default {
  name: 'wel',
  components: { QuestionFilled },
  created() {
    verifySetPassword().then(res => {
      if (res.status === 200) {
        const data = res.data;
        if (data.isUserResetPassword === 'true') {
          this.tips = '当前密码为初始密码，请重置密码！';
          this.$message.warning(this.tips);
          this.resetPasswordVisible = true;
        } else if (data.isPasswordExpireTime === 'true') {
          this.tips = '当前密码已过期请重新设置密码！';
          this.$message.warning(this.tips);
          this.resetPasswordVisible = true;
        }
      }
    });
    getValueByKey('account.initPassword').then(res => {
      if (func.notEmpty(res.data.data)) {
        this.initPassword = res.data.data;
      }
    });
  },
  data() {
    const validatePass = (rule, value, callback) => {
      const ISPWD = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[\W_]).{8,20}$/;
      if (value === '') {
        callback(new Error('请输入密码'));
      } else if (ISPWD.test(value)) {
        callback();
      } else {
        callback(new Error('密码必须8-20位字符,同时含有字母、特殊字符和数字组合'));
      }
    };
    const validateNewPass = (rule, value, callback) => {
      if (value !== '') {
        if (this.form.newPassword !== this.form.newPassword1) {
          callback(new Error('两次输入的密码不一致'));
        }
      }
      callback();
    };
    return {
      tips: '',
      initPassword: '',
      func,
      indexDisplay: {
        currCodeVersion: '',
        codeTag: '',
        testAddress: '',
        codeBootUrl: '',
        funcPeople: {},
        tecPeople: {},
        adminPeople: {},
        tag: '',
      },
      indexDisplayName: {
        currCodeVersion: '',
        codeTag: '',
        testAddress: '',
        codeBootUrl: '',
        tag: '',
      },
      form: {},
      option: {
        prop: 'password',
        menuBtn: true,
        column: [
          {
            label: '原密码',
            span: 24,
            row: true,
            type: 'password',
            prop: 'oldPassword',
            rules: [{ validator: validatePass, trigger: 'blur' }],
          },
          {
            label: '新密码',
            span: 24,
            row: true,
            type: 'password',
            prop: 'newPassword',
            rules: [{ validator: validatePass, trigger: 'blur' }],
          },
          {
            label: '确认密码',
            span: 24,
            row: true,
            type: 'password',
            prop: 'newPassword1',
            rules: [
              { validator: validatePass, trigger: 'blur' },
              { validator: validateNewPass, trigger: 'blur' },
            ],
          },
        ],
      },
      resetPasswordVisible: false,
    };
  },
  computed: {
    ...mapGetters(['userInfo']),
    mapKeyName() {
      return this.$store.getters.systemParam['mapKeyName'];
    },
  },
  watch: {
    mapKeyName() {
      if (func.notEmpty(this.mapKeyName)) {
        this.getIndexData();
      }
    },
  },
  mounted() {
    if (func.notEmpty(this.mapKeyName)) {
      this.getIndexData();
    }
  },
  methods: {
    showDictValue() {
      this.indexDisplay = {
        currCodeVersion: '',
        codeTag: '',
        testAddress: '',
        codeBootUrl: '',
        tag: '',
        funcPeople: {},
        tecPeople: {},
        adminPeople: {},
      };
      this.indexDisplayName = {
        currCodeVersion: '',
        codeTag: '',
        testAddress: '',
        codeBootUrl: '',
        tag: '',
      };
      const paramNameMap = this.$store.getters.systemParam['mapKeyName'];

      const currCodeVersion = this.$store.getters.systemParam['soft.tag'];
      const currCodeVersionName = paramNameMap['soft.tag'];
      if (func.notEmpty(currCodeVersion) && func.notEmpty(currCodeVersionName)) {
        this.indexDisplay.currCodeVersion = currCodeVersion;
        this.indexDisplayName.currCodeVersion = currCodeVersionName;
      }

      const codeTag = this.$store.getters.systemParam['branch.code.tag'];
      const codeTagName = paramNameMap['branch.code.tag'];
      if (func.notEmpty(codeTag) && func.notEmpty(codeTagName)) {
        this.indexDisplay.codeTag = codeTag;
        this.indexDisplayName.codeTag = codeTagName;
      }

      const codeBootUrl = this.$store.getters.systemParam['code.address'];
      const codeBootUrlName = paramNameMap['code.address'];
      if (func.notEmpty(codeBootUrl) && func.notEmpty(codeBootUrlName)) {
        this.indexDisplay.codeBootUrl = codeBootUrl;
        this.indexDisplayName.codeBootUrl = codeBootUrlName;
      }

      const testAddress = this.$store.getters.systemParam['test.address'];
      const testAddressName = paramNameMap['test.address'];
      if (func.notEmpty(testAddress) && func.notEmpty(testAddressName)) {
        this.indexDisplay.testAddress = testAddress;
        this.indexDisplayName.testAddress = testAddressName;
      }

      const tag = this.$store.getters.systemParam['from.tag'];
      const tagName = paramNameMap['from.tag'];
      if (func.notEmpty(tag) && func.notEmpty(tagName)) {
        this.indexDisplay.tag = tag;
        this.indexDisplayName.tag = tagName;
      }

      getUserContactList({ isMain: true }).then(res => {
        const resData = res.data.data;
        if (resData != null) {
          this.indexDisplay.adminPeople = resData.adminUser[0] || {};
          this.indexDisplay.tecPeople = resData.tecUser[0] || {};
          this.indexDisplay.funcPeople = resData.funcUser[0] || {};
        }
      });
    },
    getIndexData() {
      this.showDictValue();
    },
    logout() {
      this.$confirm(this.$t('logoutTip'), this.$t('tip'), {
        confirmButtonText: this.$t('submitText'),
        cancelButtonText: this.$t('cancelText'),
        type: 'warning',
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          resetRouter();
          this.$router.push({ path: '/login' });
        });
      });
    },
    handleSubmit(form, done) {
      if (form.newPassword === this.initPassword) {
        this.$message({
          type: 'warning',
          message: '新密码不得为初始默认密码！',
        });
        done();
        return;
      }
      updatePassword(md5(form.oldPassword), md5(form.newPassword), md5(form.newPassword1)).then(
        res => {
          if (res.data.success) {
            this.$message({
              type: 'success',
              message: '修改密码成功!',
            });
            this.resetPasswordVisible = false;
          } else {
            this.$message({
              type: 'error',
              message: res.data.msg,
            });
          }
          done();
        },
        error => {
          window.console.log(error);
          done();
        }
      );
    },
  },
};
</script>

<style>
.row-padding-left p {
  padding-left: 40px;
}

.row-align-center {
  display: flex;
  align-items: center;
}

.row-link {
  color: #25c2a0;
}
</style>
<style scoped>
.bottom-left-tips {
  position: absolute;
  left: -12px;
  top: -12px;
  font-size: 12px;
  z-index: 999;
}
</style>
