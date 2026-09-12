<template>
  <basic-container class="user-page">
      <avue-crud
        :option="option"
        v-model:search="query"
        :table-loading="loading"
        :data="data"
        ref="crud"
        v-model="form"
        :permission="permissionList"
        @row-del="rowDel"
        @row-update="rowUpdate"
        @row-save="rowSave"
        :before-open="beforeOpen"
        v-model:page="page"
        @search-change="searchChange"
        @search-reset="searchReset"
        @selection-change="selectionChange"
        @current-change="currentChange"
        @size-change="sizeChange"
        @refresh-change="refreshChange"
        @cell-click="rowView"
        @cell-dblclick="editList"
        @on-load="onLoad"
      >
        <template #menu-left>
          <el-button
            class="button-size"
            round
            title="新增"
            v-if="permission.user_add"
            size="small"
            icon="el-icon-plus"
            @click.stop="addList()"
          />
          <el-button
            class="button-size"
            size="small"
            round
            title="删 除"
            plain
            icon="el-icon-delete"
            v-if="permission.user_delete"
            @click="handleDelete"
          />
          <el-button
            class="button-size"
            round
            title="复制"
            v-if="permission.user_add"
            size="small"
            icon="el-icon-document-copy"
            @click.stop="copyList()"
          />
          <el-button
            class="button-size"
            round
            title="密码重置"
            size="small"
            plain
            v-if="permission.user_reset"
            :disabled="selectionList.length > 1"
            icon="el-icon-refresh"
            @click="handleReset"
          />
        </template>

        <template #menu-form="{ type }">
          <template v-if="type !== 'view'">
            <el-button
              class="form-menu-btn form-menu-btn--save"
              type="primary"
              size="small"
              circle
              title="保存"
              @click="$refs.crud.rowSave()"
            >
              <el-icon><Check /></el-icon>
            </el-button>
            <el-button
              class="form-menu-btn"
              size="small"
              circle
              title="取消"
              @click="$refs.crud.closeDialog()"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </template>
        </template>

        <template #index="{ index }">
          <div>{{ index + 1 }}</div>
        </template>

        <template #foreignUser-header="{ column }">
          <el-icon class="avue-group__icon"><Connection /></el-icon>
          <h1 class="avue-group__title">{{ (column || {}).label }}</h1>
          <el-tooltip
            class="item"
            effect="dark"
            content="切换来源保存之后，除非禁用或删除，否则不能更改来源模式！"
            placement="top"
          >
            <el-icon class="group-header-tip"><Warning /></el-icon>
          </el-tooltip>
          <div class="group-header-switch" @click.stop>
            <avue-switch
              active-color="#13ce66"
              inactive-color="#00BBFF"
              v-model="isRelPeople"
              :dic="relDic"
              :disabled="formType !== 'add'"
              inactiveColor="#DCDFE6"
              :inlinePrompt="true"
              @change="changeRelPeople"
            />
          </div>
        </template>

        <template #baseInfo-header="{ column }">
          <el-icon class="avue-group__icon"><UserFilled /></el-icon>
          <h1 class="avue-group__title">{{ (column || {}).label }}</h1>
        </template>

        <template #detailInfo-header="{ column }">
          <el-icon class="avue-group__icon"><Tickets /></el-icon>
          <h1 class="avue-group__title">{{ (column || {}).label }}</h1>
        </template>

        <template #dutyInfo-header="{ column }">
          <el-icon class="avue-group__icon"><Avatar /></el-icon>
          <h1 class="avue-group__title">{{ (column || {}).label }}</h1>
        </template>

        <template #identity-label>
          <span>用户身份&nbsp;&nbsp;</span>
          <el-tooltip
            class="item"
            effect="dark"
            content='六个用户身份中包含"主要"字样的选择项只能有一个用户选择，并在首页仅展示"主要**身份"'
            placement="top"
          >
            <el-icon class="field-label-tip"><QuestionFilled /></el-icon>
          </el-tooltip>
        </template>

        <template #roleName="{ row }">
          <el-tag>{{ row.roleName }}</el-tag>
        </template>
        <template #deptName="{ row }">
          <el-tag>{{ row.deptName }}</el-tag>
        </template>
        <template #status="{ row }">
          <avue-switch
            active-color="#13ce66"
            inactive-color="#00BBFF"
            v-model="row.status"
            :dic="dic"
            inactiveColor="#DCDFE6"
            :inlinePrompt="true"
            @click="handleLock(row, row.status)"
          />
        </template>

        <template #oauth2LoginTips-form>
          <div class="oauth2-login-tips">
            登录账号限定为唯一登录，不论在任何地址或浏览器，相同的系统相同的账号仅限一处登录，最新登录的系统将会挤出其他系统
          </div>
        </template>
      </avue-crud>

      <page-attach ref="pageAttach" />
      <el-dialog title="用户数据导入" append-to-body v-model="excelBox" width="555px">
        <avue-form :option="excelOption" v-model="excelForm" :upload-after="uploadAfter">
          <template #excelTemplate>
            <el-button type="primary" @click="handleTemplate">
              点击下载<i class="el-icon-download el-icon--right"></i>
            </el-button>
          </template>
        </avue-form>
      </el-dialog>
  </basic-container>
