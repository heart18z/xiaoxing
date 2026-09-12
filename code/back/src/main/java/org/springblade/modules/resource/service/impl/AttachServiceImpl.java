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
package org.springblade.modules.resource.service.impl;

import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.modules.resource.entity.AttachSourceEntity;
import org.springblade.modules.resource.service.IAttachSourceService;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.oss.OssTemplate;
import org.springblade.core.oss.model.BladeFile;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.utils.FileUtil;
import org.springblade.modules.resource.builder.oss.OssBuilder;
import org.springblade.modules.resource.entity.Attach;
import org.springblade.modules.resource.entity.Oss;
import org.springblade.modules.resource.mapper.AttachMapper;
import org.springblade.modules.resource.service.IAttachService;
import org.springblade.modules.resource.vo.AttachVO;
import org.springblade.modules.resource.vo.FileUpLoadVO;
import org.springblade.modules.standard.support.vo.KeyValueVO;
import org.springblade.modules.system.entity.Menu;
import org.springblade.modules.system.service.IMenuService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 附件表 服务实现类
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class AttachServiceImpl extends BaseServiceImpl<AttachMapper, Attach> implements IAttachService {
	private final Logger logger = LoggerFactory.getLogger(AttachServiceImpl.class);

	/**
	 * 对象存储构建类
	 */
	private final OssBuilder ossBuilder;


	private final IAttachSourceService attachSourceService;

	private final IMenuService menuService;


	@Override
	public IPage<AttachVO> selectAttachPage(IPage<AttachVO> page, AttachVO attach) {
		return page.setRecords(baseMapper.selectAttachPage(page, attach));
	}

	@Override
	@Transactional
	public BladeFile saveFileWithSource(MultipartFile file, FileUpLoadVO fileUpLoadVO) throws IOException {
		String fileName = file.getOriginalFilename();
		BladeFile bladeFile = ossBuilder.template().putFile(fileName, file.getInputStream());
		Attach attach = buildAttachForSource(fileName, file.getSize(), bladeFile);
		buildAttachSource(fileUpLoadVO, attach);
		bladeFile.setAttachId(attach.getId());
		return bladeFile;
	}

	@Override
	@Transactional
	public Boolean restore(List<Long> idList) {
		this.restoreAttach(idList);
		this.attachSourceService.updateAttachSourceDeletedByAttachId(idList);
		return true;
	}

	@Override
	public Boolean restoreAttach(List<Long> idList) {
		return this.baseMapper.updateAttachDeleted(idList);
	}

	@Override
	@Transactional
	public Boolean completeRemove(List<Long> ids) {
		if (ids == null || ids.size() == 0) {
			return true;
		}
		List<Attach> attaches = this.baseMapper.selectAttachByIds(ids);
		if (attaches == null || attaches.size() == 0) {
			return true;
		}
		Oss oss =  ossBuilder.getOss("000000",null);
		String domainOss = oss.getEndpoint()+ "/" + oss.getBucketName();
		List<String> fileNames = attaches.stream().filter(attach->domainOss.equals(attach.getDomainUrl())).map(Attach::getName).collect(Collectors.toList());
		OssTemplate ossTemplate = ossBuilder.template();
		// ossTemplate.removeFiles(fileList); //不好使 封装的有问题，官方文档上面说删除完了需要遍历get一边
		if (fileNames!=null&&fileNames.size()>0) {
			for (String name : fileNames) {
				ossTemplate.removeFile(name);
			}
		}
		if (ids.size()>0) {
			this.baseMapper.physicsRemoveByIds(ids);
		}
		return true;
	}

	@Override
	@Transactional
	public Boolean removeAttach(List<Long> ids) {
		List<AttachSourceEntity> attachSources = attachSourceService.list(new LambdaQueryWrapper<AttachSourceEntity>()
			.in(AttachSourceEntity::getAttachId,ids));
		List<Long> attachSourceIds = attachSources.stream().map(AttachSourceEntity::getId).collect(Collectors.toList());
		if (attachSourceIds!=null &&attachSourceIds.size()>0) {
			attachSourceService.deleteLogic(attachSourceIds);
		}
		this.deleteLogic(ids);
		return true;
	}

	@Override
	public List<KeyValueVO> getBucketNameList() {
		return this.baseMapper.selectBucketNameList();
	}

	@Override
	public IPage<AttachVO> selectAttachPageBySource(IPage<AttachVO> page, AttachVO attach) {
		if (attach.getSourceIds() == null || attach.getSourceIds().size() == 0) {
			return page.setRecords(new ArrayList<>());
		}
		List<AttachSourceEntity> list = attachSourceService.list(new LambdaQueryWrapper<AttachSourceEntity>()
			.in(AttachSourceEntity::getSourceId,attach.getSourceIds())
		);
		if (list == null || list.size() == 0) {
			return page.setRecords(new ArrayList<>());
		}
		attach.setAttachIds(list.stream().map(AttachSourceEntity::getAttachId).collect(Collectors.toList()));
		return page.setRecords(baseMapper.selectAttachPage(page, attach));
	}

	/**
	 * 构建附件表
	 *
	 * @param fileName  文件名
	 * @param fileSize  文件大小
	 * @param bladeFile 对象存储文件
	 * @return attachId
	 */
	private Attach buildAttachForSource(String fileName, Long fileSize, BladeFile bladeFile) {
		String fileExtension = FileUtil.getFileExtension(fileName);
		Attach attach = new Attach();
		attach.setDomainUrl(bladeFile.getDomain());
		attach.setLink(bladeFile.getLink());
		attach.setName(bladeFile.getName());
		attach.setOriginalName(bladeFile.getOriginalName());
		attach.setAttachSize(fileSize);
		attach.setExtension(fileExtension);
		this.save(attach);
		return attach;
	}


	private Boolean buildAttachSource(FileUpLoadVO fileUpLoadVO, Attach attach) {
//		if (fileUpLoadVO.getType() == null) {}
		if (fileUpLoadVO.getType() == 1) {
			Menu menu = menuService.getOne(new LambdaQueryWrapper<Menu>()
				.eq(Menu::getPath, fileUpLoadVO.getPath())
				.eq(Menu::getIsDeleted, BladeConstant.DB_NOT_DELETED)
				.last("limit 1")
			);
			if (menu == null) {
				throw new ServiceException("当前功能不允许上传视频，请联系管理员！");
			}
			fileUpLoadVO.setSourceId(menu.getId());
		}
		AttachSourceEntity attachSource = new AttachSourceEntity();

		attachSource.setAttachId(attach.getId());
		attachSource.setAttachExtension(fileUpLoadVO.getAttachExtension());
		attachSource.setSourceType(fileUpLoadVO.getType());
		attachSource.setSourceId(fileUpLoadVO.getSourceId());
		attachSource.setDiyFileName(fileUpLoadVO.getDiyFileName());
		return attachSourceService.save(attachSource);
	}

}
