/*
 *      Copyright (c) 2018-2028, Chill Zhuang All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the dreamlu.net developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: Chill 庄骞 (smallchill@163.com)
 */
package org.springblade.modules.system.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.DictCache;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.cache.SysCache;
import org.springblade.common.cache.UserCache;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.constant.TenantConstant;
import org.springblade.common.enums.DictEnum;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tenant.BladeTenantProperties;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.*;
import org.springblade.modules.auth.enums.UserEnum;
import org.springblade.modules.standard.support.vo.KeyValueVO;
import org.springblade.modules.system.entity.*;
import org.springblade.modules.system.enums.UserIdentityEnum;
import org.springblade.modules.system.excel.UserExcel;
import org.springblade.modules.system.mapper.UserMapper;
import org.springblade.modules.system.service.*;
import org.springblade.modules.system.vo.ContactUserVO;
import org.springblade.modules.system.vo.UserPlatformVO;
import org.springblade.modules.system.vo.UserVO;
import org.springblade.modules.system.wrapper.UserWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.springblade.common.constant.CommonConstant.DEFAULT_PARAM_PASSWORD;

/**
 * 服务实现类
 *
 * @author Chill
 */
@Service
@AllArgsConstructor
public class UserServiceImpl extends BaseServiceImpl<UserMapper, User> implements IUserService {
	private static final String GUEST_NAME = "guest";

