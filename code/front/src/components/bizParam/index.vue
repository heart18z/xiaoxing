<template>
    <div :style="parentStyle">
        <template v-if="type === 'select'">
            <!-- 下拉框类型 -->
            <el-select ref="target" :size="size" v-model="tmpValue" :disabled="tmpDisabled" :placeholder="placeholder" :style="css" @change="onChange">
                <el-option v-for="(item, index) in tmpOptions" :key="index" :label="item.label" :value="item.value" :disabled="item.disabled"></el-option>
<!--                <el-option v-if="custom" label="自定义" :value="customValue"></el-option>-->
            </el-select>
        </template>
        <template v-if="type === 'select-multiple'">
            <!-- 多选下拉框类型 -->
            <el-select ref="target" :size="size" v-model="tmpArrValue" multiple :disabled="tmpDisabled" :placeholder="placeholder" :style="css" @change="onChange">
                <el-option v-for="(item, index) in tmpOptions" :key="index" :label="item.label" :value="item.value" :disabled="item.disabled"></el-option>
<!--                <el-option v-if="custom" label="自定义" :value="customValue"></el-option>-->
            </el-select>
        </template>
        <template v-else-if="type === 'radio'">
            <!-- 单选框类型 -->
            <el-radio-group v-model="tmpValue" @change="onChange" :size="size" :style="css" :disabled="tmpDisabled">
                <template v-if="radioBtn">
                    <el-radio-button v-for="(item, index) in tmpOptions" :key="index" :label="item.value" :disabled="item.disabled">{{item.label}}</el-radio-button>
<!--                    <el-radio-button :label="customValue">自定义</el-radio-button>-->
                </template>
                <template v-else>
                    <el-radio v-for="(item, index) in tmpOptions" :key="index" :label="item.value" :disabled="item.disabled">{{item.label}}</el-radio>
<!--                    <el-radio :label="customValue">自定义</el-radio>-->
                </template>
            </el-radio-group>
        </template>
        <template v-else-if="type === 'checkbox'">
            <!-- 多选框类型 -->
            <el-checkbox-group v-model="tmpArrValue" @change="onChange" :style="css" :disabled="tmpDisabled">
                <el-checkbox v-for="(item, index) in tmpOptions" :key="index" :label="item.label" :disabled="item.disabled" :name="item.value"></el-checkbox>
<!--                <el-checkbox :label="customValue">自定义</el-checkbox>-->
            </el-checkbox-group>
        </template>
        <template v-else-if="type === 'tree'">
            <!-- 单/多选树 类型 -->
            <el-select ref="target" :size="size" v-model="tmpArrValue" multiple :disabled="tmpDisabled" :placeholder="placeholder" :style="css" @change="onChange">
                <el-option v-for="(item, index) in treeToList(tmpOptions)" :key="index" :label="item.label" :value="item[tmpTreeProps.nodeKey]" :disabled="item.disabled" style="display:none;"></el-option>
                <el-tree
                    ref="tree"
                    :node-key="tmpTreeProps.nodeKey"
                    :disabled="tmpDisabled"
                    :show-checkbox="tmpTreeProps.multiple"
                    :accordion="tmpTreeProps.accordion"
                    :check-strictly="tmpTreeProps.checkStrictly"
                    :data="treeData"
                    :props="tmpTreeProps"
                    @node-click="checkNode"
                    @check="checkNode"
                    :default-checked-keys="tmpArrValue">
                </el-tree>
            </el-select>
        </template>
        <el-dialog v-model="showModal" title="新增自定义" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="80px">
                <el-form-item label="名称" prop="paramName">
                    <el-input v-model="form.paramName"></el-input>
                </el-form-item>
            </el-form>
            <template #footer><div class="dialog-footer">
                <el-button @click="showModal = false">取 消</el-button>
                <el-button type="primary" @click="onSubmit" :loading="saving">保 存</el-button>
            </div></template>
        </el-dialog>
    </div>
</template>

