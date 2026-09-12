<template>
  <basic-container>
    <avue-crud :option="option"
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
               @sort-change="sortChange"
               @selection-change="selectionChange"
               @current-change="currentChange"
               @size-change="sizeChange"
               @refresh-change="refreshChange"
               @on-load="onLoad">
        <!-- 左边按钮 -->
        <template #menu-left="{ row,index }">
            <el-button class="button-size" round title="新增" v-if="permission.btnConfig_add" size="small" icon="el-icon-plus" @click.stop="addList()">
            </el-button>
            <el-button class="button-size" round title="删除" v-if="permission.btnConfig_delete" size="small" icon="el-icon-delete" @click.stop="handleDelete()">
            </el-button>
            <el-button class="button-size" round title="复制" v-if="permission.btnConfig_add" size="small" icon="el-icon-document-copy" @click.stop="copyList()">
            </el-button>
            <el-button class="button-size" round title="AI对话" v-if="permission.btnConfig_add" size="small" icon="el-icon-chat-dot-round" @click.stop="openAiChatDialog()">
            </el-button>
        </template>

        <!-- 打开附件列表 -->
        <template #uploadList="{ row }">
            <row-attach :source-id="row.id"></row-attach>
        </template>

        <template #index="{row,index}">
            <div>{{index+1}}</div>
        </template>



      <template #btnTypeType="{ item,value,label }">
        &nbsp;&nbsp;{{ item.dictValue }}&nbsp;&nbsp;
        <el-tooltip class="item" effect="dark" :content="item.remark" placement="top-start"
                    v-if="item.remark">
          <i style="float: right;margin-right: 4rem;line-height: inherit;" class="el-icon-question"></i>
        </el-tooltip>
      </template>


      <template #interactionTypeType="{ item,value,label }">
        &nbsp;&nbsp;{{ item.dictValue }}&nbsp;&nbsp;
        <el-tooltip class="item" effect="dark" :content="item.remark" placement="top-start"
                    v-if="item.remark">
          <i style="float: right;margin-right: 4rem;line-height: inherit;" class="el-icon-question"></i>
        </el-tooltip>
      </template>

      <template slot="outputFormatDescLabel" slot-scope="scope">
        <span>输出格式说明&nbsp;&nbsp;</span>
        <el-tooltip class="item" effect="dark" content="如果对输出格式没有特别要求请保留默认，如果需要落库或有其他特别格式要求，请务必在此明确描述" placement="top-start">
          <i class="el-icon-warning"></i>
        </el-tooltip>
      </template>

      <template slot="promptLabel" slot-scope="scope">
        <span>要求AI的处理逻辑&nbsp;&nbsp;</span>
        <el-tooltip class="item" effect="dark" content="如果对处理逻辑没有特别要求请保留默认，如果有特别的逻辑处理要求，尽可能严谨清晰的描述清楚，这是AI能力发挥的关键" placement="top-start">
          <i class="el-icon-warning"></i>
        </el-tooltip>
      </template>




        <!-- AI物资列表 -->
        <template #detailListForm="{ row,size,type }">
          <avue-crud
            :option="detailOption"
            :data="detailData"
            v-model="detailForm"
            ref="crudDetail"
            @cell-click="rowViewDetail"
            @cell-dblclick="editListDetail"
            @row-save="rowSaveDetail"
            @row-update="rowUpdateDetail"
            @selection-change="selectionChangeDetail"
            >
            <template #menu-left v-if="type !== 'view'">
              <el-button plain circle size="small" title="新增" icon="el-icon-plus" @click="handleAddDetail()"/>
              <el-button plain circle size="small" title="删除" icon="el-icon-delete" @click="handleDeleteDetail()"/>
            </template>

            <template #menu-right >
              <el-button plain circle size="small" title="预览" icon="el-icon-view" @click="preView()"/>
            </template>





            <template #dataTypeType="{ item,value,label }">
              &nbsp;&nbsp;{{ item.dictValue }}&nbsp;&nbsp;
              <el-tooltip class="item" effect="dark" :content="item.remark" placement="top-start"
                          v-if="item.remark">
                <i style="float: right;margin-right: 4rem;line-height: inherit;" class="el-icon-question"></i>
              </el-tooltip>
            </template>


            <template #dataStructureType="{ item,value,label }">
              &nbsp;&nbsp;{{ item.dictValue }}&nbsp;&nbsp;
              <el-tooltip class="item" effect="dark" :content="item.remark" placement="top-start"
                          v-if="item.remark">
                <i style="float: right;margin-right: 4rem;line-height: inherit;" class="el-icon-question"></i>
              </el-tooltip>
            </template>

            <!-- DetailCrud 字段：label + header tooltip -->
            <template slot="dataTypeLabel" slot-scope="scope">
              <span>数据分类&nbsp;&nbsp;</span>
              <el-tooltip class="item" effect="dark" content="构成背景知识要素，用于ai的理解识别" placement="top-start">
                <i class="el-icon-warning"></i>
              </el-tooltip>
            </template>
            <template #dataTypeHeader="{  column  }">
              <span>{{ (column || {}).label }}</span>
              <el-popover trigger="hover" placement="top">
                <el-row>构成背景知识要素，用于ai的理解识别</el-row>
                <template #reference><el-icon class="el-icon-info" /></template>
              </el-popover>
            </template>

            <template slot="dataNameLabel" slot-scope="scope">
              <span>数据类名称&nbsp;&nbsp;</span>
              <el-tooltip class="item" effect="dark" content="构成背景知识要素，给出通俗的称谓用于ai的理解识别" placement="top-start">
                <i class="el-icon-warning"></i>
              </el-tooltip>
            </template>
            <template #dataNameHeader="{  column  }">
              <span>{{ (column || {}).label }}</span>
              <el-popover trigger="hover" placement="top">
                <el-row>构成背景知识要素，给出通俗的称谓用于ai的理解识别</el-row>
                <template #reference><el-icon class="el-icon-info" /></template>
              </el-popover>
            </template>

            <template slot="dataDescLabel" slot-scope="scope">
              <span>数据类说明&nbsp;&nbsp;</span>
              <el-tooltip class="item" effect="dark" content="构成背景知识要素，通过的数据类名称的解释用于ai的理解识别" placement="top-start">
                <i class="el-icon-warning"></i>
              </el-tooltip>
            </template>
            <template #dataDescHeader="{  column  }">
              <span>{{ (column || {}).label }}</span>
              <el-popover trigger="hover" placement="top">
                <el-row>构成背景知识要素，通过的数据类名称的解释用于ai的理解识别</el-row>
                <template #reference><el-icon class="el-icon-info" /></template>
              </el-popover>
            </template>

            <template slot="dataStructureLabel" slot-scope="scope">
              <span>数据类结构&nbsp;&nbsp;</span>
              <el-tooltip class="item" effect="dark" content="构成背景知识要素，要求由按钮的开发人员根据数据分类中的库表数据和对应的筛选条件按照本结构生成后填入背景知识用于ai的的理解识别" placement="top-start">
                <i class="el-icon-warning"></i>
              </el-tooltip>
            </template>
            <template #dataStructureHeader="{  column  }">
              <span>{{ (column || {}).label }}</span>
              <el-popover trigger="hover" placement="top">
                <el-row>构成背景知识要素，要求由按钮的开发人员根据数据分类中的库表数据和对应的筛选条件按照本结构生成后填入背景知识用于ai的的理解识别</el-row>
                <template #reference><el-icon class="el-icon-info" /></template>
              </el-popover>
            </template>

            <template slot="specificDescriptionLabel" slot-scope="scope">
              <span>具体描述&nbsp;&nbsp;</span>
              <el-tooltip class="item" effect="dark" content="构成背景知识要素，针对数据类分类中库表数据可以给出字段的注释及进一步解释和相关含义的描述，针对文本数据给出具体的文本内容用于ai的理解识别" placement="top-start">
                <i class="el-icon-warning"></i>
              </el-tooltip>
            </template>
            <template #specificDescriptionHeader="{  column  }">
              <span>{{ (column || {}).label }}</span>
              <el-popover trigger="hover" placement="top">
                <el-row>构成背景知识要素，针对数据类分类中库表数据可以给出字段的注释及进一步解释和相关含义的描述，针对文本数据给出具体的文本内容用于ai的理解识别</el-row>
                <template #reference><el-icon class="el-icon-info" /></template>
              </el-popover>
            </template>

            <template slot="tableNameLabel" slot-scope="scope">
              <span>表名&nbsp;&nbsp;</span>
              <el-tooltip class="item" effect="dark" content="由具体业务配置人员给出，用于按钮开发人员根据本条数据的具体要求选择的表名，本列内容不作为构成背景知识的要素" placement="top-start">
                <i class="el-icon-warning"></i>
              </el-tooltip>
            </template>
            <template #tableNameHeader="{  column  }">
              <span>{{ (column || {}).label }}</span>
              <el-popover trigger="hover" placement="top">
                <el-row>由具体业务配置人员给出，用于按钮开发人员根据本条数据的具体要求选择的表名，本列内容不作为构成背景知识的要素</el-row>
                <template #reference><el-icon class="el-icon-info" /></template>
              </el-popover>
            </template>

            <template slot="fieldNameLabel" slot-scope="scope">
              <span>字段&nbsp;&nbsp;</span>
              <el-tooltip class="item" effect="dark" content="由具体业务配置人员给出，用于按钮开发人员针对表名给出的对应数据库表的具体字段，本列内容不作为构成背景知识的要素" placement="top-start">
                <i class="el-icon-warning"></i>
              </el-tooltip>
            </template>
            <template #fieldNameHeader="{  column  }">
              <span>{{ (column || {}).label }}</span>
              <el-popover trigger="hover" placement="top">
                <el-row>由具体业务配置人员给出，用于按钮开发人员针对表名给出的对应数据库表的具体字段，本列内容不作为构成背景知识的要素</el-row>
                <template #reference><el-icon class="el-icon-info" /></template>
              </el-popover>
            </template>

            <template slot="filterConditionsLabel" slot-scope="scope">
              <span>数据过滤条件&nbsp;&nbsp;</span>
              <el-tooltip class="item" effect="dark" content="由具体业务配置人员给出，用于按钮开发人员针对给出的数据库表进行对应的数据的筛选，本列内容不作为构成背景知识的要素" placement="top-start">
                <i class="el-icon-warning"></i>
              </el-tooltip>
            </template>
            <template #filterConditionsHeader="{  column  }">
              <span>{{ (column || {}).label }}</span>
              <el-popover trigger="hover" placement="top">
                <el-row>由具体业务配置人员给出，用于按钮开发人员针对给出的数据库表进行对应的数据的筛选，本列内容不作为构成背景知识的要素</el-row>
                <template #reference><el-icon class="el-icon-info" /></template>
              </el-popover>
            </template>

            <template #index="{row,index}">
              <div>{{index+1}}</div>
            </template>

            <template #menu-form="{ size, type }">

              <template v-if="type !== 'view'">
              <el-button circle icon="el-icon-check" :size="size" @click="$refs.crudDetail.rowSave()"/>
              <el-button circle icon="el-icon-close" :size="size" @click="$refs.crudDetail.resetForm()"/>
              </template>

            </template>
          </avue-crud>
        </template>

        <template #menu-form="{ row,index,type }">

          <template v-if="type !== 'view'">
            <el-button circle icon="el-icon-check" size="small" title="保存" @click="$refs.crud.rowSave()" ></el-button>
            <el-button circle icon="el-icon-close" size="small" title="取消" @click="$refs.crud.closeDialog()"></el-button>
          </template>

        </template>
    </avue-crud>
    <!-- 左下按钮 -->
    <page-attach ref="pageAttach"></page-attach>
    <ai-btn-chat-dialog
      ref="aiBtnChatDialog"
      @apply-structured-json="onApplyAiStructuredJson"
    />

    <el-dialog
      title="预览"
      v-model="previewDialogVisible"
      width="70%"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-input
        type="textarea"
        :rows="18"
        v-model="previewContent"
        readonly
        show-word-limit
        :maxlength="200000"
      />
      <template #footer><span class="dialog-footer">
