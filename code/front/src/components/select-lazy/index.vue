<template>
  <div>
    <el-select v-el-select-loadmore="loadMore" :value="defaultValue" :loading="loading" :multiple="multiple"
               :placeholder="placeholder" :allow-create="allowCreate" filterable remote clearable
               :remote-method="(query) => {remoteMethod(query, value)}" style="width: 100%;" @change="change"
               @input="$emit('input',$event)" @visible-change="visibleChange" @clear="clearChange">
      <el-option v-if="hasAll" :label="defaultLabel" value="" />
      <el-option v-for="(item) in optionsList" :key="item.index+'s'+item.id"
                 :label="concatString2(item[label], item[labelTwo])" :value="item[valueString]">
        {{ concatString(item[label], item[labelTwo]) }}</el-option>
    </el-select>
  </div>
</template>

<script>
export default {
  name: 'YSelect',
  directives: {
    'el-select-loadmore': {
      bind(el, binding) {
        // 获取element-ui定义好的scroll盒子
        const DOM = el.querySelector('.el-select-dropdown .el-select-dropdown__wrap')
        DOM.addEventListener('scroll', function() {
          /**
           * scrollHeight 获取元素内容高度(只读)
           * scrollTop 获取或者设置元素的偏移值,常用于, 计算滚动条的位置, 当一个元素的容器没有产生垂直方向的滚动条, 那它的scrollTop的值默认为0.
           * clientHeight 读取元素的可见高度(只读)
           * 如果元素滚动到底, 下面等式返回true, 没有则返回false:
           * ele.scrollHeight - ele.scrollTop === ele.clientHeight;
           */
          const condition = this.scrollHeight - this.scrollTop <= this.clientHeight
          if (condition) {
            binding.value()
          }
        })
      }
    }
  },
  props: {
    // 是否允许创建条目
    allowCreate: {
      type: Boolean,
      default: false
    },
    // 需要显示的名称
    label: {
      type: String,
      default: ''
    },
    // 需要显示的名称
    labelTwo: {
      type: String,
      default: ''
    },
    // 传入的数据，必填
    value: {
      type: [String, Number, Array],
      default: null
    },
    // 是否拼接label | value
    isConcat: {
      type: Boolean,
      default: false
    },
    isConcatShowText: {
      type: Boolean,
      default: false
    },
    // 拼接label、value符号
    concatSymbol: {
      type: String,
      default: ' | '
    },
    valueString: {
      type: String,
      default: ''
    },
    // 选项数据，必填
    options: {
      type: Array,
      default: () => {
        return []
      }
    },
    // 是否有全部选项
    hasAll: {
      type: Boolean,
      default: true
    },
    defaultLabel: {
      type: String,
      default: '全部'
    },
    // 加载loading
    loading: {
      type: Boolean,
      default: false
    },
    // 提示
    placeholder: {
      type: String,
      default: '请选择'
    },
    // 是否支持多选
    multiple: {
      type: Boolean,
      default: false
    },
    // 每次显示数量
    size: {
      type: Number,
      default: 30
    }
  },
  data() {
    return {
      page: 1,
      pageRemote: 1,
      defaultLoading: false,
      timer: null,
      optionsList: [],
      oldOptions: [],
      isRemote: false,
      defaultValue:null,
    }
  },
  watch: {
    options: {
      handler(val) {
        if (this.isRemote) {
          if (val) {
            this.optionsList = val
            this.oldOptions = this.oldOptions.filter((item) => {
              return !val.some(valItem => item.id === valItem.id)
            })
            this.oldOptions = [...this.oldOptions, ...val]
          }
        } else {
          if (val) {
            this.optionsList = this.optionsList.filter((item) => {
              return !val.some(valItem => item.id === valItem.id)
            })
            this.optionsList = [...this.optionsList, ...val]
          }
        }
      },
      deep: true
    },
    value: {
      handler(val) {
        this.defaultValue = this.value
        if (val==='null' || val===null || val==='undefined' || val===undefined || val===''){
          this.clearChange()
        }
      },
      immediate: false,
      deep: true
    }
  },
  mounted() {
    this.defaultValue = this.value
    this.optionsList = this.options
  },
  methods: {
    //选择后 只显示label
    //张三
    concatString(a, b) {
      a = a || ''
      b = b || ''
      if (this.isConcat) {
        // return a + ((a && b) ? ' | ' : '') + b
        return a + ((a && b) ? this.concatSymbol : '') + b
      }
      return a
    },
    //选择下拉展示时 可以展示label和labelTwo
    //123||张三
    concatString2(a, b) {
      a = a || ''
      b = b || ''
      if (this.isConcat) {
        // return a + ((a && b) ? ' | ' : '') + b
        if (this.isConcatShowText == true) {
          return a + ((a && b) ? this.concatSymbol : '') + b
        } else {
          return a
        }
      }
      return a
    },
    change(val) {
      console.log('change', val)
      this.$emit('change', val)
    },
    visibleChange(status) {
      console.log('change2', status)
      if (!status) {
        if (this.isRemote) {
          this.isRemote = false
          this.optionsList = [...this.oldOptions]
        }
      }
      this.$emit('visibleChange', status)
    },
    loadMore() {
      console.log(this.isRemote, this.pageRemote, this.page)
      if (this.isRemote) {
        if (this.pageRemote === 1) {
          this.$emit('loadMore', this.pageRemote)
          this.pageRemote++
        } else {
          this.pageRemote++
          this.$emit('loadMore', this.pageRemote)
        }
      } else {
        this.page++
        this.$emit('loadMore', this.page)
      }
    },
    remoteMethod(query) {
      debugger
      this.pageRemote = 1
      if (this.timer) {
        clearTimeout(this.timer)
        this.timer = null
      }
      this.timer = setTimeout(() => {
        this.isRemote = true
        this.oldOptions = [...this.optionsList]
        this.optionsList = []
        this.$emit('remoteMethod', query, this.pageRemote)
      }, 500)
    },
    //清除
    clearChange() {
      if (typeof this.defaultValue === 'string') {
        this.defaultValue = ''
      } else if (this.isMultiple) {
        this.defaultValue = []
      }
      this.$emit('clear')
    }
  }
}
</script>

<style lang='scss' scoped>

</style>


