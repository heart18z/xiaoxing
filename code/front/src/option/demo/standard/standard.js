

export default  {
  lazy: true,
  tip: false,
  //simplePage: true,
  saveBtn: false,
  updateBtn: false,
  cancelBtn: false,
  emptyBtn: true,
  // refreshBtnText:"查询",
  searchMenuSpan: 6,
  searchBtn: true,
  dialogWidth: "60%",
  border: true,
  selection: true,
  menu: false,
  addBtn: false,
  tree: true,
  dialogDrag: true,
  delBtn: false,
  editBtn: false,
  labelWidth: 90,
  height: 'auto',
  calcHeight: 30,
  column: [{
    label: '序号',
    prop: 'no',
    width: 90,
    display: false,
  },

    {
      label: '题目',
      prop: 'title',
       type:"input",
      //
       //dicUrl: "/api/blade-system/bizParam/dictionary?paramValue=LZLB&isTree=true",
      span: 12,
      cell: true,
      width: 400,
      rules: [{
        required: true,
        message: '请输入题目',
        trigger: 'blur'
      }]
    },
    {
      label: "上级题目",
      prop: "parentId",
      span: 12,
      type: "input",
      // dicUrl: "/api/blade-standard/standard/tree",
      hide: true,
      // disabled: true,
      props: {
        label: "title"
      },
      rules: [{
        required: false,
        message: "请选择上级菜单",
        trigger: "click"
      }]
    },
    {
      label: '更新人',
      span: 24,
      prop: 'updateUser',
      type: "select",
      dicMethod: "post",
            dicUrl: "/api/blade-system/user/user-kv-list",
      width: "120",
      dicFormatter: (res) => {
        return res.data
      },
      props: {
        label: 'key',
        value: 'value',
      },
      display: false,
    },
    {
      label: '更新时间',
      span: 24,
      width: "200",
      prop: 'updateTime',
      display: false,
    },
    {
      label: '附件列表',
      span: 24,
      width: "100",
      prop: 'uploadList',
      slot: true,
      display: false,
    },
    {
      label: '备注',
      span: 12,
      prop: 'remark',
      slot: true
    },
    {
      prop: 'serialNumber',
      label: '排序',
      type:"number",
      width: 100
      // editDisplay: false,
      // formslot: true,
    },
    // {
    //   prop: 'serialNumber',
    //   label: '测试',
    //   type:"tree",
    //   width: 100,
    //   dicMethod: "post",
       //     dicUrl: "/api/blade-system/bizParam/dictionary?paramValue=LZLB&isTree=true",
    // }
  ]
}