</template>

<script>
import {
  getList,
  getUser,
  remove,
  update,
  add,
  resetPassword,
  unlock,
  lock,
  resetPasswordShow,
} from '@/api/system/user';
import { exportBlob } from '@/api/common';
import { getDeptTree } from '@/api/system/dept';
import { getRoleTree, getList as getRoleList } from '@/api/system/role';
import { getPostList } from '@/api/system/post';
import { getSysTemParamMap } from '@/api/system/param';
import { mapGetters } from 'vuex';
import website from '@/config/website';
import { getToken } from '@/utils/auth';
import { downloadXls } from '@/utils/util';
import { dateNow } from '@/utils/date';
import NProgress from 'nprogress';
import 'nprogress/nprogress.css';
import func from '@/utils/func';
import pageAttach from '@/components/attach-dialog/page-attach/main.vue';
import { Connection } from '@element-plus/icons-vue';

export default {
  components: { pageAttach, Connection },
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
    const validatePass2 = (rule, value, callback) => {
      if (value !== '') {
        if (value !== this.form.password) {
          callback(new Error('两次输入的密码不一致'));
        }
      }
      callback();
    };
    return {
      isRelPeople: 2,
      formType: 'add',
      dic: [],
      relDic: [],
      form: {},
      search: {},
      excelBox: false,
      initFlag: true,
      selectionList: [],
      query: {},
      loading: true,
      timer: null,
      page: {
        pageSize: 30,
        currentPage: 1,
        total: 0,
      },
      props: {
        label: 'title',
        value: 'key',
      },
      roleGrantList: [],
      roleTreeObj: [],
      treeData: [],
      option: {
        searchShowBtn: false,
        height: 'auto',
        calcHeight: 80,
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
        selection: true,
        viewBtn: true,
        dialogType: 'drawer',
        dialogClickModal: false,
        column: [
          {
            label: '序号',
            prop: 'index',
            fixed: true,
            width: 50,
            display: false,
            align: 'center',
          },
          {
            label: '登录账号',
            prop: 'account',
            search: true,
            display: false,
          },
          {
            label: '用户姓名',
            prop: 'realName',
            search: true,
            display: false,
          },
          {
            label: '所属角色',
            prop: 'roleName',
            slot: true,
            display: false,
          },
          {
            label: '所属部门',
            prop: 'deptId',
            type: 'tree',
            dicMethod: 'post',
            dicUrl: '/api/blade-system/dept/tree',
            props: {
              children: 'children',
              label: 'title',
              value: 'id',
            },
            search: true,
            slot: true,
            display: false,
            hide: true,
          },
          {
            label: '所属部门',
            prop: 'deptName',
            slot: true,
            display: false,
            hide: true,
          },
          {
            label: '所属角色',
            prop: 'roleId',
            dicData: [],
            type: 'select',
            props: {
              label: 'title',
              value: 'id',
            },
            search: true,
            display: false,
            hide: true,
          },
          {
            label: '所属岗位',
            prop: 'postId',
            dicData: [],
            props: {
              label: 'postName',
              value: 'id',
            },
            type: 'select',
            search: true,
            display: false,
            hide: true,
          },
          {
            label: '模块筛选',
            prop: 'menuId',
            type: 'tree',
            dicMethod: 'post',
            dicUrl: '/api/blade-system/menu/grant-top-tree',
            search: true,
            dicFormatter: res => {
              return res.data.menu;
            },
            hide: true,
            display: false,
            props: {
              label: 'title',
              value: 'id',
              res: 'id',
            },
          },
          {
            label: '用户平台',
            type: 'select',
            dicUrl: '/api/blade-system/dict/dictionary?code=user_type',
            dicMethod: 'post',
            props: {
              label: 'dictValue',
              value: 'dictKey',
            },
            multiple: true,
            dataType: 'number',
            search: false,
            display: false,
            prop: 'userType',
            rules: [
              {
                required: true,
                message: '请选择用户平台',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '关联人员号',
            prop: 'foreignNo',
            display: false,
            type: 'select',
            dicMethod: 'post',
            dicUrl: '/api/blade-people/people/select',
            props: {
              label: 'account',
              value: 'id',
            },
          },
          {
            label: '关联人姓名',
            prop: 'foreignName',
            display: false,
            type: 'select',
            dicMethod: 'post',
            dicUrl: '/api/blade-people/people/select',
            props: {
              label: 'realName',
              value: 'id',
            },
          },
          {
            label: '最后登录时间',
            prop: 'lastLoginTime',
            display: false,
          },
          {
            label: '帐号状态',
            prop: 'status',
            dicData: [],
            type: 'select',
            search: true,
            slot: true,
            display: false,
          },
        ],
        group: [
          {
            label: '来源',
            prop: 'foreignUser',
            icon: 'Connection',
            formSlot: true,
            column: [
              {
                label: '关联人员',
                prop: 'peopleId',
                type: 'tree',
                disabled: false,
                filterable: true,
                dicMethod: 'post',
                dicUrl: '/api/blade-people/people/select',
                props: {
                  label: 'label',
                  value: 'id',
                },
                dicFormatter: res => {
                  const data = res.data;
                  for (const d of data) {
                    d.hasChildren = false;
                    d.children = [];
                  }
                  return data;
                },
                rules: [],
                change: () => {},
              },
              {
                label: '编制隶属',
                prop: 'deptBelongTo',
              },
            ],
          },
          {
            label: '基础信息',
            prop: 'baseInfo',
            icon: 'UserFilled',
            column: [
              {
                label: '登录账号',
                prop: 'account',
                rules: [
                  {
                    required: true,
                    message: '请输入登录账号',
                    trigger: 'blur',
                  },
                ],
                disabled: false,
              },
              {
                label: '用户平台',
                type: 'select',
                dicMethod: 'post',
                dicUrl: '/api/blade-system/dict/dictionary?code=user_type',
                props: {
                  label: 'dictValue',
                  value: 'dictKey',
                },
                multiple: true,
                dataType: 'number',
                search: false,
                slot: true,
                prop: 'userType',
                rules: [
                  {
                    required: true,
                    message: '请选择用户平台',
                    trigger: 'blur',
                  },
                ],
              },
              {
                label: '密码',
                prop: 'password',
                hide: true,
                editDisplay: false,
                viewDisplay: false,
                disabled: true,
                rules: [{ required: true, validator: validatePass, trigger: 'blur' }],
              },
              {
                label: '确认密码',
                prop: 'password2',
                hide: true,
                editDisplay: false,
                viewDisplay: false,
                disabled: true,
                rules: [
                  { required: true, validator: validatePass, trigger: 'blur' },
                  {
                    required: true,
                    validator: validatePass2,
                    trigger: 'blur',
                  },
                ],
              },
              {
                label: '',
                labelWidth: 0,
                prop: 'oauth2LoginTips',
                slot: true,
                hide: true,
                span: 24,
                row: true,
              },
            ],
          },
          {
            label: '详细信息',
            prop: 'detailInfo',
            icon: 'Tickets',
            column: [
              {
                label: '用户姓名',
                prop: 'realName',
                rules: [
                  {
                    required: true,
                    message: '请输入用户姓名',
                    trigger: 'blur',
                  },
                  {
                    min: 2,
                    max: 18,
                    message: '姓名长度在2到18个字符',
                  },
                ],
                disabled: false,
              },
              {
                label: '用户昵称',
                prop: 'name',
                hide: true,
                disabled: false,
              },
              {
                label: '手机号码',
                prop: 'phone',
                disabled: false,
                overHidden: true,
              },
              {
                label: '电子邮箱',
                prop: 'email',
                hide: true,
                disabled: false,
                overHidden: true,
              },
              {
                label: '微信',
                prop: 'wechat',
                hide: true,
                disabled: false,
                overHidden: true,
              },
              {
                label: '用户性别',
                prop: 'sex',
                type: 'select',
                disabled: false,
                dicData: [
                  { label: '男', value: 1 },
                  { label: '女', value: 2 },
                  { label: '未知', value: 3 },
                ],
                hide: true,
              },
              {
                label: '用户生日',
                type: 'date',
                prop: 'birthday',
                disabled: false,
                format: 'YYYY-MM-DD HH:mm:ss',
                valueFormat: 'YYYY-MM-DD HH:mm:ss',
                hide: true,
              },
              {
                label: '账号状态',
                prop: 'statusName',
                hide: true,
                display: false,
              },
            ],
          },
          {
            label: '职责信息',
            prop: 'dutyInfo',
            icon: 'Avatar',
            column: [
              {
                label: '用户编号',
                prop: 'code',
              },
              {
                label: '所属角色',
                prop: 'roleId',
                multiple: true,
                type: 'tree',
                dicData: [],
                props: {
                  label: 'title',
                },
                checkStrictly: true,
                slot: true,
                rules: [
                  {
                    required: true,
                    message: '请选择所属角色',
                    trigger: 'click',
                  },
                ],
                control: data => {
                  this.checkRoleGetIdentity(data);
                },
              },
              {
                label: '所属部门',
                prop: 'deptId',
                type: 'tree',
                multiple: true,
                dicData: [],
                props: {
                  label: 'title',
                  value: 'id',
                },
                slot: true,
              },
              {
                label: '所属岗位',
                prop: 'postId',
                type: 'tree',
                multiple: true,
                dicData: [],
                props: {
                  label: 'postName',
                  value: 'id',
                },
                rules: [],
              },
              {
                label: '用户身份',
                prop: 'identity',
                type: 'select',
                multiple: true,
                dicMethod: 'post',
                dicUrl: '/api/blade-system/user/user-contact-dict',
                props: {
                  label: 'key',
                  value: 'value',
                },
                rules: [],
                formslot: true,
              },
              {
                label: '职级',
                prop: 'postLevel',
              },
            ],
          },
        ],
      },
      data: [],
      excelForm: {},
      excelOption: {
        submitBtn: false,
        emptyBtn: false,
        column: [
          {
            label: '模板上传',
            prop: 'excelFile',
            type: 'upload',
            drag: true,
            loadText: '模板上传中，请稍等',
            span: 24,
            propsHttp: {
              res: 'data',
            },
            tip: '请上传 .xls,.xlsx 标准格式文件',
            action: '/api/blade-system/user/import-user',
          },
          {
            label: '数据覆盖',
            prop: 'isCovered',
            type: 'switch',
            align: 'center',
            width: 80,
            dicData: [
              { label: '否', value: 0 },
              { label: '是', value: 1 },
            ],
            value: 0,
            slot: true,
            rules: [
              {
                required: true,
                message: '请选择是否覆盖',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '模板下载',
            prop: 'excelTemplate',
            formslot: true,
            span: 24,
          },
        ],
      },
      adminRoleList: [],
    };
  },
  watch: {
    'excelForm.isCovered'() {
      if (this.excelForm.isCovered !== '') {
        const column = this.findObject(this.excelOption.column, 'excelFile');
        column.action = `/api/blade-system/user/import-user?isCovered=${this.excelForm.isCovered}`;
      }
    },
  },
  computed: {
    ...mapGetters(['userInfo', 'permission']),
    permissionList() {
      return {
        addBtn: this.vaildData(this.permission.user_add, false),
        viewBtn: this.vaildData(this.permission.user_view, false),
        delBtn: this.vaildData(this.permission.user_delete, false),
        editBtn: this.vaildData(this.permission.user_edit, false),
      };
    },
    ids() {
      const ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
    initPassword() {
      return this.$store.getters.systemParam['account.initPassword'];
    },
  },
  mounted() {
    if (!website.tenantMode) {
      this.initData(website.tenantId);
    }
    this.initDicData();
    this.dic = [
      { label: '禁用', value: 2 },
      { label: '启用', value: 1 },
    ];
    this.relDic = [
      { label: '', value: 2 },
      { label: '', value: 1 },
    ];
    const statusColumn = this.findObject(this.option.column, 'status');
    statusColumn.dicData = [
      { label: '禁用', value: 2 },
      { label: '启用', value: 1 },
    ];
  },
  methods: {
    changPeople(data) {
      if (data && data.item) {
        this.form.account = data.item.account;
        this.form.realName = data.item.realName;
        this.form.phone = data.item.phone;
        this.form.sex = data.item.sex;
        this.form.email = data.item.email;
        this.form.wechat = data.item.wechat;
        this.form.deptId = data.item.deptId;
        this.form.postId = data.item.postId;
      }
    },
    changeRelPeople(event) {
      this.isRelPeople = event.value;
      const peopleIdColumn = this.findObject(this.option.group, 'peopleId');
      if (this.isRelPeople === 2) {
        this.changeBaseInfoDisabled(false);
        peopleIdColumn.disabled = true;
        peopleIdColumn.rules = [];
        peopleIdColumn.change = () => {};
      } else {
        peopleIdColumn.disabled = false;
        peopleIdColumn.rules = [
          {
            required: true,
            message: '请选择来源',
            trigger: 'blur',
          },
        ];
        peopleIdColumn.change = this.changPeople;
        if (func.isEmpty(this.form.peopleId)) {
          this.form.account = null;
          this.form.realName = null;
          this.form.phone = null;
          this.form.sex = null;
          this.form.email = null;
          this.form.wechat = null;
        }
        this.changeBaseInfoDisabled(true);
      }
    },
    changeBaseInfoDisabled(flag) {
      const accountColumn = this.findObject(this.option.group, 'account');
      const realNameColumn = this.findObject(this.option.group, 'realName');
      const phoneColumn = this.findObject(this.option.group, 'phone');
      const wechatColumn = this.findObject(this.option.group, 'wechat');
      const sexColumn = this.findObject(this.option.group, 'sex');
      const emailColumn = this.findObject(this.option.group, 'email');
      const deptIdColumn = this.findObject(this.option.group, 'deptId');
      const postIdColumn = this.findObject(this.option.group, 'postId');
      const columnCollection = [
        accountColumn,
        realNameColumn,
        phoneColumn,
        wechatColumn,
        sexColumn,
        emailColumn,
        postIdColumn,
        deptIdColumn,
      ];
      for (const colum of columnCollection) {
        colum.disabled = flag;
      }
    },
    refreshSystemParam() {
      getSysTemParamMap().then(res => {
        const data = res.data.data;
        if (func.notEmpty(data)) {
          this.$store.commit('SET_SYSTEM_PARAM', res.data.data);
        }
      });
    },
    initDicData() {
      getRoleList().then(res => {
        const roleList = res.data.data;
        this.adminRoleList = roleList.filter(role => {
          return role.roleAlias.includes('admin');
        });
      });
      this.refreshSystemParam();
    },
    checkRoleGetIdentity(roles) {
      if (
        func.notEmpty(roles) &&
        func.notEmpty(this.adminRoleList) &&
        func.isEmpty(this.form.identity)
      ) {
        const roleArr = Array.isArray(roles) ? roles : roles.split(',');
        const identity = [];
        for (const role of roleArr) {
          for (const adminRole of this.adminRoleList) {
            if (role == adminRole.id) {
              if (adminRole.roleAlias == 'administrator') {
                identity.push('main_t');
              } else if (adminRole.roleAlias == 'admin') {
                identity.push('main_a');
              }
            }
          }
        }
        if (identity.length === 0) {
          identity.push('customer');
        }
        this.form.identity = identity.join(',');
      }
    },
    addList() {
      this.formType = 'add';
      this.$refs.crud.rowAdd();
      this.form = {
        password: this.initPassword || 'Dfyj123',
        password2: this.initPassword || 'Dfyj123',
        status: 1,
        userType: [1],
        loginType: 'user_loginMethod_account',
      };
      this.$refs.crud.dicInit();
    },
    copyList() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.formType = 'add';
      this.$refs.crud.rowAdd();
      this.form.deptId = [];
      this.form.postId = [];
      this.form.roleId = [];
      getUser(this.selectionList[0].id).then(res => {
        this.form = res.data.data;
        if (this.form.hasOwnProperty('deptId')) {
          this.form.deptId = this.form.deptId.split(',');
        }
        if (this.form.hasOwnProperty('roleId')) {
          this.form.roleId = this.form.roleId.split(',');
        }
        if (this.form.hasOwnProperty('postId')) {
          this.form.postId = this.form.postId.split(',');
        }
        if (this.form.hasOwnProperty('userType')) {
          this.form.userType = this.form.userType.split(',');
        }
      });
      this.form.id = '';
      this.form.password = this.initPassword || 'Dfyj123';
      this.form.password2 = this.initPassword || 'Dfyj123';
      this.form.status = 1;
      this.$refs.crud.dicInit();
    },
    editList(row, { property }) {
      if (this.permission.user_edit && property === 'index') {
        clearTimeout(this.timer);
        if (row.account == 'admin' && this.userInfo.account != 'admin') {
          this.$message.warning('超级管理员信息只能由本人修改！');
          return;
        }
        const column = this.findObject(this.option.group, 'roleId');
        if (row.account == 'admin') {
          column.disabled = true;
        } else {
          column.disabled = false;
        }
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
    initData(tenantId) {
      getRoleTree(tenantId).then(res => {
        const searchColumn = this.findObject(this.option.column, 'roleId');
        searchColumn.dicData = [...res.data.data];
        const column = this.findObject(this.option.group, 'roleId');
        res.data.data.forEach(ele => {
          if (ele.title === '超级管理员' && !this.userInfo.role_name.includes('administrator')) {
            ele.disabled = true;
          }
        });
        column.dicData = res.data.data;
      });
      getDeptTree(tenantId).then(res => {
        const column = this.findObject(this.option.group, 'deptId');
        column.dicData = res.data.data;
      });
      getPostList(tenantId).then(res => {
        const column = this.findObject(this.option.group, 'postId');
        column.dicData = res.data.data;
        const searchColumn = this.findObject(this.option.column, 'postId');
        searchColumn.dicData = res.data.data;
      });
    },
    rowSave(row, done, loading) {
      row.deptId = func.join(row.deptId);
      row.roleId = func.join(row.roleId);
      row.postId = func.join(row.postId);
      row.identity = func.join(row.identity);
      row.userType = func.join(row.userType);
      add(row).then(
        () => {
          this.initFlag = false;
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
      row.deptId = func.join(row.deptId);
      row.roleId = func.join(row.roleId);
      row.postId = func.join(row.postId);
      row.identity = func.join(row.identity);
      row.userType = func.join(row.userType);
      update(row).then(
        () => {
          this.initFlag = false;
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
      this.onLoad(this.page);
      done();
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    selectionClear() {
      this.selectionList = [];
      if (this.$refs.crud) {
        this.$refs.crud.toggleSelection();
      }
    },
    handleDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      for (let i = 0; i < this.selectionList.length; i++) {
        const row = this.selectionList[i];
        if (row.account == 'admin') {
          this.$message.warning('超级管理员信息不得删除！');
          return;
        }
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
    handleReset() {
      if (this.selectionList.length !== 1) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      for (let i = 0; i < this.selectionList.length; i++) {
        const row = this.selectionList[i];
        if (row.account == 'admin' && this.userInfo.account != 'admin') {
          this.$message.warning('超级管理员信息只能由本人修改！');
          return;
        }
        resetPasswordShow(this.ids).then(res => {
          this.$confirm('确定将选择账号密码重置为：' + res.data.data, {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          })
            .then(() => {
              return resetPassword(this.ids);
            })
            .then(() => {
              this.$message({
                type: 'success',
                message: '操作成功!',
              });
              this.$refs.crud.toggleSelection();
            });
        });
      }
    },
    handleLock(row, type) {
      if (row.account == 'admin' && this.userInfo.account != 'admin') {
        this.$message.warning('超级管理员信息只能由本人修改！');
        return;
      }
      if (type == 2) {
        lock(row.id).then(() => {
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
        });
      } else {
        unlock(row.id).then(() => {
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
        });
      }
    },
    handleImport() {
      this.excelBox = true;
    },
    uploadAfter(res, done, loading, column) {
      window.console.log(column);
      this.excelBox = false;
      this.refreshChange();
      done();
    },
    handleExport() {
      const account = func.toStr(this.search.account);
      const realName = func.toStr(this.search.realName);
      this.$confirm('是否导出用户数据?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        NProgress.start();
        exportBlob(
          `/api/blade-system/user/export-user?${this.website.tokenHeader}=${getToken()}&account=${account}&realName=${realName}`
        ).then(res => {
          downloadXls(res.data, `用户数据表${dateNow()}.xlsx`);
          NProgress.done();
        });
      });
    },
    handleTemplate() {
      exportBlob(
        `/api/blade-system/user/export-template?${this.website.tokenHeader}=${getToken()}`
      ).then(res => {
        downloadXls(res.data, '用户数据模板.xlsx');
      });
    },
    beforeOpen(done, type) {
      this.formType = type;
      this.form.deptId = [];
      this.form.postId = [];
      this.form.roleId = [];
      if (['edit', 'view'].includes(type)) {
        const peopleIdColumn = this.findObject(this.option.group, 'peopleId');
        peopleIdColumn.change = () => {};
        getUser(this.form.id).then(res => {
          if (res.data.data.peopleId === null) {
            res.data.data.peopleId = '';
          }
          this.form = res.data.data;
          if (this.form.hasOwnProperty('deptId')) {
            this.form.deptId = this.form.deptId.split(',');
          }
          if (this.form.hasOwnProperty('roleId')) {
            this.form.roleId = this.form.roleId.split(',');
          }
          if (this.form.hasOwnProperty('postId')) {
            this.form.postId = this.form.postId.split(',');
          }
          if (this.form.hasOwnProperty('userType')) {
            this.form.userType = this.form.userType.split(',');
          }
          if (func.isEmpty(this.form.peopleId)) {
            this.isRelPeople = 2;
          } else {
            this.isRelPeople = 1;
          }
        });
      } else {
        this.isRelPeople = 2;
      }
      this.initFlag = true;
      done();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
    },
    refreshChange() {
      this.onLoad(this.page);
    },
    onLoad(page) {
      this.loading = true;
      getList(page.currentPage, page.pageSize, Object.assign({}, this.query)).then(res => {
        const data = res.data.data;
        data.records.forEach(item => {
          if (item.peopleId === null) {
            item.peopleId = '';
          }
          item.foreignName = item.peopleId;
          item.foreignNo = item.peopleId;
        });
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
.user-page :deep(.el-table) {
  width: 100%;
}

.user-page :deep(.avue-crud .el-dropdown + .el-button) {
  margin-left: 0 !important;
}

.user-page :deep(.avue-group__header) {
  display: flex;
  align-items: center;
}

.user-page :deep(.avue-group__icon),
.user-page :deep(.avue-group__header .el-icon) {
  margin-right: 8px;
  font-size: 16px;
  color: #409eff;
}

.user-page :deep(.avue-group__title) {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}

.group-header-tip,
.field-label-tip {
  margin-left: 10px;
  color: #e6a23c;
  cursor: pointer;
}

.group-header-switch {
  margin-left: 10px;
}

.oauth2-login-tips {
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
}

.form-menu-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.form-menu-btn + .form-menu-btn {
  margin-left: 8px;
}

.form-menu-btn .el-icon {
  margin: 0;
}
</style>