<!--        <el-button size="small" @click="handleCopyPreview">复制</el-button>-->
        <el-button size="small" type="primary" @click="previewDialogVisible = false">关闭</el-button>
      </span></template>
    </el-dialog>
  </basic-container>
</template>

<script>
  import pageAttach from "@/components/attach-dialog/page-attach/main.vue"
  import rowAttach from "@/components/attach-dialog/row-attach/main.vue"
  import AiBtnChatDialog from "@/components/ai-btn-chat-dialog/main.vue"
  import {getList, getDetail, add, update, remove} from "./api/btnConfig";
  import {mapGetters} from "vuex";
  import 'nprogress/nprogress.css';
  import optionFunc from "@/utils/option";
  import func from "@/utils/func";
  import _ from 'lodash'
  import { parseValidatedOutputFormatJsonSchema } from '@/utils/outputFormatJsonSchema'

  export default {
    components: {
     pageAttach, rowAttach, AiBtnChatDialog
    },
    data() {
      return {
        form: {},
        query: {
          ascs: '',
          descs: 'update_time'
        },
        search: {},
        loading: true,
        page: {
          pageSize: 30,
          currentPage: 1,
          total: 0
        },
        selectionList: [],
        option: {
          searchShowBtn:false,
          saveBtn: false,
          updateBtn: false,
          cancelBtn: false,
          columnBtn: false,
          gridBtn:false,
          menu: false,
          addBtn: false,
          delBtn: false,
          editBtn: false,
          dialogDrag: true,
          searchBtn: false,
          emptyBtn: false,
          height:'auto',
          calcHeight: 30,
          tip: false,
          searchShow: true,
          searchMenuSpan: 6,
          border: true,
          viewBtn: true,
          selection: true,
          dialogClickModal: false,
          labelWidth: 140,
          searchLabelWidth: 130,
          column: [
            {
              label:'序号',
              prop:'index',
              fixed:true,
              width: 70,
              display: false,
              align: "center"
            },
            {
              label: "ID",
              prop: "id",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "按钮名称",
              prop: "btnName",
              type: "input",
              search: true,
              rules: [{
                required: true,
                message: "请输入按钮名称",
                trigger: "blur",
              }],
            },
            {
              label: "按钮编号",
              prop: "btnNo",
              type: "input",
              rules: [{
                required: true,
                message: "请输入按钮编号",
                trigger: "blur",
              }],
              disabled: true,
            },
            {
              label: "页面路径",
              prop: "pagePaths",
              type: "tree",
              dicMethod: "post",
              dicUrl: "/api/blade-system/menu/tree",
              multiple: true,
              dataType: "string",
              parent: false,
              checkStrictly: true,
              leafOnly: true,
              props: {
                label: "title"
              },
              rules: [{
                required: true,
                message: "请选择页面路径",
                trigger: "blur",
              }],
              overHidden: true
            },
            {
              label: "按钮说明",
              prop: "btnDesc",
              type: "input",
            },
            {
              label: "AI接口地址",
              prop: "aiInterfaceUrl",
              type: "input",
              rules: [{
                required: true,
                message: "请输入AI接口地址",
                trigger: "blur",
              }],
              overHidden: true
            },
            {
              label: "密钥",
              prop: "secretKey",
              type: "input",
              overHidden: true
            },
            {
              label: "AI按钮类型",
              prop: "btnType",
              type: "select",
              dicMethod: "post",
              dicUrl: "/api/blade-system/dict-biz/dictionary?code=btn_type",
              props: {
                label: 'dictValue',
                value: 'dictKey'
              },
              search: true,
              rules: [
                {
                  required: true,
                  message: "请选择AI按钮类型",
                  trigger: "change"
                }
              ],
            },
            {
              label: "中台交互类型",
              prop: "interactionType",
              type: "select",
              dicMethod: "post",
              dicUrl: "/api/blade-system/dict-biz/dictionary?code=interaction_type",
              props: {
                label: 'dictValue',
                value: 'dictKey'
              },
              rules: [
                {
                  required: true,
                  message: "请选择中台交互类型",
                  trigger: "change"
                }
              ],
            },
            {
              label: "字段输出格式要求",
              prop: "outputFormatRequirement",
              type: "textarea",
              rules: [{
                required: true,
                message: "请输入字段输出格式要求",
                trigger: "blur",
              }, {
                validator: (rule, value, callback) => {
                  const bt = this.form && this.form.btnType;
                  if (bt !== 'btn_type_02' && bt !== 'btn_type_03') {
                    callback();
                    return;
                  }
                  const v = parseValidatedOutputFormatJsonSchema(value);
                  if (!v.ok) {
                    callback(new Error(v.message));
                    return;
                  }
                  callback();
                },
                trigger: "blur",
              }],
              placeholder: "填写完整 JsonSchema（合法 JSON 对象文本）；转发时原样传入，拆分由接口处理",
              span: 24,
              rows:3,
              maxlength: 5000,
              showWordLimit: true,
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true
            },
            {
              label: "输出格式说明",
              prop: "outputFormatDesc",
              type: "textarea",
              rules: [{
                required: true,
                message: "请输入输出格式说明",
                trigger: "blur",
              }],
              span: 24,
              rows:3,
              maxlength: 5000,
              showWordLimit: true,
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true
            },
            {
              label: "要求AI处理逻辑",
              prop: "prompt",
              type: "textarea",
              labelWidth:160,
              span: 24,
              rows:3,
              maxlength: 500,
              showWordLimit: true,
              overHidden: true
            },
            {
              label: "引导回复",
              prop: "guideReply",
              type: "textarea",
              span: 24,
              rows: 2,
              maxlength: 500,
              showWordLimit: true,
              placeholder: "配置后，AI 对话弹窗输入框将显示此占位提示；留空则使用默认提示",
              overHidden: true
            },
            {
              label: "背景资料数据明细",
              prop: "detailList",
              type: "input",
              slot: true,
              span:24,
              hide: true
            },
            {
              label: "创建人",
              prop: "createUser",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "创建部门",
              prop: "createDept",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "创建时间",
              prop: "createTime",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "更新人",
              prop: "updateUserName",
              type: "input",
              width: 100,
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
            },
            {
              label: "更新时间",
              prop: "updateTime",
              type: "input",
              width: 140,
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              sortable: 'custom',
            },
            {
              label: "状态（1:启用，0:禁用）默认:1",
              prop: "status",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "是否已删除",
              prop: "isDeleted",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: '附件列表',
              span: 24,
              width: "100",
              prop: 'uploadList',
              slot: true,
              display: false,
              hide: true,
            },
          ]
        },
        data: [],
        timer: null,
        detailOption: {
          searchShowBtn:false,
          saveBtn: false,
          updateBtn: false,
          cancelBtn: false,
          menu: false,
          addBtn: false,
          delBtn: false,
          editBtn: false,
          gridBtn:false,
          dialogDrag: true,
          searchBtn: false,
          emptyBtn: false,
          emptyText: "数据为空",
          refreshBtn: false, // 表格刷新数据按钮
          columnBtn: false, // 表格列操作
          // height:'auto',
          calcHeight: 30,
          tip: false,
          searchShow: true,
          searchMenuSpan: 6,
          border: true,
          viewBtn: true,
          selection: true,
          dialogClickModal: false,
          labelWidth: 130,
          column: [
            {
              label:'序号',
              prop:'index',
              fixed:true,
              width: 70,
              display: false,
              align: "center"
            },
            {
              label: "ID",
              prop: "id",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },





            {
              label: '背景知识要素',
              headerAlign:"center",
              children: [
                {
                label: "按钮配置ID",
                prop: "btnConfigId",
                type: "input",
                addDisplay: false,
                editDisplay: false,
                viewDisplay: false,
                hide: true,
              },
                {
                  label: "数据分类",
                  prop: "dataType",
                  type: "select",
                  width: 100,
                  dicMethod: "post",
                  dicUrl: "/api/blade-system/dict-biz/dictionary?code=data_type",
                  props: {
                    label: 'dictValue',
                    value: 'dictKey'
                  },
                  change: this.dataTypeChange,
                  rules: [
                    {
                      required: true,
                      message: "请选择AI按钮类型",
                      trigger: "change"
                    }
                  ],
                },
                {
                  label: "数据类名称",
                  prop: "dataName",
                  type: "input",
                  rules: [{
                    required: true,
                    message: "请输入数据类名称",
                    trigger: "blur",
                  }],
                },
                {
                  label: "数据类说明",
                  prop: "dataDesc",
                  type: "input",
                  rules: [{
                    required: true,
                    message: "请输入数据类说明",
                    trigger: "blur",
                  }],
                  overHidden: true
                },
                {
                  label: "数据类结构",
                  prop: "dataStructure",
                  type: "select",
                  width: 100,
                  dicMethod: "post",
                  dicUrl: "/api/blade-system/dict-biz/dictionary?code=data_structure",
                  props: {
                    label: 'dictValue',
                    value: 'dictKey'
                  },
                  rules: [
                    {
                      required: true,
                      message: "请选择AI按钮类型",
                      trigger: "change"
                    }
                  ],
                },
                {
                  label: "具体描述",
                  prop: "specificDescription",
                  type: "textarea",
                  rules: [{
                    required: true,
                    message: "请输入具体描述",
                    trigger: "blur",
                  }],
                  span: 24,
                  rows:3,
                  maxlength: 5000,
                  showWordLimit: true,
                  overHidden: true
                },
                {
                  label: "具体数据",
                  prop: "mockData",
                  type: "textarea",
                  span: 24,
                  rows: 3,
                  maxlength: 2000,
                  showWordLimit: true,
                  overHidden: true
                },]
            },

            {
              label: '数据处理要求',
              headerAlign:"center",
              children: [
                {
                label: "表名",
                prop: "tableName",
                type: "input",
                rules: [{
                  required: true,
                  message: "请输入表名",
                  trigger: "blur",
                }],
                  row:true,
                  span:24,
                overHidden: true,
                display:false
              },
                {
                  label: "字段名",
                  prop: "fieldName",
                  type: "input",
                  rules: [{
                    required: true,
                    message: "请输入字段名",
                    trigger: "blur",
                  }],
                  row:true,
                  span:24,
                  overHidden: true,
                  display:false
                },

                {
                  label: "数据过滤条件",
                  prop: "filterConditions",
                  type: "textarea",
                  rules: [{
                    required: true,
                    message: "请输入数据过滤条件",
                    trigger: "blur",
                  }],
                  span: 24,
                  rows:3,
                  maxlength: 2000,
                  showWordLimit: true,
                  overHidden: true,
                  display:false
                },]
            },


            {
              label: "创建人",
              prop: "createUser",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "创建部门",
              prop: "createDept",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "创建时间",
              prop: "createTime",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "修改人",
              prop: "updateUser",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "修改时间",
              prop: "updateTime",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "状态（1:启用，0:禁用）默认:1",
              prop: "status",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: "是否已删除",
              prop: "isDeleted",
              type: "input",
              addDisplay: false,
              editDisplay: false,
              viewDisplay: false,
              hide: true,
            },
            {
              label: '附件列表',
              span: 24,
              width: "100",
              prop: 'uploadList',
              slot: true,
              display: false,
              hide: true,
            },
          ]
        },
        detailData:[],
        detailForm: {},
        selectionDetailList: [],
        previewDialogVisible: false,
        previewContent: "",
      };
    },
    computed: {
      ...mapGetters(["permission"]),
      permissionList() {
        return {
          addBtn: this.vaildData(this.permission.btnConfig_add, false),
          viewBtn: this.vaildData(this.permission.btnConfig_view, false),
          delBtn: this.vaildData(this.permission.btnConfig_delete, false),
          editBtn: this.vaildData(this.permission.btnConfig_edit, false)
        };
      },
      ids() {
        let ids = [];
        this.selectionList.forEach(ele => {
          ids.push(ele.id);
        });
        return ids.join(",");
      }
    },
    watch: {
      //监听AI按钮类型
      'form.btnType'(val) {
        this.watchBtnType(val)
      },

    },

    methods: {
      dataTypeChange(e) {

        const value = e.value
        const tableNameColumn = this.findObject(this.detailOption.column, "tableName");
        const fieldNameColumn = this.findObject(this.detailOption.column, "fieldName");
        const filterConditionsColumn = this.findObject(this.detailOption.column, "filterConditions");
        if (value === "data_type_01") {
          //文本类型
          tableNameColumn.display =false
          fieldNameColumn.display = false
          filterConditionsColumn.display = false
        }else if (value === "data_type_02") {
          //数据库表
          tableNameColumn.display =true
          fieldNameColumn.display = true
          filterConditionsColumn.display = true
        }
      },
      preView() {
        if (!Array.isArray(this.detailData) || this.detailData.length === 0) {
          this.$message.warning("背景资料数据明细为空，无法预览");
          return;
        }
        this.previewContent = this.buildPreviewContent(
          this.detailData,
          this.form.prompt,
          this.form.outputFormatRequirement,
          this.form.outputFormatDesc
        );
        this.previewDialogVisible = true;
      },

      openAiChatDialog() {
        if (this.selectionList.length === 0) {
          this.$message.warning("请选择至少一条数据");
          return;
        }
        const row = this.selectionList[0];
        const btnNo = row.btnNo != null ? String(row.btnNo).trim() : "";
        if (btnNo) {
          this.$refs.aiBtnChatDialog.open({ btnNo });
        } else {
          this.$refs.aiBtnChatDialog.open({ id: row.id });
        }
      },
      onApplyAiStructuredJson(jsonData) {
        console.log(
          "[btnConfig] apply-structured-json",
          JSON.stringify(jsonData, null, 2)
        );
      },
      buildPreviewContent(list, prompt, outputFormatRequirement,outputFormatDesc) {
        const safe = (v) => (v === null || v === undefined ? "" : String(v));
        const nonEmpty = (v) => {
          const s = safe(v).trim();
          return s ? s : "";
        };
        const blocks = list.map((item) => {
          const md = safe(item.mockData).trim();
          const mdPart = md ? `；mockData：${md}` : "";
          return `【名称：${safe(item.dataName)}；说明：${safe(item.dataDesc)}；给出的是：${safe(item.$dataStructure)}：具体描述如下：${safe(item.specificDescription)}${mdPart}】`;
        });

        const background = `背景资料包括：\n${blocks.join("\n")}`;
        const p = nonEmpty(prompt);
        const o = nonEmpty(outputFormatRequirement);
        const op = nonEmpty(outputFormatDesc);

        const extraParts = [];
        if (p) {
          extraParts.push(`结合上述背景资料，你需要按如下要求进行处理：\n【${p}】`);
        }
        if (o) {
          extraParts.push(`你需要按照如下格式进行输出：\n【${o}】`);
        }
        if (op) {
          extraParts.push(`输出格式说明：\n【${op}】`);
        }

        if (!extraParts.length) return background;

        // 注意：背景资料与后续要求之间需要四个换行
        return background + "\n" + extraParts.join("\n");
      },

      async handleCopyPreview() {
        const text = this.previewContent || "";
        if (!text) {
          this.$message.warning("预览内容为空");
          return;
        }
        try {
          await navigator.clipboard.writeText(text);
          this.$message.success("已复制到剪贴板");
        } catch (e) {
          const textarea = document.createElement("textarea");
          textarea.value = text;
          textarea.setAttribute("readonly", "");
          textarea.style.position = "absolute";
          textarea.style.left = "-9999px";
          document.body.appendChild(textarea);
          textarea.select();
          const ok = document.execCommand("copy");
          document.body.removeChild(textarea);
          if (ok) this.$message.success("已复制到剪贴板");
          else this.$message.error("复制失败，请手动复制");
        }
      },

      addList() {
        this.$refs.crud.rowAdd()
        this.form = {
          btnNo: this.genRandomCode(),
          prompt: "你作为专业的AI自行理解自行分析处理",
          outputFormatRequirement:""
        }
        this.$refs.crud.dicInit()
      },
      copyList(){
        if (this.selectionList.length === 0) {
          this.$message.warning("请选择至少一条数据");
          return;
        }
        this.$refs.crud.rowAdd()
        this.form = this.selectionList[0]
        this.form.id = ""
        this.$refs.crud.dicInit()
      },
      editList(row,{property}) {
        if (this.permission.btnConfig_edit && property === "index") {
            clearTimeout(this.timer);
            this.$refs.crud.dicInit()
            this.$refs.crud.rowEdit(row, row.$index)
        }
      },
      rowView(row, {property}) {
        if (property === "index") {
          clearTimeout(this.timer);
          this.timer = setTimeout(() =>{
            this.$refs.crud.rowView(row, row.$index)
          },250)
        }
      },
      rowSave(row, done, loading) {
        if (row.btnType === 'btn_type_02' || row.btnType === 'btn_type_03') {
          const v = parseValidatedOutputFormatJsonSchema(row.outputFormatRequirement);
          if (!v.ok) {
            this.$message.error(v.message);
            loading();
            return;
          }
        }
        row.detailList = this.detailData;
        add(row).then(() => {
          this.onLoad(this.page);
          this.$message({
            type: "success",
            message: "操作成功!"
          });
          done();
        }, error => {
          loading();
          window.console.log(error);
        });
      },
      rowUpdate(row, index, done, loading) {
        if (row.btnType === 'btn_type_02' || row.btnType === 'btn_type_03') {
          const v = parseValidatedOutputFormatJsonSchema(row.outputFormatRequirement);
          if (!v.ok) {
            this.$message.error(v.message);
            loading();
            return;
          }
        }
        row.detailList = this.detailData;
        update(row).then(() => {
          this.onLoad(this.page);
          this.$message({
            type: "success",
            message: "操作成功!"
          });
          done();
        }, error => {
          loading();
          console.log(error);
        });
      },
      rowDel(row) {
        this.$confirm("确定将选择数据删除?", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        })
          .then(() => {
            return remove(row.id);
          })
          .then(() => {
            this.onLoad(this.page);
            this.$message({
              type: "success",
              message: "操作成功!"
            });
          });
      },
      handleDelete() {
        if (this.selectionList.length === 0) {
          this.$message.warning("请选择至少一条数据");
          return;
        }
        this.$confirm("确定将选择数据删除?", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        })
          .then(() => {
            return remove(this.ids);
          })
          .then(() => {
            this.onLoad(this.page);
            this.$message({
              type: "success",
              message: "操作成功!"
            });
            this.$refs.crud.toggleSelection();
          });
      },

      beforeOpen(done, type) {
        // 关键：弹窗打开前先把详情与明细回显完成，避免二次打开时明细为空/不刷新
        if (["edit", "view"].includes(type)) {
          const id = (this.form || {}).id;
          getDetail({ id })
            .then(res => {
              const d = (res && res.data && res.data.data) ? res.data.data : {};
              this.form = d || {};
              const list = d ? d.detailList : null;
              // 明细始终保持为数组，避免 avue-crud data 接收到 undefined/null
              this.detailData = Array.isArray(list) ? list : [];
            })
            .finally(() => {
              done();
            });
          return;
        }
        // 新增/复制等场景：清空明细，避免脏数据串场
        this.detailData = [];
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
      sortChange(column) {
        this.query.ascs = column.order === 'ascending' ? this.camel2Underline(column.prop) : '';
        this.query.descs = column.order === 'descending' ? this.camel2Underline(column.prop) : '';
        this.onLoad(this.page, this.query);
      },
      // 驼峰转换
      camel2Underline(str) {
        return str.replace(/([A-Z])/g,"_$1").toLowerCase();
      },
      selectionChange(list) {
        this.selectionList = list;
      },
      selectionClear() {
        this.selectionList = [];
        this.$refs.crud.toggleSelection();
      },
      currentChange(currentPage){
        this.page.currentPage = currentPage;
      },
      sizeChange(pageSize){
        this.page.pageSize = pageSize;
      },
      refreshChange() {
        this.onLoad(this.page, this.query);
      },
      onLoad(page) {
        this.loading = true;

        const {
          btnName,
          btnType,
          ascs,
          descs
        } = this.query;

        let values = {
          btnName,
          btnType,
          ascs,
          descs
        };

        getList(page.currentPage, page.pageSize, values).then(res => {
          const data = res.data.data;
          this.page.total = data.total;
          this.data = data.records;
          this.loading = false;
          this.selectionClear();
        });
      },
      /** 生成5位随机数 */
      genRandomCode(){
        let randomStrWithUpper = Math.random().toString(36).substring(2, 4) + Math.random().toString(36).substring(2, 5).toUpperCase();
        return randomStrWithUpper.substring(0, 5);
      },

      handleAddDetail() {
        const tableNameColumn = this.findObject(this.detailOption.column, "tableName");
        const fieldNameColumn = this.findObject(this.detailOption.column, "fieldName");
        const filterConditionsColumn = this.findObject(this.detailOption.column, "filterConditions");

          tableNameColumn.display =false
          fieldNameColumn.display = false
          filterConditionsColumn.display = false


        this.$refs.crudDetail.rowAdd()
        this.$refs.crudDetail.dicInit()
      },

      selectionChangeDetail(list) {
        this.selectionDetailList = list;
      },

      editListDetail(row,{property}) {
        if (property === "index") {
          clearTimeout(this.timer);
          this.$refs.crudDetail.dicInit()
          this.$refs.crudDetail.rowEdit(row, row.$index)
        }
      },

      rowViewDetail(row, {property}) {
        if (property === "index") {
          clearTimeout(this.timer);
          this.timer = setTimeout(() =>{
            this.$refs.crudDetail.rowView(row, row.$index)
          },250)
        }
      },

      //背景资料列表保存
      rowSaveDetail(row, loading, done) {
        loading();
        //判断数据项是否重复
        let flag = this.detailData.some(item => item.dataName == row.dataName);
        if(flag){
          this.$message.warning("已存在，请勿重复添加");
          return;
        }else{
          this.detailData.push(row);
        }
        done()
      },

      //背景资料列表修改
      rowUpdateDetail(row, index, done, loading){
        loading();
        //判断数据项是否重复
        let existList = this.detailData.filter(item=>item.dataName == row.dataName)
        if(existList.length > 0 && existList[0].$index != index){
          this.$message.warning("已存在，请勿重复添加");
          return;
        }else{
          this.detailData[index] = row;
        }
        done()
      },

      handleDeleteDetail(){
        if (this.selectionDetailList.length === 0) {
          this.$message.warning("请选择至少一条数据");
          return;
        }
        this.$confirm("确定将选择数据删除?", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        })
          .then(() => {
            this.selectionDetailList.forEach(selection => {
              this.detailData = _.remove(this.detailData, item => {
                return !_.isEqual(item, selection)
              })
            })
            this.$refs.crudDetail.toggleSelection();
          })
      },

       /** 监听AI按钮类型 */
      watchBtnType(val){
        if(func.notEmpty(val)){
          if(val == 'btn_type_02' || val == 'btn_type_03'){
            optionFunc.showFormField(this, this.option, 'outputFormatRequirement');
            optionFunc.showFormField(this, this.option, 'outputFormatDesc');
          }else{
            optionFunc.hideFormField(this, this.option, 'outputFormatRequirement');
            optionFunc.hideFormField(this, this.option, 'outputFormatDesc');
           // this.form.outputFormatRequirement='';
            this.form.outputFormatDesc=''
          }
        }else{
          optionFunc.hideFormField(this, this.option, 'outputFormatRequirement');
          optionFunc.hideFormField(this, this.option, 'outputFormatDesc');
          //this.form.outputFormatRequirement='';
          this.form.outputFormatDesc=''
        }
      },
    }
  };
</script>


