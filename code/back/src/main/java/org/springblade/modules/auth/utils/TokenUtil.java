package org.springblade.modules.auth.utils;

import org.springblade.common.cache.SysCache;
import org.springblade.core.oauth2.provider.OAuth2Request;
import org.springblade.core.oauth2.service.OAuth2User;
import org.springblade.core.oauth2.service.impl.OAuth2UserDetail;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.entity.UserInfo;

/**
 * 认证工具类
 */
public class TokenUtil {

	public final static String DEPT_HEADER_KEY = "Dept-Id";
	public final static String ROLE_HEADER_KEY = "Role-Id";
	public final static String CAPTCHA_HEADER_KEY = "Captcha-Key";
	public final static String CAPTCHA_HEADER_CODE = "Captcha-Code";
	public final static String TENANT_HEADER_KEY = "Tenant-Id";
	public final static String DEFAULT_TENANT_ID = "000000";
	public final static String USER_TYPE_HEADER_KEY = "User-Type";
	public final static String DEFAULT_USER_TYPE = "web";
	public final static String HEADER_KEY = "Authorization";

	/**
	 * 系统用户转换为 OAuth2 标准用户
	 */
	public static OAuth2User convertUser(UserInfo userInfo, OAuth2Request request) {
		if (userInfo == null || userInfo.getUser() == null) {
			return null;
		}
		User user = userInfo.getUser();
		String userDept = request.getUserDept();
		String userRole = request.getUserRole();
		if (Func.isNotEmpty(userDept) && user.getDeptId().contains(userDept)) {
			user.setDeptId(userDept);
		}
		if (Func.isNotEmpty(userRole) && user.getRoleId().contains(userRole)) {
			user.setRoleId(userRole);
			userInfo.setRoles(SysCache.getRoleAliases(userRole));
		}
		OAuth2UserDetail userDetail = new OAuth2UserDetail();
		userDetail.setUserId(String.valueOf(user.getId()));
		userDetail.setOauthId(userInfo.getOauthId());
		userDetail.setTenantId(user.getTenantId());
		userDetail.setName(user.getName());
		userDetail.setRealName(user.getRealName());
		userDetail.setAccount(user.getAccount());
		userDetail.setPassword(user.getPassword());
		userDetail.setDeptId(user.getDeptId());
		userDetail.setPostId(user.getPostId());
		userDetail.setRoleId(user.getRoleId());
		userDetail.setRoleName(Func.join(userInfo.getRoles()));
		userDetail.setAvatar(user.getAvatar());
		userDetail.setAuthorities(userInfo.getRoles());
		userDetail.setDetail(userInfo.getDetail());
		return userDetail;
	}
}
