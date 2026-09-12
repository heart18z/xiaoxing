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
package org.springblade.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.system.entity.BizParam;
import org.springblade.modules.system.vo.BizParamVO;

import java.util.List;

/**
 * 业务参数表 Mapper 接口
 *
 * @author BladeX
 * @since 2022-03-14
 */
public interface BizParamMapper extends BaseMapper<BizParam> {

	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param bizParam
	 * @return
	 */
	List<BizParamVO> selectBizParamPage(IPage page, BizParamVO bizParam);

	/**
	 * 获取对象
	 *
	 * @param bizParam
	 * @return
	 */
	BizParamVO getOne(@Param("bizParam")BizParam bizParam);

	/**
	 * 获取参数表对应中文
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
	 * 获取树形节点
	 *
	 * @return
	 */
	List<BizParamVO> tree();

	/**
	 * 获取树形节点
	 *
	 * @return
	 */
	List<BizParamVO> parentTree();

	BizParam selectOneBySql(@Param("sql") String sql);

	List<BizParam> deepChild(@Param("parentId") Long parentId, @Param("limitSql") String limitSql);

	/**
	 * 清除同步数据
	 * @return
	 */
	Boolean deleteAllData();


	IPage<BizParamVO> selectBizParentPage(IPage<BizParamVO> page, BizParamVO bizParam);
}
