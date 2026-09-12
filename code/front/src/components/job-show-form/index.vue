

<template>
<div >
  <avue-form :ref="Math.random()" v-if="item" :option="jobOption"    :value="item">
    <template #cronExpression="{value}">
      <el-input :model-value="value" disabled>
        <template #append>
          <el-button type="primary" @click="handleShowCron">
            查看表达式
            <i class="el-icon-time el-icon--right"></i>
          </el-button>
        </template></el-input>
    </template>
  </avue-form>
  <el-dialog title="Cron表达式生成器" v-model="openCron" append-to-body destroy-on-close class="scrollbar">
    <crontab @hide="openCron=false" :readOnly="true" :expression="expression"></crontab>
  </el-dialog>
</div>
</template>


<script>
import Crontab from "@/components/Crontab/index.vue";
const process_dic=[

  {value:"1",label:"暂停"},
  {value:"0",label:"启用"},

]
export default {
  name: 'avueshowform',
  components: {Crontab},
  props: {
    item: {
      type: Object,
    },

  },
  data(){
    return{
      expression:"",
      openCron:false,
      jobOption:{
        menuBtn:false,
        column: [
          {
            label: "任务状态",
            prop: "processStatus",
            type: "switch",
            dicData: process_dic,
            hide: true,
            disabled:true,
          },
          {
            label: "创建时间",
            prop: "createTime",
            type:"datetime",
            hide: true,
            disabled:true,
          },
          {
            label: "cron执行表达式",
            prop: "cronExpression",
            type: "input",
            formslot: true,
            labelWidth: 130,
            hide:true,
            disabled:true,
          },
          {
            label: "下次执行时间",
            prop: "jobGroup",
            type: "input",
            labelWidth: 130,
            hide: true,
            disabled:true,
            placeholder: "  "
          },

        ]
      },
    }
  },
  methods :{
    /** cron表达式按钮操作 */
    handleShowCron() {
      this.expression = this.item.cronExpression;
      this.openCron = true;
    },
  }
}
</script>