	private static final String INIT_PASSWORD_KEY="";
	private final IUserDeptService userDeptService;
	private final IUserOauthService userOauthService;
	private final IRoleService roleService;
	private final BladeTenantProperties tenantProperties;
	private final IRoleMenuService roleMenuService;


	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submit(User user) {
		if (StringUtil.isBlank(user.getTenantId())) {
			user.setTenantId(BladeConstant.ADMIN_TENANT_ID);
		}
		String tenantId = user.getTenantId();
		if (Boolean.TRUE.equals(tenantProperties.getLicense())) {
			Tenant tenant = SysCache.getTenant(tenantId);
			if (Func.isNotEmpty(tenant)) {
				Integer accountNumber = tenant.getAccountNumber();
				String licenseKey = tenant.getLicenseKey();
				String decrypt = DesUtil.decryptFormHex(licenseKey, TenantConstant.DES_KEY);
				accountNumber = JsonUtil.parse(decrypt, Tenant.class).getAccountNumber();
				Long tenantCount = baseMapper.selectCount(Wrappers.<User>query().lambda().eq(User::getTenantId, tenantId));
				if (accountNumber != null && accountNumber > 0 && accountNumber <= tenantCount) {
					throw new ServiceException("当前租户已到最大账号额度!");
				}
			}
		}
		if (Func.isNotEmpty(user.getPassword())) {
			user.setPassword(DigestUtil.encrypt(user.getPassword()));
		}
		Long userCount = baseMapper.selectCount(Wrappers.<User>query().lambda().eq(User::getTenantId, tenantId).eq(User::getAccount, user.getAccount()));
		if (userCount > 0L && Func.isEmpty(user.getId())) {
			throw new ServiceException(StringUtil.format("当前用户 [{}] 已存在!", user.getAccount()));
		}
		String identityNames = this.checkIdentity(user.getIdentity(),user.getId());
		if (Func.isNotBlank(identityNames)) {
			throw new ServiceException(StringUtil.format("主要身份 [{}]已存在，请重新选择身份！",identityNames));
		}
		Long userId = IdWorker.getId();
		user.setId(userId);
		if (user.getDeptId() != null) {
			submitUserDept(user);
		}
		if (user.getStatus() == CommonConstant.DB_STATUS_NOT_NORMAL) {
			user.setIdentity("");
		}
		return save(user);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateUser(User user) {
		String tenantId = user.getTenantId();
		Long userCount = baseMapper.selectCount(
			Wrappers.<User>query().lambda()
				.eq(User::getTenantId, tenantId)
				.eq(User::getAccount, user.getAccount())
				.notIn(User::getId, user.getId())
		);
		if (userCount > 0L) {
			throw new ServiceException(StringUtil.format("当前用户 [{}] 已存在!", user.getAccount()));
		}
		String identityNames = this.checkIdentity(user.getIdentity(),user.getId());
		if (Func.isNotBlank(identityNames)) {
			throw new ServiceException(StringUtil.format("主要身份 [{}]已存在，请重新选择身份！",identityNames));
		}

		if (user.getDeptId() != null) {
			submitUserDept(user);
		}
		if (user.getStatus() == CommonConstant.DB_STATUS_NOT_NORMAL) {
			user.setIdentity("");
		}
		return updateUserInfo(user);
	}
	//判断用户身份，每种主要身份同时只能有一个人存在
	private String checkIdentity(String identities,Long id){
		if (Func.isBlank(identities)) {
			return null;
		}
		List<String> mainIdentities = UserIdentityEnum.getMainIdentities();
		mainIdentities = mainIdentities.stream().filter(item->identities.indexOf(item)>-1).collect(Collectors.toList());
		if (mainIdentities.size()==0) {
			return null;
		}
		List<String> finalMainIdentities = mainIdentities;
		LambdaQueryWrapper<User> lambdaQueryWrapper =new LambdaQueryWrapper<User>()
			.ne(id!=null,User::getId,id)
			.eq(User::getIsDeleted,BladeConstant.DB_NOT_DELETED).and((wrapper)->{
				for (String s : finalMainIdentities) {
				wrapper.or().like(User::getIdentity,s);
			}
		});


		List<String> usersIdentity = this.list(lambdaQueryWrapper).stream()
			.map(User::getIdentity)
			.distinct()
			.collect(Collectors.toList());
		List<String> eachList= new ArrayList<>();
		for (String s : usersIdentity) {
			if (Func.isNotBlank(s)) {
				eachList.addAll(Arrays.asList(s.split(",")));
			}
		}
		eachList = eachList.stream().distinct().collect(Collectors.toList());
		if (usersIdentity.size()==0) {
			return null;
		}

		return UserIdentityEnum.getNameByValueFilterMains(Func.join(eachList));
	}

	@Override
	public boolean updateUserInfo(User user) {
		user.setPassword(null);
		return updateById(user);
	}

	private boolean submitUserDept(User user) {
		List<Long> deptIdList = Func.toLongList(user.getDeptId());
		List<UserDept> userDeptList = new ArrayList<>();
		deptIdList.forEach(deptId -> {
			UserDept userDept = new UserDept();
			userDept.setUserId(user.getId());
			userDept.setDeptId(deptId);
			userDeptList.add(userDept);
		});
		userDeptService.remove(Wrappers.<UserDept>update().lambda().eq(UserDept::getUserId, user.getId()));
		return userDeptService.saveBatch(userDeptList);
	}

	@Override
	public IPage<User> selectUserPage(IPage<User> page, User user, Long deptId, Long menuId, String tenantId) {
		List<Long> deptIdList = SysCache.getDeptChildIds(deptId);
		List<Long> roleIdList = new ArrayList<>();
		if (menuId!=null) {
			roleIdList = roleMenuService.list(new LambdaQueryWrapper<RoleMenu>()
				.eq(RoleMenu::getMenuId,menuId)).stream().map(RoleMenu::getRoleId).collect(Collectors.toList());
		}
//		if (Func.isNotEmpty(user.getRoleId())) {
//			roleIdList.addAll(Func.toLongList(user.getRoleId()));
//		}

		return page.setRecords(baseMapper.selectUserPage(page, user, deptIdList, roleIdList, tenantId));
	}

	@Override
	public IPage<UserVO> selectUserSearch(UserVO user, Query query) {
		LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>query().lambda();
		String tenantId = AuthUtil.getTenantId();
		if (StringUtil.isNotBlank(tenantId)) {
			queryWrapper.eq(User::getTenantId, tenantId);
		}
		if (StringUtil.isNotBlank(user.getName())) {
			queryWrapper.like(User::getName, user.getName());
		}
		if (StringUtil.isNotBlank(user.getDeptName())) {
			String deptIds = SysCache.getDeptIdsByFuzzy(AuthUtil.getTenantId(), user.getDeptName());
			if (StringUtil.isNotBlank(deptIds)) {
				queryWrapper.and(wrapper -> {
					List<String> ids = Func.toStrList(deptIds);
					ids.forEach(id -> wrapper.like(User::getDeptId, id).or());
				});
			}
		}
		if (StringUtil.isNotBlank(user.getPostName())) {
			String postIds = SysCache.getPostIdsByFuzzy(AuthUtil.getTenantId(), user.getPostName());
			if (StringUtil.isNotBlank(postIds)) {
				queryWrapper.and(wrapper -> {
					List<String> ids = Func.toStrList(postIds);
					ids.forEach(id -> wrapper.like(User::getPostId, id).or());
				});
			}
		}
		IPage<User> pages = this.page(Condition.getPage(query), queryWrapper);
		return UserWrapper.build().pageVO(pages);
	}

	@Override
	public User userByAccount(String tenantId, String account) {
		return baseMapper.selectOne(Wrappers.<User>query().lambda().eq(User::getTenantId, tenantId).eq(User::getAccount, account).eq(User::getIsDeleted, BladeConstant.DB_NOT_DELETED));
	}

	@Override
	public UserInfo userInfo(Long userId) {
		User user = baseMapper.selectById(userId);
		return buildUserInfo(user);
	}

	@Override
	public UserInfo userInfo(String tenantId, String account, String password) {
		User user = baseMapper.getUser(tenantId, account, password);
		return buildUserInfo(user);
	}

	@Override
	public UserInfo userInfo(String tenantId, String account, String password, UserEnum userEnum) {
		User user = baseMapper.getUser(tenantId, account, password);
		return buildUserInfo(user, userEnum);
	}

	@Override
	public UserInfo userInfo(String userId, UserEnum userEnum) {
		User user = this.getById(userId);
		return buildUserInfo(user,userEnum);
	}

	private UserInfo buildUserInfo(User user) {
		return buildUserInfo(user, UserEnum.WEB);
	}

	private UserInfo buildUserInfo(User user, UserEnum userEnum) {
		if (ObjectUtil.isEmpty(user)) {
			return null;
		}
		UserInfo userInfo = new UserInfo();
		userInfo.setUser(user);
		if (Func.isNotEmpty(user)) {
			List<String> roleAlias = roleService.getRoleAliases(user.getRoleId());
			userInfo.setRoles(roleAlias);
		}
		// 根据每个用户平台，建立对应的detail表，通过查询将结果集写入到detail字段
		Kv detail = Kv.create().set("type", userEnum.getName());
		if (userEnum == UserEnum.WEB) {
			UserWeb userWeb = new UserWeb();
			UserWeb query = userWeb.selectOne(Wrappers.<UserWeb>lambdaQuery().eq(UserWeb::getUserId, user.getId()));
			if (ObjectUtil.isNotEmpty(query)) {
				detail.set("ext", query.getUserExt());
			}
		} else if (userEnum == UserEnum.APP) {
			UserApp userApp = new UserApp();
			UserApp query = userApp.selectOne(Wrappers.<UserApp>lambdaQuery().eq(UserApp::getUserId, user.getId()));
			if (ObjectUtil.isNotEmpty(query)) {
				detail.set("ext", query.getUserExt());
			}
		} else {
			UserOther userOther = new UserOther();
			UserOther query = userOther.selectOne(Wrappers.<UserOther>lambdaQuery().eq(UserOther::getUserId, user.getId()));
			if (ObjectUtil.isNotEmpty(query)) {
				detail.set("ext", query.getUserExt());
			}
		}
		userInfo.setDetail(detail);
		return userInfo;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public UserInfo userInfo(UserOauth userOauth) {
		UserOauth uo = userOauthService.getOne(Wrappers.<UserOauth>query().lambda().eq(UserOauth::getUuid, userOauth.getUuid()).eq(UserOauth::getSource, userOauth.getSource()));
		UserInfo userInfo;
		if (Func.isNotEmpty(uo) && Func.isNotEmpty(uo.getUserId())) {
			userInfo = this.userInfo(uo.getUserId());
			userInfo.setOauthId(Func.toStr(uo.getId()));
		} else {
			userInfo = new UserInfo();
			if (Func.isEmpty(uo)) {
				userOauthService.save(userOauth);
				userInfo.setOauthId(Func.toStr(userOauth.getId()));
			} else {
				userInfo.setOauthId(Func.toStr(uo.getId()));
			}
			User user = new User();
			user.setAccount(userOauth.getUsername());
			user.setTenantId(userOauth.getTenantId());
			userInfo.setUser(user);
			userInfo.setRoles(Collections.singletonList(GUEST_NAME));
		}
		return userInfo;
	}

	@Override
	public boolean grant(String userIds, String roleIds) {
		User user = new User();
		user.setRoleId(roleIds);
		return this.update(user, Wrappers.<User>update().lambda().in(User::getId, Func.toLongList(userIds)));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean resetPassword(String userIds) {
		User user = new User();
		String password = ParamCache.getValue(CommonConstant.DEFAULT_PARAM_PASSWORD);
		if (Func.isBlank(password)) {
			password = CommonConstant.DEFAULT_PASSWORD;
		}
		user.setPassword(DigestUtil.encrypt(password));
		user.setUpdateTime(DateUtil.now());
		user.setLastChangePasswordTime(new Date());
		this.update(user, Wrappers.<User>update().lambda().in(User::getId, Func.toLongList(userIds)));


		List<User> users = this.list(new LambdaQueryWrapper<User>().in(User::getId, Func.toLongList(userIds)));
		// 过滤获取特殊账户
		users = users.stream().filter(u->Arrays.asList(CommonConstant.DEFAULT_PARAM_SPECIAL_ACCOUNT_VALUE)
			.contains(u.getAccount())).collect(Collectors.toList());
		if (users.size()>0) {
			// 特殊账户，重置为自定义的密码
			String specialPass 	 = ParamCache.getValue(CommonConstant.DEFAULT_PARAM_SPECIAL_ACCOUNT);
			if (Func.isNotBlank(specialPass)) {
				List<String> passList = Arrays.asList(specialPass.split(","));
				if (passList.size()>=3) {
					Map<String,String> map = new HashMap<>();
					map.put(CommonConstant.DEFAULT_PARAM_SPECIAL_ACCOUNT_VALUE[0],passList.get(0));
					map.put(CommonConstant.DEFAULT_PARAM_SPECIAL_ACCOUNT_VALUE[1],passList.get(1));
					map.put(CommonConstant.DEFAULT_PARAM_SPECIAL_ACCOUNT_VALUE[2],passList.get(2));
					users.stream().forEach(u->{
						if (map.containsKey(u.getAccount())) {
							u.setPassword(DigestUtil.encrypt(map.get(u.getAccount())));
							u.setUpdateTime(DateUtil.now());
						}
					});
					this.updateBatchById(users);
				}

			}

		}
		return true;
	}

	@Override
	public boolean updatePassword(Long userId, String oldPassword, String newPassword, String newPassword1) {
		User user = getById(userId);
		if (!newPassword.equals(newPassword1)) {
			throw new ServiceException("请输入正确的确认密码!");
		}
		if (!user.getPassword().equals(DigestUtil.hex(oldPassword))) {
			throw new ServiceException("原密码不正确!");
		}
		return this.update(Wrappers.<User>update().lambda()
			.set(User::getPassword, DigestUtil.hex(newPassword))
				.set(User::getLastChangePasswordTime,new Date())
			.eq(User::getId, userId));
	}

	@Override
	public boolean removeUser(String userIds) {
		if (Func.contains(Func.toLongArray(userIds), AuthUtil.getUserId())) {
			throw new ServiceException("不能删除本账号!");
		}
		return deleteLogic(Func.toLongList(userIds));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void importUser(List<UserExcel> data, Boolean isCovered) {
		data.forEach(userExcel -> {
			User user = Objects.requireNonNull(BeanUtil.copy(userExcel, User.class));
			// 设置用户平台
			//user.setUserType(Func.toInt(DictCache.getKey(DictEnum.USER_TYPE, userExcel.getUserTypeName()), 1));
			// 设置部门ID
			user.setDeptId(Func.toStrWithEmpty(SysCache.getDeptIds(userExcel.getTenantId(), userExcel.getDeptName()), StringPool.EMPTY));
			// 设置岗位ID
			user.setPostId(Func.toStrWithEmpty(SysCache.getPostIds(userExcel.getTenantId(), userExcel.getPostName()), StringPool.EMPTY));
			// 设置角色ID
			user.setRoleId(Func.toStrWithEmpty(SysCache.getRoleIds(userExcel.getTenantId(), userExcel.getRoleName()), StringPool.EMPTY));
			// 设置租户ID
			if (!AuthUtil.isAdministrator() || StringUtil.isBlank(user.getTenantId())) {
				user.setTenantId(AuthUtil.getTenantId());
			}
			// 覆盖数据
			if (isCovered) {
				// 查询用户是否存在
				User oldUser = UserCache.getUser(userExcel.getTenantId(), userExcel.getAccount());
				if (oldUser != null && oldUser.getId() != null) {
					user.setId(oldUser.getId());
					this.updateUser(user);
					return;
				}
			}
			// 获取默认密码配置
			String initPassword = ParamCache.getValue(DEFAULT_PARAM_PASSWORD);
			user.setPassword(initPassword);
			this.submit(user);
		});
	}

	@Override
	public List<UserExcel> exportUser(Wrapper<User> queryWrapper) {
		List<UserExcel> userList = baseMapper.exportUser(queryWrapper);
		userList.forEach(user -> {
			user.setUserTypeName(DictCache.getValue(DictEnum.USER_TYPE, user.getUserType()));
			user.setRoleName(StringUtil.join(SysCache.getRoleNames(user.getRoleId())));
			user.setDeptName(StringUtil.join(SysCache.getDeptNames(user.getDeptId())));
			user.setPostName(StringUtil.join(SysCache.getPostNames(user.getPostId())));
		});
		return userList;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean registerGuest(User user, Long oauthId) {
		Tenant tenant = SysCache.getTenant(user.getTenantId());
		if (tenant == null || tenant.getId() == null) {
			throw new ServiceException("租户信息错误!");
		}
		UserOauth userOauth = userOauthService.getById(oauthId);
		if (userOauth == null || userOauth.getId() == null) {
			throw new ServiceException("第三方登陆信息错误!");
		}
		user.setRealName(user.getName());
		user.setAvatar(userOauth.getAvatar());
		user.setRoleId(StringPool.MINUS_ONE);
		user.setDeptId(StringPool.MINUS_ONE);
		user.setPostId(StringPool.MINUS_ONE);
		boolean userTemp = this.submit(user);
		userOauth.setUserId(user.getId());
		userOauth.setTenantId(user.getTenantId());
		boolean oauthTemp = userOauthService.updateById(userOauth);
		return (userTemp && oauthTemp);
	}

	@Override
	public boolean updatePlatform(List<UserPlatformVO> userPlatformVOS) {
		for(UserPlatformVO platformVO : userPlatformVOS) {
			String userType = platformVO.getUserType();
			Long userId = platformVO.getUserId();
			String userExt = platformVO.getUserExt();
			if (userType.equals(String.valueOf(UserEnum.WEB.getCategory()))) {
				UserWeb userWeb = new UserWeb();
				UserWeb query = userWeb.selectOne(Wrappers.<UserWeb>lambdaQuery().eq(UserWeb::getUserId, userId));
				if (ObjectUtil.isNotEmpty(query)) {
					userWeb.setId(query.getId());
				}
				userWeb.setUserId(userId);
				userWeb.setUserExt(userExt);
				userWeb.insertOrUpdate();
			} else if (userType.equals(String.valueOf(UserEnum.APP.getCategory()))) {
				UserApp userApp = new UserApp();
				UserApp query = userApp.selectOne(Wrappers.<UserApp>lambdaQuery().eq(UserApp::getUserId, userId));
				if (ObjectUtil.isNotEmpty(query)) {
					userApp.setId(query.getId());
				}
				userApp.setUserId(userId);
				userApp.setUserExt(userExt);
				userApp.insertOrUpdate();
			} else {
				UserOther userOther = new UserOther();
				UserOther query = userOther.selectOne(Wrappers.<UserOther>lambdaQuery().eq(UserOther::getUserId, userId));
				if (ObjectUtil.isNotEmpty(query)) {
					userOther.setId(query.getId());
				}
				userOther.setUserId(userId);
				userOther.setUserExt(userExt);
				userOther.insertOrUpdate();
			}
		}
		return true;
	}

	@Override
	public UserVO platformDetail(User user) {
		User detail = baseMapper.selectOne(Condition.getQueryWrapper(user));
		UserVO userVO = UserWrapper.build().entityVO(detail);
		userVO.setUserExtList(new ArrayList<>());
		if (Func.isNotEmpty(userVO.getUserType())) {
			String[] types =  Func.split(userVO.getUserType(),",");
			for (int i = 0; i < types.length; i++) {
				if (types[i].equals(String.valueOf(UserEnum.WEB.getCategory()))) {
					UserWeb userWeb = new UserWeb();
					UserWeb query = userWeb.selectOne(Wrappers.<UserWeb>lambdaQuery().eq(UserWeb::getUserId, user.getId()));
					userVO.getUserExtList().add(new UserPlatformVO(userVO.getId(),String.valueOf(UserEnum.WEB.getCategory()),UserEnum.WEB.getName(),query==null?"":query.getUserExt(),new ArrayList<>()));
				} else if (types[i].equals(String.valueOf(UserEnum.APP.getCategory()))) {
					UserApp userApp = new UserApp();
					UserApp query = userApp.selectOne(Wrappers.<UserApp>lambdaQuery().eq(UserApp::getUserId, user.getId()));
					userVO.getUserExtList().add(new UserPlatformVO(userVO.getId(),String.valueOf(UserEnum.APP.getCategory()),UserEnum.APP.getName(),query==null?"":query.getUserExt(),new ArrayList<>()));
				} else {
					UserOther userOther = new UserOther();
					UserOther query = userOther.selectOne(Wrappers.<UserOther>lambdaQuery().eq(UserOther::getUserId, user.getId()));
					userVO.getUserExtList().add(new UserPlatformVO(userVO.getId(),String.valueOf(UserEnum.OTHER.getCategory()),UserEnum.OTHER.getName(),query==null?"":query.getUserExt(),new ArrayList<>()));
				}
			}
			userVO.setUserTypeName(Func.join(types));
		}


		return userVO;
	}

	@Override
	public Boolean isUserExists(String tenantId, String account, String password) {
		return this.baseMapper.getUser(tenantId,account,password) != null;
	}

	@Override
	public User userInfoById(Long userId) {
		return this.baseMapper.selectById(userId);
	}


	@Override
	public ContactUserVO getUserContact(boolean isMain) {
		ContactUserVO contactUserVO = new ContactUserVO();
		List<User> adminUser;
		List<User> tecUser;
		List<User> funcUser;

		if (!isMain) {
			adminUser = this.list(new LambdaQueryWrapper<User>()
				.select(User::getId,User::getRealName,User::getPhone,User::getEmail,User::getWechat,User::getIdentity)
				.and(wrapperer->wrapperer.like(User::getIdentity,UserIdentityEnum.USER_IDENTITY_ADMIN.getValue())
					.or().like(User::getIdentity,UserIdentityEnum.USER_IDENTITY_ADMIN_MAIN.getValue()))
				.eq(User::getIsDeleted,BladeConstant.DB_NOT_DELETED));

			tecUser = this.list(new LambdaQueryWrapper<User>()
				.select(User::getId,User::getRealName,User::getPhone,User::getEmail,User::getWechat,User::getIdentity)
				.and(wrapperer->wrapperer.like(User::getIdentity,UserIdentityEnum.USER_IDENTITY_TEC_MAIN.getValue())
					.or().like(User::getIdentity,UserIdentityEnum.USER_IDENTITY_TEC.getValue()))
				.eq(User::getIsDeleted,BladeConstant.DB_NOT_DELETED));

			funcUser = this.list(new LambdaQueryWrapper<User>()
				.select(User::getId,User::getRealName,User::getPhone,User::getEmail,User::getWechat,User::getIdentity)
				.and(wrapperer->wrapperer.like(User::getIdentity,UserIdentityEnum.USER_IDENTITY_FUNC.getValue())
					.or().like(User::getIdentity,UserIdentityEnum.USER_IDENTITY_FUNC_MAIN.getValue()))
				.eq(User::getIsDeleted,BladeConstant.DB_NOT_DELETED));
		}else {
			adminUser = this.list(new LambdaQueryWrapper<User>()
				.select(User::getId,User::getRealName,User::getPhone,User::getEmail,User::getWechat,User::getIdentity)
				.like(User::getIdentity,UserIdentityEnum.USER_IDENTITY_ADMIN_MAIN.getValue()).eq(User::getIsDeleted,BladeConstant.DB_NOT_DELETED));
			tecUser = this.list(new LambdaQueryWrapper<User>()
				.select(User::getId,User::getRealName,User::getPhone,User::getEmail,User::getWechat,User::getIdentity)
				.like(User::getIdentity,UserIdentityEnum.USER_IDENTITY_TEC_MAIN.getValue()).eq(User::getIsDeleted,BladeConstant.DB_NOT_DELETED));
			funcUser = this.list(new LambdaQueryWrapper<User>()
				.select(User::getId,User::getRealName,User::getPhone,User::getEmail,User::getWechat,User::getIdentity)
				.like(User::getIdentity,UserIdentityEnum.USER_IDENTITY_FUNC_MAIN.getValue()).eq(User::getIsDeleted,BladeConstant.DB_NOT_DELETED));
		}

		contactUserVO.setAdminUser(adminUser);
		contactUserVO.setFuncUser(funcUser);
		contactUserVO.setTecUser(tecUser);
		return contactUserVO;
	}

	@Override
	public List<KeyValueVO> getUserKvList() {
		List<User> userList = this.list(
			new LambdaQueryWrapper<User>().select(User::getId,User::getRealName)
		);
		List<KeyValueVO> result = userList.stream().map(item->
			new KeyValueVO(item.getRealName(),String.valueOf(item.getId()))).collect(Collectors.toList());
		return result;
	}

	@Override
	public List<KeyValueVO> getUserAccountKvList() {
		List<User> userList = this.list(
			new LambdaQueryWrapper<User>().select(User::getAccount,User::getRealName)
		);
		List<KeyValueVO> result = userList.stream().map(item->
			new KeyValueVO(item.getRealName(),item.getAccount())).collect(Collectors.toList());
		return result;
	}

	@Override
	public List<KeyValueVO> getUserContactKeyValue() {
		List<KeyValueVO> list = new ArrayList<>();
		for(UserIdentityEnum u:UserIdentityEnum.values()) {
			list.add(new KeyValueVO(u.getName(),u.getValue()));
		}
		return list;
	}

	@Override
	public String resetPasswordShow(String userId) {
		if (Func.isBlank(userId)) {throw new ServiceException("用户参数为空");}
		User user = this.getById(userId);
		boolean incloud = Arrays.asList(CommonConstant.DEFAULT_PARAM_SPECIAL_ACCOUNT_VALUE)
			.contains(user.getAccount());
		// 过滤获取特殊账户

		if (incloud) {
			// 特殊账户，重置为自定义的密码
			String specialPass 	 = ParamCache.getValue(CommonConstant.DEFAULT_PARAM_SPECIAL_ACCOUNT);
			if (Func.isNotBlank(specialPass)) {
				List<String> passList = Arrays.asList(specialPass.split(","));
				if (passList.size()>=3) {
					Map<String,String> map = new HashMap<>();
					map.put(CommonConstant.DEFAULT_PARAM_SPECIAL_ACCOUNT_VALUE[0],passList.get(0));
					map.put(CommonConstant.DEFAULT_PARAM_SPECIAL_ACCOUNT_VALUE[1],passList.get(1));
					map.put(CommonConstant.DEFAULT_PARAM_SPECIAL_ACCOUNT_VALUE[2],passList.get(2));
					return map.get(user.getAccount());
				}

			}

		}else {
			String password = ParamCache.getValue(CommonConstant.DEFAULT_PARAM_PASSWORD);
			if (Func.isBlank(password)) {
				password = CommonConstant.DEFAULT_PASSWORD;
			}
			return password;
		}

		return "";
	}



}
