import func from '@/utils/func';

/**
 * option相关工具类
 */
export default class optionFunc {
  static findColumn(vm, optionData, columName) {
    const column = vm.findObject(optionData.column, columName);
    if (func.notEmpty(column)) {
      return column;
    }
    return null;
  }

  static operateColumn(vm, optionData, columName, callable) {
    const column = this.findColumn(vm, optionData, columName);
    if (func.notEmpty(column)) {
      callable(column);
    }
  }

  static hideTableColumn(vm, optionData, columName) {
    this.operateColumn(vm, optionData, columName, column => {
      column.hide = true;
    });
  }

  static hideFormField(vm, optionData, columName) {
    this.operateColumn(vm, optionData, columName, column => {
      column.display = false;
      column.addDisplay = false;
      column.editDisplay = false;
    });
  }

  static hideFormFieldSearch(vm, optionData, columName) {
    this.operateColumn(vm, optionData, columName, column => {
      column.search = false;
    });
  }

  static showTableColumn(vm, optionData, columName) {
    this.operateColumn(vm, optionData, columName, column => {
      column.hide = false;
    });
  }

  static showFormField(vm, optionData, columName) {
    this.operateColumn(vm, optionData, columName, column => {
      column.display = true;
      column.addDisplay = true;
      column.editDisplay = true;
    });
  }

  static showFormFieldSearch(vm, optionData, columName) {
    this.operateColumn(vm, optionData, columName, column => {
      column.search = true;
    });
  }

  static disableFormField(vm, optionData, columName) {
    this.operateColumn(vm, optionData, columName, column => {
      column.disabled = true;
    });
  }

  static enableFormField(vm, optionData, columName) {
    this.operateColumn(vm, optionData, columName, column => {
      column.disabled = false;
    });
  }
}
