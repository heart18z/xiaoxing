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
package org.springblade.modules.develop.mapper;

import org.springblade.modules.develop.excel.ThirdPartyTableColumnExcel;
import org.springblade.modules.develop.vo.ThirdPartyTableColumnVO;
import org.springblade.modules.develop.entity.ThirdPartyTableColumnEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 数据中台列信息 Mapper 接口
 *
 * @author BladeX
 * @since 2025-02-09
 */
public interface ThirdPartyTableColumnMapper extends BaseMapper<ThirdPartyTableColumnEntity> {

	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param thirdPartyTableColumn
	 * @return
	 */
	List<ThirdPartyTableColumnVO> selectThirdPartyTableColumnPage(IPage page, ThirdPartyTableColumnVO thirdPartyTableColumn);


	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<ThirdPartyTableColumnExcel> exportThirdPartyTableColumn(@Param("ew") Wrapper<ThirdPartyTableColumnEntity> queryWrapper);

}
