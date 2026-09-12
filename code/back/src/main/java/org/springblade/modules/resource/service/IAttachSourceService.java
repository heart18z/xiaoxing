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
package org.springblade.modules.resource.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.resource.dto.AttachSourceDTO;
import org.springblade.modules.resource.entity.AttachSourceEntity;
import org.springblade.modules.resource.vo.AttachSourceVO;
import org.springblade.modules.resource.vo.RegionVo;
import org.springblade.core.mp.base.BaseService;
import org.springblade.modules.resource.vo.AttachVO;
import org.springblade.modules.resource.vo.FileUpLoadVO;

import java.util.List;

/**
 * 附件来源表 服务类
 *
 * @author BladeX
 * @since 2023-06-10
 */
public interface IAttachSourceService extends BaseService<AttachSourceEntity> {
	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param attachSource
	 * @return
	 */
	IPage<AttachSourceVO> selectAttachSourcePage(IPage<AttachSourceVO> page, AttachSourceVO attachSource);


    List<AttachVO> getAttachBySource(FileUpLoadVO fileUpLoadVO);

    Boolean submit(AttachSourceDTO attachSource);

	Boolean removeAttach(AttachSourceDTO attachRemoveVO);

	Boolean updateAttachSourceDeletedByAttachId(List<Long> idList);

	List<RegionVo> getRegionList();

	boolean changeUserAccountPermission(AttachSourceDTO attachSource);

	boolean postSign(AttachSourceDTO attachRemoveVOS);

	List<AttachVO> listByOriId(FileUpLoadVO fileUpLoadVO);
}