<script>
import { add } from "@/api/system/bizParam";
import { getBizParamTreeByPath } from "@/api/system/bizParam";
export default {

    props: {
        value: { // 当前选择的数据,v-model绑定, 多选框，多选下拉框、树形传入数组，其他传入非数组
            type: String | Number | Array,
            default: () => { return '' }
        },
        parent: { // 父级参数, 需包含属性：id, paramKey, $path（参数路径，点号分割）
            type: Object,
            default: () => { return {} }
        },
        options: { // 下拉选项，需包含属性：label, value, sort, disabled
            type: Array,
            default: () => { return [] }
        },
        type: { // 组件类型：select, radio, checkbox, select-multiple, tree
            type: String,
            default: () => { return 'select' },
        },
        disabled: { // 是否禁用
            type: Boolean,
            default: () => { return false; }
        },
        parentStyle: { // 最外层样式
            type: Object,
            default: () => { return {
                display: 'inline-block'
            } }
        },
        css: { // 组件样式
            type: Object,
            default: () => { return {
                width: '100%'
            } }
        },
        placeholder: { // 提示文字
            type: String,
            default: () => { return '请选择' }
        },
        size: { // 控件大小
            type: String,
            default: () => { return 'small' },
        },
        custom: { // 是否启用自定义选项
                  // 开启自定义后，用户可以自行创建一个应用参数，但权限被卡死，方便后期提炼应用字典
            type: Boolean,
            default: () => { return true },
        },

        radioBtn: { // 是否开启按钮样式，仅适用于radio类型
            type: Boolean,
            default: () => { return false; }
        },
        treeProps: { // 属性组件选项
            type: Object,
            default: () => {
                return {};
            }
        },
    },
    watch: {
        value(newVal) {
            if(newVal instanceof Array) {
                this.tmpArrValue = newVal;
            } else {
                this.tmpValue = newVal;
            }
        },
        tmpValue(newVal) {
            this.$emit("input", newVal);
        },
        tmpArrValue(newVal) {
            this.$emit("input", newVal);
            if(this.type === 'tree') {
                this.$refs['tree'].setCheckedKeys(newVal);
            }
        },
        options(newVal) {
            if(this.tmpOptions != newVal) {
                this.tmpOptions = newVal;
                if(this.type === 'tree') {
                    this.treeData = newVal;
                }
            }
        },
        tmpOptions(newVal) {
            this.$emit("update:options", newVal);
        },

        disabled(newVal) {
            this.tmpDisabled = newVal;
        },

        parent(newVal) {
            this.tmpParent = newVal;
        }
    },
    data() {
        return {
            treeToList,
            tmpValue: this.value,
            tmpArrValue: ( this.value instanceof Array ) ? this.value : [],
            tmpDisabled: this.disabled,
            tmpParent: this.parent,

            customValue: this.uuid(),
            tmpOptions: this.options,
            treeData: this.options,

            tmpTreeProps: Object.assign({
                multiple: true, // 是否开启多选
                accordion: true, // 是否开启手风琴模式
                checkStrictly: false, // 选择层级是否强制选中
                nodeKey: 'value', // v-model 同步哪个属性
            }, this.treeProps),

            saving: false,
            showModal: false,
            form: {
                id: '',
                parentId: '',
                paramName: '',
                paramKey: '',
                paramValue: '',
                sort: 0,
                remark: '',
                hierarchical: 0,
                currentHierarchy: 0,
            },
            rules: {
                paramName: [
                    { required: true, message: '请输入名称', trigger: 'blur' },
                ]
            },
        }
    },
    created() {
        if(this.type === 'tree') {
            this.tmpOptions = this.options;
            this.treeData = this.options;
        }
    },
    methods: {
        onChange(value) {
            if(value === this.customValue || ( value instanceof Array && value.includes(this.customValue)) ) {
                // 如果是选择的自定义
                this.form.paramName = '';
                this.form.parentId = this.tmpParent.id;
                this.form.paramKey = this.tmpParent.paramKey;
                this.form.hierarchical = this.tmpParent.hierarchical;
                this.form.currentHierarchy = this.tmpParent.currentHierarchy + 1;
                let sorts = this.tmpOptions ? this.tmpOptions.map(item => item.sort).sort() : [];
                this.form.sort = (sorts && sorts.length) ? (Number(sorts[sorts.length - 1]) + 1) : 0;
                this.form.paramValue = this.tmpParent.$path.replace(".", "_") + "_custom-" + this.s6();
                this.showModal = true;
                // 清空value值
                if(value instanceof Array) {
                    if(value.includes(this.customValue)) {
                        this.tmpArrValue = this.tmpArrValue.filter(item => item != this.customValue);
                    }
                } else if (value === this.customValue) {
                    this.tmpValue = '';
                }
                // 失去焦点
                if(this.$refs['target'] && this.$refs['target'].blur) {
                    this.$refs['target'].blur();
                }
            }
            let item;
            if(value instanceof Array) {
                item = this.tmpOptions.filter(item => value.contains(item.value));
            } else {
                let items = this.tmpOptions.filter(item => item.value === value);
                item = items[0];
            }
            this.$emit('change', { value: value, item: item});
        },
        s4() {
            return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
        },
        s6() {
            return (((1+Math.random())*0x1000000)|0).toString(16).substring(1);
        },
        uuid() {
            return (this.s4()+this.s4()+"-"+this.s4()+"-"+this.s4()+"-"+this.s4()+"-"+this.s4()+this.s4()+this.s4());
        },
        onSubmit() {
            this.$refs['form'].validate((valid) => {
                if (valid) {
                    this.saving = true;
                    add(this.form).then(({data}) => {
                        this.$message({ type: "success", message: "操作成功!" });
                        this.showModal = false;
                        this.tmpOptions.push({
                            id: data.data.id,
                            label: data.data.paramName,
                            value: data.data.paramValue,
                            sort: this.form.sort,
                            disabled: false,
                        });
                        if(this.value instanceof Array) {
                            this.tmpArrValue = [ data.data.paramValue ];
                        } else {
                            this.tmpValue = data.data.paramValue;
                        }
                        this.saving = false;
                    }).catch(() => {
                        this.saving = false;
                    });
                } else {
                    return false;
                }
            });
        },

        checkNode(node) {
            if(this.tmpTreeProps.multiple) {
                this.tmpArrValue = this.$refs['tree'].getCheckedKeys(!this.tmpTreeProps.checkStrictly);
            } else if(!node.children || !node.children.length) {
                this.tmpArrValue = [ node[this.tmpTreeProps.nodeKey] ];
            }

        },
    }
}

