package org.springblade.modules.smartreminder.support;

import lombok.experimental.UtilityClass;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.Func;

import java.util.List;

@UtilityClass
public class AppRoleGuard {

	public static final String APP_ROLE = "app_user";

	public void requireAppUser() {
		List<String> roles = Func.toStrList(AuthUtil.getUserRole());
		if (!roles.contains(APP_ROLE)) {
			throw new ServiceException("当前账号未分配APP端使用人员角色");
		}
	}

	public void requireAdmin() {
		List<String> roles = Func.toStrList(AuthUtil.getUserRole());
		if (!roles.contains("administrator") && !roles.contains("admin")) {
			throw new ServiceException("仅管理员可以维护AI服务配置");
		}
	}
}
