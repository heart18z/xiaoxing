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
package org.springblade.common.cache;

import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.tool.utils.SpringUtil;
import org.springblade.core.tool.utils.StringPool;
import org.springblade.modules.system.entity.BizParam;
import org.springblade.modules.system.service.IBizParamService;

import java.util.List;
import java.util.Optional;

import static org.springblade.core.cache.constant.CacheConstant.BIZ_CACHE;

/**
 * 参数缓存工具类
 *
 * @author Chill
 */
public class BizParamCache {

	private static final String BIZ_PARAM_ID = "bizParam:id:";
	private static final String Tree_By_Path = "bizTree::path";
	private static final String BIZ_PARAMS_ID = "bizParams:id:";
	private static final String BIZ_PARAM_KEY = "bizParam:key:";
	private static final String BIZ_PARAM_VALUE = "bizParam:value:";
	private static final String BIZ_PARAM_LIST = "bizParam:list:";
	private static final String BIZ_PARAM_NAME = "bizParam:name:";
	private static final String BIZ_GET_PARAM_ID = "bizParam:get:id:";
	private static final String BIZ_GET_PARAM_NAME_BY_ID = "bizParam:getName:id:";
	private static final String GET_PARAM_NAME_BY_PARAM_VALUE = "bizParam:getParamName:paramValue:";
	private static final String GET_PARAMS_BY_PARAM_VALUES = "bizParam:getParamName:paramValues:";

	private static final Boolean TENANT_MODE = Boolean.FALSE;

	private static IBizParamService bizParamService;

	private static IBizParamService getBizParamlient() {
		if (bizParamService == null) {
			bizParamService = SpringUtil.getBean(IBizParamService.class);
		}
		return bizParamService;
	}


	/**
	 * 获取参数实体
	 *
	 * @param id 主键
	 * @return BizParam
	 */
	public static BizParam getById(Long id) {
		return CacheUtil.get(BIZ_CACHE, BIZ_PARAM_ID, id, () -> {
			BizParam result = getBizParamlient().getById(id);
			return result;
		}, TENANT_MODE);
	}

	/**
	 * 获取参数键
	 *
	 * @param paramName  参数名
	 * @param paramValue 参数值
	 * @return String
	 */
	public static String getKey(String paramName, String paramValue) {
		return CacheUtil.get(BIZ_CACHE, BIZ_PARAM_KEY + paramName + StringPool.COLON, paramValue, () -> {
			List<BizParam> list = getList(paramName);
			Optional<String> key = list.stream().filter(
				dict -> dict.getParamValue().equalsIgnoreCase(paramValue)
			).map(BizParam::getParamKey).findFirst();
			return key.orElse(StringPool.EMPTY);
		}, TENANT_MODE);
	}

	/**
	 * 获取参数值
	 *
	 * @param paramName 参数名
	 * @param paramKey  参数键
	 * @return String
	 */
	public static String getValue(String paramName, String paramKey) {
		return CacheUtil.get(BIZ_CACHE, BIZ_PARAM_VALUE + paramName + StringPool.COLON, paramKey, () -> {
			String result = getBizParamlient().getValue(paramName, paramKey);
			return result;
		}, TENANT_MODE);
	}

	/**
	 * 获取参数集合
	 *
	 * @param code 参数编号
	 * @return List<BizParam>
	 */
	public static List<BizParam> getList(String code) {
		return CacheUtil.get(BIZ_CACHE, BIZ_PARAM_LIST, code, () -> {
			List<BizParam> result = getBizParamlient().getList(code);
			return result;
		}, TENANT_MODE);
	}



	public static String getParamNameByParamKeyAndParamValue(String ParamKey ,String ParamValue) {
		return CacheUtil.get(BIZ_CACHE, BIZ_PARAM_NAME + ParamKey + StringPool.COLON, ParamValue, () -> {
			String result = getBizParamlient().getParamNameByParamKeyAndParamValue(ParamKey, ParamValue);
			return result;
		}, TENANT_MODE);
	}


	public static Long getParamIdByParamKeyAndParamValue(String ParamKey ,String ParamValue) {
		return CacheUtil.get(BIZ_CACHE, BIZ_GET_PARAM_ID + ParamKey + StringPool.COLON, ParamValue, () -> {
			Long result = getBizParamlient().getParamIdByParamKeyAndParamValue(ParamKey, ParamValue);
			return result;
		}, TENANT_MODE);
	}


	public static String getParamNameById(Long id) {
		return CacheUtil.get(BIZ_CACHE, BIZ_GET_PARAM_NAME_BY_ID, id, () -> {
			String result = getBizParamlient().getParamNameById(id);
			return result;
		}, TENANT_MODE);
	}


	public static String getParamNameByParamValue(String paramValue) {
		return CacheUtil.get(BIZ_CACHE, GET_PARAM_NAME_BY_PARAM_VALUE, paramValue, () -> {
			String result = getBizParamlient().getParamNameByParamValue(paramValue);
			return result;
		}, TENANT_MODE);
	}


	public static List<BizParam> getParamsByParamValues(String paramValues) {
		return CacheUtil.get(BIZ_CACHE, GET_PARAMS_BY_PARAM_VALUES, paramValues, () -> {
			List<BizParam> result = getBizParamlient().getParamsByParamValues(paramValues);
			return result;
		}, TENANT_MODE);
	}


}