const treeToList = (treeData) => {
    let allDatas = [];
    const f = (arr) => {
        arr.forEach(item => {
            allDatas.push(item);
            if(item.children && item.children.length) {
                f(item.children);
            }
        })
    }
    f(treeData);
    return allDatas;
};

// 为防止数据多次调用，此方法应该在父组件内执行
// 参数调用路径，参数值点号分隔,顶级开始，例如：IT_Soft.dns_purpose,即开始调用dns用途下级的参数
export const loadParams = path => {
    return new Promise((resolve, reject) => {
        getBizParamTreeByPath(path).then(({ data })=>{
            if(!data.data.children) {
                data.data.children = [];
            }
            data.data.$path = path;
            const deepIn = (parent) => {
                parent.label = parent.paramName;
                parent.value = parent.paramValue;
                parent.disabled = false;
                if(parent.children) {
                    for(let item of parent.children) {
                        deepIn(item);
                    }
                }
            }
            deepIn(data.data);
            resolve({ parent: data.data, options: data.data.children });
        }).catch(reject)
    });
}

/**
 * 获取label属性
 * @param { 所有的options集合 } options
 * @param { 匹配value相等的值 } value
 */
export const getLabel = (options, value, type="select") => {
    let findObj = [];
    if(type ==='tree') {
        findObj = treeToList(options).filter(item => item.value == value);
    } else {
        findObj = options.filter(item => item.value == value);
    }
    if(findObj.length > 0) {
        return findObj[0].label;
    }
    else {
        return value;
    }
}
</script>

<style>

</style>
