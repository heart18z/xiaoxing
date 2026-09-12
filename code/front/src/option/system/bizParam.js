// let _this = null
// export const assign = (vm) => {
//   _this = vm
// }

/**
 * 参数值称验证器
 */
export const paramValueValid = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('不能为空'));
  } else if (value.indexOf('.') != -1) {
    return callback(new Error('不能包含点号'));
  } else if (value.indexOf('_') != -1) {
    return callback(new Error('不能包含下划线'));
  }
  return callback();
}

export const optionParent = {
  searchShowBtn: false,
  menu: false,
  height: "auto",
  calcHeight: 30,
  tip: false,
  delBtn: false,
  searchShow: true,
  searchMenuSpan: 10,
  saveBtn: false,
  updateBtn: false,
  cancelBtn: false,
  searchBtn: false,
  emptyBtn: false,
  border: true,
  // index: true,
  selection: true,
  viewBtn: true,
  menuWidth: 280,
  dialogWidth: 880,
  dialogDrag: true,
  dialogClickModal: false,
  addBtn: false,
  editBtn: false,
  column: [
    {
      label: '序号',
      prop: 'index',
      fixed: true,
      width: 70,
      display: false,
      align: "center"
    }, {
      label: "参数名称",
      prop: "paramName",
      search: true,
      slot: true,
      editDisabled: false,
      searchPlaceholder: "本搜索显示含子级内容，无层级列表显示",
      rules: [{
        required: true,
        message: "请输入参数名称",
        trigger: "blur",
      },],
      // searchChange: (val)=>{
      //     if (val.value !='') {
      //         _this.query.paramKey = ""
      //     }
      // },
      // searchBlur: ()=>{
      //   _this.onLoadParent(_this.pageParent)
      // }
    },
    {
      label: "参数值",
      prop: "paramKey",
      editDisabled: true,

      align: "center",
      searchPlaceholder: "本搜索只显示下表中包含的字符",

      rules: [{
        required: true,
        message: "请输入参数类型",
        trigger: "blur",
      }, {
        required: true,
        validator: paramValueValid,
        trigger: "blur",
      }],
      // searchChange: (val)=>{
      //     if (val.value !='') {
      //         _this.query.paramName = ""
      //     }
      // },
      // searchBlur: ()=>{
      //   _this.onLoadParent(_this.pageParent)
      // }
    },
    {
      label: "参数值",
      prop: "paramValue",

      search: true,

      display: false,
      hide: true
    },
    {
      label: "层级限制",
      prop: "hierarchical",
      editDisabled: false,
      type: "number",
      align: "center",
      width: 70,
      rules: [{
        required: true,
        message: "请输入层级限制",
        trigger: "blur",
      },],
    },
    {
      label: "排序",
      prop: "sort",
      type: "number",
      align: "center",
      width: 70,
      rules: [{
        required: true,
        message: "请输入参数排序",
        trigger: "blur",
      },],
    },
    {
      label: "是否启用",
      prop: "status",
      align: "center",
      editDisabled: false,
      type: "switch",
      width: 70,
      dicData: [{
        label: "否",
        value: 0,
      },
        {
          label: "是",
          value: 1,
        },
      ],
      value: 1,
      slot: true,
      rules: [{
        required: true,
        message: "请选择是否启用",
        trigger: "blur",
      },],
    },

    {
      label: "数据来源",
      prop: "isSync",
      type: "select",
      width: 100,
      display: false,
      search: true,
      dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=data_source",
      props: {
        label: "dictValue",
        value: "dictKey"
      },
    },
    {
      label: "公共参数",
      width: 95,
      search: true,
      prop: "isPublicParam",
      type: "select",
      display: false,
      dicData: [{value: "1", label: "是"}, {value: "0", label: "否"}]
    },
    {
      label: "来源系统",
      prop: "systemSource",
      search: true,
      width: 120,
      display: false
    },
    {
      label: "含义界定",
      prop: "remark",
      type: "textarea",
      minRows: 3,
      editDisabled: false,
      overHidden: true,
      span: 24,
      width: 200,
      rules: [{
        required: false,
        message: "请输入备注",
        trigger: "blur",
      },],
    },
  ],
};

