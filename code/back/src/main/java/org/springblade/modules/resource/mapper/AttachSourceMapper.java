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
package org.springblade.modules.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.resource.entity.AttachSourceEntity;
import org.springblade.modules.resource.vo.AttachSourceVO;
import org.springblade.modules.resource.vo.RegionVo;
import org.springblade.modules.resource.vo.AttachVO;

import java.util.List;

/**
 * 附件来源表 Mapper 接口
 *
 * @author BladeX
 * @since 2023-06-10
 */
public interface AttachSourceMapper extends BaseMapper<AttachSourceEntity> {

	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param attachSource
	 * @return
	 */
	List<AttachSourceVO> selectAttachSourcePage(IPage page, AttachSourceVO attachSource);


	List<AttachVO> selectAttachBySource(AttachSourceEntity attachSource);

	Boolean updateAttachSourceDeletedByAttachId(List<Long> idList);

	List<AttachSourceEntity> selectLastUpdateListByAttachIds(List<Long> idList);

	List<RegionVo> selectRegionList();

	List<AttachVO> selectAttachByOriginalId(AttachSourceEntity attachSource);
}
