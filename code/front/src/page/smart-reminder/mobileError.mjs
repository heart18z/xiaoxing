export function mobileError(error, fallback='操作未完成，请稍后重试') {
  const message = String(error?.response?.data?.msg || error?.message || error || '');
  if (/timeout|timed out|ECONNABORTED|ETIMEDOUT/i.test(message)) return '连接超时，请检查网络后重试';
  if (/Network Error|Failed to fetch|Load failed|fetch failed/i.test(message)) return '网络连接失败，请检查网络后重试';
  if (/SQLException|SQLSyntax|Jdbc|java\.|stack trace|<html|AxiosError/i.test(message)) return fallback;
  return message && message.length <= 160 ? message : fallback;
}
