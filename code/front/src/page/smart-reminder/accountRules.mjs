export const DEFAULT_PASSWORD = 'admin@123';
export function profileError(form) {
  if (!form.name?.trim() || form.name.length > 20) return '请输入姓名，最多20个字符';
  if (form.phone && !/^1[3-9][0-9]{9}$/.test(form.phone)) return '请输入正确的11位手机号';
  if (form.email && (form.email.length > 45 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))) return '请输入正确的邮箱，最多45个字符';
  return '';
}
export function registrationError(form) {
  if (!/^[A-Za-z0-9][A-Za-z0-9_-]{3,31}$/.test(form.account || '')) return '账号须为4–32位字母、数字、下划线或短横线，首位为字母或数字';
  if (!form.name?.trim() || form.name.length > 20) return '请输入姓名，最多20个字符';
  if (!form.password?.trim() || form.password.length < 8 || form.password.length > 64) return '密码须为8–64个字符';
  if (!form.phone?.trim() && !form.email?.trim()) return '手机号和邮箱至少填写一项';
  if (form.phone && !/^1[3-9][0-9]{9}$/.test(form.phone)) return '请输入正确的11位手机号';
  if (form.email && (form.email.length > 45 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))) return '请输入正确的邮箱，最多45个字符';
  return '';
}
export function passwordError(form) {
  if (!form.oldPassword) return '请输入原密码';
  if (!form.password?.trim() || form.password.length < 8 || form.password.length > 64) return '密码须为8–64个字符';
  return form.password !== form.confirmation ? '两次新密码不一致' : '';
}
