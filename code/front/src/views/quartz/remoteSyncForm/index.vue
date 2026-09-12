<template>
  <el-dialog title="数据同步配置"
             append-to-body
             v-model="boxShow"
             :close-on-click-modal="false"
             :close-on-press-escape="false"
             :show-close="false"
             width="800px">
    <avue-form :option="option"
               v-model="form">
      <template #input="{row}">
        <el-tag>序号:{{row.$index}}-数据:{{row.input}}</el-tag>
      </template>
    </avue-form>
    <template #footer><span class="dialog-footer">
              <el-button type="primary" :loading="loading" @click="submit">确 定</el-button>
            </span></template>
  </el-dialog>
</template>

<script>


export default {
  name: "thirdRegister",
  data() {
    return {
      form: {
        tenantId: '000000',
        name: '',
        account: '',
        password: '',
        password2: '',
      },

      option: {
        submitBtn:false,
        emptyBtn:false,
        column: [
          {
            label:'调用url',
            prop:'url',
            type:'input',
            row:true,
            span:24,
            rules: [{
              required: true,
              message: "请输入远程调用url",
              trigger: "blur"
            }],
          },
          {
            label:'调用类型',
            prop:'method',
            type:'select',
            dicData:[{label: "post", value: "post",}, {label: "get", value: "get",}],
            rules: [{
              required: true,
              message: "请选择调用类型",
              trigger: "blur"
            }],
          },
          {
            label:'返回结构',
            prop:'res',
            type:'input',
            rules: [{
              required: true,
              message: "请输入返回结构",
              trigger: "blur"
            }],
          },
          {
            label:'表名称',
            prop:'tableName',
            type:'input',
            rules: [{
              required: true,
              message: "请输入表名称",
              trigger: "blur"
            }],
          },
          {
            label:'表主键',
            prop:'tablePk',
            type:'input',
            rules: [{
              required: true,
              message: "请输入表主键",
              trigger: "blur"
            }],
          },
          {
            label:'是否创建主键数据',
            labelWidth:"130",
            prop:'isNeedCreatedId',
            type:'switch',
            dicData: [ {
              label: "否",
              value: false
            }, {
              label: "是",
              value: true
            },
             ],
          },
          {
            label:'更新字段',
            prop:'updateColumn',
            type:'input',
          },
          {
            label:'当前页',
            prop:'currentPage',
            type:'input',
          },
          {
            label:'页大小',
            prop:'pageSize',
            type:'input',
          },
          {
            label: 'header参数',
            prop: 'headers',
            type: 'dynamic',
            span:24,
            children: {
              align: 'center',
              headerAlign: 'center',
              rowAdd:(done)=>{
                //this.$message.success('新增回调');
                done({
                  // input:''
                });
              },
              rowDel:(row,done)=>{
                //this.$message.success('删除回调'+JSON.stringify(row));
                done();
              },
              column: [{
                label: '参数名',
                prop: "key"
              }, {
                label: '参数值',
                prop: "value",
              }]
            }
          },
          {
            label: '请求参数',
            prop: 'params',
            type: 'dynamic',
            span:24,
            children: {
              align: 'center',
              headerAlign: 'center',
              rowAdd:(done)=>{
                //this.$message.success('新增回调');
                done({
                  // input:''
                });
              },
              rowDel:(row,done)=>{
                //this.$message.success('删除回调'+JSON.stringify(row));
                done();
              },
              column: [{
                label: '参数名',
                prop: "key"
              }, {
                label: '参数值',
                prop: "value",
              }]
            }
          },
          {
            label: '数据对应',
            prop: 'columnMatches',
            type: 'dynamic',
            span:24,
            children: {
              align: 'center',
              headerAlign: 'center',
              rowAdd:(done)=>{
                //this.$message.success('新增回调');
                done({
                  // input:''
                });
              },
              rowDel:(row,done)=>{
                //this.$message.success('删除回调'+JSON.stringify(row));
                done();
              },
              column: [{
                label: '数据表字段名',
                prop: "tableColumn"
              }, {
                label: '接口数据属性名',
                prop: "targetProp",
              }]
            }
          },

        ]
      },

      loading: false,

      boxShow: false,
    };
  },
  computed: {

  },
  created() {

  },
  mounted() {

  },
  methods: {
    open(data) {
      this.boxShow = true
      this.form = JSON.parse(data)
    },
    submit() {
      this.$emit("submit",this.form)
      this.boxShow = false
    }
  },
};
</script>