export const optionChild = {
  searchShowBtn: false,
  menu: false,
  height: "auto",
  calcHeight: 95,
  tip: false,
  searchShow: true,
  searchBtn: false,
  emptyBtn: false,
  dialogDrag: true,
  // searchMenuSpan: 10,
  tree: true,
  border: true,
  // index: true,
  viewBtn: true,
  selection: true,
  menuWidth: 300,
  labelWidth: 100,
  dialogWidth: 880,
  dialogClickModal: false,
  addBtn: false,
  editBtn: false,
  column: [
    {
      label: '序号',
      prop: 'no',
      width: 90,
      display: false,
    },
    {
      label: "父级参数",
      prop: "parentId",
      type: "tree",
      dicData: [],
      hide: true,
      props: {
        label: "title",
      },
      addDisabled: true,
      editDisabled: true,
      rules: [{
        required: true,
        message: "请选择父级参数",
        trigger: "click",
      },],
    },
    {
      label: "父级参数值",
      prop: "paramKey",
      addDisabled: true,
      editDisabled: true,
      // search: true,
      searchLabelWidth: "120",
      // searchSpan: 8,
      hide: true,
      rules: [{
        required: true,
        message: "请输入父级参数值",
        trigger: "blur",
      },],
    },
    {
      label: "层级限制",
      prop: "hierarchical",
      type: "number",
      align: "right",
      addDisabled: true,
      editDisabled: true,
      hide: true,
      rules: [{
        required: true,
        message: "请输入层级限制",
        trigger: "blur",
      },],
    },
    {
      label: "当前层级",
      prop: "currentHierarchy",
      type: "number",
      align: "right",
      addDisabled: true,
      editDisabled: true,
      hide: true,
      rules: [{
        required: true,
        message: "请输入当前层级",
        trigger: "blur",
      },],
    },
    {
      label: "参数名称",
      prop: "paramName",
      search: true,
      slot: true,
      // searchSpan: 8,
      width: 180,
      rules: [{
        required: true,
        message: "请输入参数名称",
        trigger: "blur",
      },],
    },
    {
      label: "参数值",
      prop: "paramValue",

      search: true,
      //searchSpan: 8,
      align: "center",
      editDisabled: true,
      formslot: true,
      width: 200,
      rules: [{
        required: true,
        message: "请输入参数内容",
        trigger: "blur",
      }, {
        required: true,
        validator: paramValueValid,
        trigger: "blur",
      }],
    },
    {
      label: "排序",
      prop: "sort",
      width: 95,
      type: "number",
      align: "center",
      rules: [{
        required: true,
        message: "请输入排序",
        trigger: "blur",
      },],
    },
    {
      label: "是否启用",
      prop: "status",
      type: "switch",
      align: "center",
      width: '80px',
      dicData: [{
        label: "否",
        value: 0,
      },
        {
          label: "是",
          value: 1,
        },
      ],
      value: 1,
      slot: true,
      rules: [{
        required: true,
        message: "请选择是否启用",
        trigger: "blur",
      },],
    },

    {
      label: "数据来源",
      prop: "isSync",
      type: "select",
      search: true,
      width: 95,
      display: false,
      dicMethod: "post",
            dicUrl: "/api/blade-system/dict-biz/dictionary?code=data_source",
      props: {
        label: "dictValue",
        value: "dictKey"
      },
    },
    {
      label: "公共参数",
      width: 95,
      prop: "isPublicParam",
      search: true,
      type: "select",
      display: false,
      dicData: [{value: "1", label: "是"}, {value: "0", label: "否"}]
    },
    {
      label: "来源系统",
      prop: "systemSource",
      search: true,
      width: 120,
      display: false
    },
    {
      label: "备注",
      prop: "remark",
      type: "textarea",
      overHidden: true,
      minRows: 3,
      span: 24,
      rules: [{
        required: false,
        message: "请输入备注",
        trigger: "blur",
      },],
    },
  ],
};

