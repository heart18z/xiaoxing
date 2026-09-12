import { ElMessage, ElMessageBox } from 'element-plus';
import {
  isNeedConfirmLogin,
  normalizeAxiosResponse,
  parseOAuthResponseData,
} from '@/utils/oauthLoginError';

/**
 * 登录请求：遇到互踢确认时弹窗，确认后带 confirm 头重试
 * @param {function(boolean): Promise} requestFn 传入 confirm 标志发起请求
 * @param {function(Object): void} onSuccess 登录成功后的数据处理
 * @param {Object} options
 * @param {function(): void} [options.onCancel] 用户取消确认时回调
 */
export async function loginWithConfirm(requestFn, onSuccess, options = {}) {
  const { onCancel } = options;

  const handleResponse = async (res, allowRetry) => {
    const data = parseOAuthResponseData(res?.data);
    if (!data) {
      return;
    }

    if (isNeedConfirmLogin(data)) {
      if (!allowRetry) {
        return;
      }
      try {
        await ElMessageBox.confirm(
          data.error_description || data.msg || data.message || '是否继续登录？',
          '系统提示',
          {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            type: 'warning',
          }
        );
      } catch {
        onCancel?.();
        return;
      }
      const retryRes = await requestFn(true);
      const retryData = parseOAuthResponseData(retryRes.data);
      if (!retryData) {
        return;
      }
      if (isNeedConfirmLogin(retryData)) {
        return;
      }
      if (retryData.error_description && !retryData.access_token) {
        ElMessage({
          message: retryData.error_description,
          type: 'error',
        });
        return;
      }
      onSuccess(retryData);
      return;
    }

    if (data.error_description && !data.access_token) {
      ElMessage({
        message: data.error_description,
        type: 'error',
      });
      return;
    }

    onSuccess(data);
  };

  try {
    const res = await requestFn(false);
    await handleResponse(res, true);
  } catch (err) {
    const response = normalizeAxiosResponse(err);
    if (response) {
      await handleResponse(response, true);
      return;
    }
    throw err;
  }
}

export { isNeedConfirmLogin, parseOAuthResponseData } from '@/utils/oauthLoginError';
