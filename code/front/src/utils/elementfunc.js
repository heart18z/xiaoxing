import { ElMessage } from 'element-plus';

export function setMessageConfig(timeout) {
  const duration = timeout * 1000;

  const createMessage = msg => {
    const msgObj = {
      message: msg.message ? msg.message : msg,
      duration,
    };
    const msgType = msg.type || '';
    switch (msgType) {
      case 'success':
        return ElMessage.success(msgObj);
      case 'warning':
        return ElMessage.warning(msgObj);
      case 'error':
        return ElMessage.error(msgObj);
      default:
        return ElMessage(msgObj);
    }
  };

  createMessage.success = msg => ElMessage.success({ message: msg, duration });
  createMessage.warning = msg => ElMessage.warning({ message: msg, duration });
  createMessage.error = msg => ElMessage.error({ message: msg, duration });
  createMessage.info = msg => ElMessage.info({ message: msg, duration });

  return createMessage;
}
