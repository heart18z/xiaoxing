package org.springblade.modules.unifiedoauth.service;

import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.WebUtil;
import org.springblade.modules.auth.enums.UserEnum;
import org.springblade.modules.auth.utils.TokenUtil;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.entity.UserInfo;
import org.springblade.modules.system.service.IRoleService;
import org.springblade.modules.system.service.IUserService;
import org.springblade.modules.unifiedoauth.constant.UnifiedOauthConstant;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 统一授权用户加载
 */
@Component
@RequiredArgsConstructor
public class UnifiedOauthUserLoader {

	private static final String LOGIN_TYPE_SYSTEM = UnifiedOauthConstant.ACCOUNT_LOGIN_TYPE;
	private static final Integer ABLE_ACCOUNT_STATUS = 1;

	private final IUserService userService;
	private final IRoleService roleService;

	public UserInfo loadUserInfo(String username) {
		HttpServletRequest request = WebUtil.getRequest();
		String headerDept = request != null ? request.getHeader(TokenUtil.DEPT_HEADER_KEY) : null;
		String headerRole = request != null ? request.getHeader(TokenUtil.ROLE_HEADER_KEY) : null;

		User user = userService.userByAccount(TokenUtil.DEFAULT_TENANT_ID, username);
		if (user == null) {
			return null;
		}
		if (Func.notNull(user.getLoginType()) && LOGIN_TYPE_SYSTEM.equals(user.getLoginType())) {
			throw new ServiceException("当前账号登录方式为“仅限本系统账号登录”，请使用账号密码登录！");
		}
		UserInfo userInfo = userService.userInfo(user.getId().toString(), UserEnum.WEB);
		if (userInfo == null || userInfo.getUser() == null) {
			return null;
		}
		if (userInfo.getUser().getStatus() != ABLE_ACCOUNT_STATUS) {
			throw new ServiceException("用户已被封禁，请联系管理员解封后登陆！");
		}
		if (Func.isNotEmpty(headerDept) && userInfo.getUser().getDeptId().contains(headerDept)) {
			userInfo.getUser().setDeptId(headerDept);
		}
		if (Func.isNotEmpty(headerRole) && userInfo.getUser().getRoleId().contains(headerRole)) {
			List<String> roleAliases = roleService.getRoleAliases(headerRole);
			userInfo.setRoles(roleAliases);
			userInfo.getUser().setRoleId(headerRole);
		}
		return userInfo;
	}
}
