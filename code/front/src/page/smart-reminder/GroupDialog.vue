<template>
  <el-dialog
    :model-value="open"
    :title="title"
    :width="variant === 'group-compact' ? 'min(88vw,338px)' : 'min(92vw,460px)'"
    align-center
    append-to-body
    class="group-dialog"
    :class="{ 'group-dialog-compact': variant === 'group-compact' }"
    :show-close="!busy"
    :close-on-click-modal="!busy"
    :close-on-press-escape="!busy"
    @update:model-value="!busy && $emit('close')"
  >
    <div class="group-dialog-body"><slot /></div>
    <template v-if="fixedFooter" #footer><slot name="footer" /></template>
  </el-dialog>
</template>
<script setup>
defineProps({
  open: Boolean,
  title: String,
  busy: Boolean,
  fixedFooter: Boolean,
  panelHeight: Number,
  variant: String,
});
defineEmits(['close']);
</script>
<style>
.group-dialog.el-dialog {
  padding: 20px 20px 18px;
  border-radius: 27px;
  background: linear-gradient(145deg, #fff, #f9fcff);
  box-shadow: 0 20px 75px #14265730;
  color: #132044;
  max-height: calc(100dvh - 36px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
}
.group-dialog .el-dialog__header {
  padding: 0 42px 18px 0;
  flex-shrink: 0;
}
.group-dialog .el-dialog__title {
  font-size: 21px;
  font-weight: 700;
  line-height: 32px;
  color: #132044;
}
.group-dialog .el-dialog__headerbtn {
  right: 0;
  top: 0;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #edf2ff;
}
.group-dialog .el-dialog__headerbtn .el-dialog__close {
  font-size: 22px;
  color: #7e8bab;
}
.group-dialog .el-dialog__body {
  padding: 0;
  overflow-y: auto;
  min-height: 0;
  overscroll-behavior: contain;
}
.group-dialog .el-dialog__footer {
  padding-top: 14px;
  padding-bottom: 0;
  flex-shrink: 0;
}
.group-dialog-body div {
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.group-dialog-body span {
  display: block;
}
.group-dialog button,
.group-dialog input,
.group-dialog textarea {
  font: inherit;
  box-sizing: border-box;
}
.group-dialog button {
  cursor: pointer;
}
.group-dialog button:disabled {
  cursor: default;
  opacity: 0.55;
}
.group-dialog .form-actions {
  display: flex;
  flex-direction: row;
  gap: 10px;
}
.group-dialog .form-button {
  flex: 1;
  height: 44px;
  min-height: 44px;
  line-height: 44px;
  padding: 0;
  margin: 0;
  border: 0;
  border-radius: 14px;
  font-size: 16px;
  text-align: center;
}
.group-dialog .primary {
  color: #fff;
  background: linear-gradient(110deg, #0867ff, #4e7aff);
}
.group-dialog .secondary {
  color: #52617e;
  background: #eaf0ff;
}
.group-dialog .error {
  font-size: 12px;
  color: #ea6576;
  margin-top: 10px;
}
.group-dialog input,
.group-dialog textarea {
  outline: none;
}
.group-dialog input:focus-visible,
.group-dialog textarea:focus-visible {
  border-color: #6d9aff;
}
.group-dialog .ui-icon {
  flex-shrink: 0;
}
.group-dialog .input {
  border: 1px solid #dce4ff;
  border-radius: 10px;
  padding: 10px;
  width: 100%;
  font-size: 13px;
}
.group-dialog .danger {
  background: #fff0f2;
  border: 0;
  border-radius: 12px;
  color: #f65d71;
  padding: 10px;
  margin-top: 10px;
}
.group-dialog .tag-pill {
  cursor: pointer;
}
.group-dialog .picker-row {
  cursor: pointer;
}
@media (max-width: 360px) {
  .group-dialog.el-dialog {
    padding: 17px 14px 15px;
    width: 94vw;
  }
  .group-dialog .el-dialog__title {
    font-size: 19px;
  }
}
.group-dialog-compact.el-dialog {
  width: min(88vw, 338px) !important;
  padding: 17px 16px 15px;
  max-height: min(80dvh, 456px);
  border-radius: 23px;
}
.group-dialog-compact .el-dialog__header {
  padding-bottom: 12px;
}
.group-dialog-compact .el-dialog__title {
  font-size: 19px;
}
.group-dialog-compact .el-dialog__footer {
  padding-top: 10px;
}
.group-dialog-compact .form-button {
  height: 40px;
  min-height: 40px;
  line-height: 40px;
  font-size: 14px;
}
.group-dialog .form-button.primary {
  background: linear-gradient(115deg, #2866ed, #4b7ff5);
  color: #fff;
}
.group-dialog .form-button.secondary {
  background: #edf2fc;
  color: #64769d;
  font-weight: 600;
}
</style>
