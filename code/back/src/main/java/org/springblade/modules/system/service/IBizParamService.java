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
package org.springblade.modules.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.core.mp.support.Query;
import org.springblade.modules.system.entity.BizParam;
import org.springblade.modules.system.vo.BizParamParam;
import org.springblade.modules.system.vo.BizParamVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 业务参数表 服务类
 *
 * @author BladeX
 * @since 2022-03-14
 */
public interface IBizParamService extends BaseService<BizParam> {


	/**
	 * 树形结构
	 *
	 * @return
	 */
	List<BizParamVO> tree();

	/**
	 * 根据参数路径，获取该路径下的树形参数
	 * @param path
	 * @return
	 */
	BizParamVO treeByPath(String path);

	/**
	 * 树形结构
	 *
	 * @return
	 */
	List<BizParamVO> parentTree();

	/**
	 * 获取专用平台参数树
	 * @return
	 */
	List<BizParamVO> privatePlatformTree();

	/**
	 * 获取字典表对应中文
	 *
	 * @param paramName 参数名
	 * @param paramKey  参数键
	 * @return
	 */
	String getValue(String paramName, String paramKey);

	/**
	 * 获取参数表
	 *
	 * @param paramName 参数名
	 * @return
	 */
	List<BizParam> getList(String paramName);

	/**
	 * 新增或修改
	 *
	 * @param bizParam
	 * @return
	 */
	BizParam submit(BizParam bizParam);

	/**
	 * 删除参数
	 *
	 * @param ids
	 * @return
	 */
	boolean removeBizParam(String ids);

	/**
	 * 顶级列表
	 *
	 * @param bizParam
	 * @param query
	 * @return
	 */
	IPage<BizParamVO> parentList(BizParamVO bizParam, Query query);

	/**
	 * 子列表
	 *
	 * @param bizParam
	 * @param parentId
	 * @return
	 */
	List<BizParamVO> childList(Map<String, Object> bizParam, Long parentId);

	/**
	 * 获取对象
	 *
	 * @param bizParam
	 * @return
	 */
	BizParamVO getOne(BizParam bizParam);


	String getParamNameByParamKeyAndParamValue(String ParamKey ,String ParamValue);

	Long getParamIdByParamKeyAndParamValue(String ParamKey, String ParamValue);

	String getParamNameById(Long id);

	String getParamNameByParamValue(String paramValue);

	List<BizParam> getParamsByParamValues(String paramValues);

	boolean getNoPermissionParams(BizParam bizParam);
	Collection<BizParam> getNoPermissionParams(String bizParamId, boolean isRemove);


	List<BizParamVO> dictionary(BizParamParam bizParamParam);


	void cleanAllData();

	void cleanAllSyncData();
}
