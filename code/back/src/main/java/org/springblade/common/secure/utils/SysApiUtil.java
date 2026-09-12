package org.springblade.common.secure.utils;

import org.springblade.common.secure.domain.ApiUserInfo;
import org.springblade.common.secure.systemapi.SystemApiUtil;

public class SysApiUtil {

	private static final String DICT_ID = "system:api:";

	public static ApiUserInfo getUser()  {
		ApiUserInfo apiUserInfo = SystemApiUtil.userInfoAndDecodeHeader();
		return apiUserInfo;
	}


//	public static BladeUser getUser(String account) {
//		return CacheUtil.get(USER_CACHE, DICT_ID,  TokenUtil.DEFAULT_TENANT_ID+ StringPool.DASH + account, () -> null);
//	}
//
//	public static void putUser(BladeUser user) {
//		 CacheUtil.put(USER_CACHE, DICT_ID,  TokenUtil.DEFAULT_TENANT_ID+ StringPool.DASH + user.getAccount(),user);
//	}

//	public static void putUserByUserInfo(UserInfo userInfo) {
//		BladeUser bladeUser = new BladeUser();
//		User user = userInfo.getUser();
//		Long userId = user.getId();
//		String tenantId = user.getTenantId();
//
//		String deptId = Func.toStrWithEmpty(user.getDeptId(), "-1");
//		String postId = Func.toStrWithEmpty(user.getPostId(), "-1");
//		String roleId = Func.toStrWithEmpty(user.getRoleId(), "-1");
//		String account = Func.toStr(user.getAccount());
//		String roleName = Func.join(userInfo.getRoles());
//		String userName = Func.toStr(user.getRealName());
//		String nickName = Func.toStr(user.getName());
//
//		bladeUser.setUserId(userId);
//		bladeUser.setTenantId(tenantId);
//		bladeUser.setAccount(account);
//		bladeUser.setDeptId(deptId);
//		bladeUser.setPostId(postId);
//		bladeUser.setRoleId(roleId);
//		bladeUser.setRoleName(roleName);
//		bladeUser.setUserName(userName);
//		bladeUser.setNickName(nickName);
//		putUser(bladeUser);
//	}

	public static String getUserAccount()  {
		ApiUserInfo user = getUser();
		return null == user ? "" : user.getUserAccount();
	}


	public static String getUserName()  {
		ApiUserInfo user = getUser();
		return null == user ? "" : user.getUserName();
	}





	public static String getDeptId()  {
		ApiUserInfo user = getUser();
		return null == user ? "" : user.getDeptId();
	}



}
