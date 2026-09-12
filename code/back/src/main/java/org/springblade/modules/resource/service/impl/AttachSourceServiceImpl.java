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

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.rpc.apiplatform.constant.ApiPlatformUrlConstant;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformHttp;
import org.springblade.common.rpc.support.SignPdfParam;
import org.springblade.core.tool.api.R;
import org.springblade.modules.resource.dto.AttachSourceDTO;
import org.springblade.modules.resource.entity.AttachPermissionLogEntity;
import org.springblade.modules.resource.entity.AttachSourceEntity;
import org.springblade.modules.resource.mapper.AttachSourceMapper;
import org.springblade.modules.resource.service.IAttachPermissionLogService;
import org.springblade.modules.resource.service.IAttachSourceService;
import org.springblade.modules.resource.vo.AttachSourceVO;
import org.springblade.modules.resource.vo.RegionVo;
import org.springblade.common.constant.CommonConstant;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.resource.entity.Attach;
import org.springblade.modules.resource.service.IAttachService;
import org.springblade.modules.resource.vo.AttachVO;
import org.springblade.modules.resource.vo.FileUpLoadVO;
import org.springblade.modules.resource.wrapper.AttachSourceWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 附件来源表 服务实现类
 *
 * @author BladeX
 * @since 2023-06-10
 */
@Service
@AllArgsConstructor
public class AttachSourceServiceImpl extends BaseServiceImpl<AttachSourceMapper, AttachSourceEntity> implements IAttachSourceService {

	private final IAttachService attachService;
	//基础字典的是
	private final String BASE_DATA_YES="1";

	private final String BASE_DATA_NO="0";
	//数据脱敏
	private final String BASE_FIT_STR="*********";

	private final String PERMISSION_USER_ROLE_NAME="fileSecrecySet";

	private final IAttachPermissionLogService attachPermissionLogService;


	@Override
	public IPage<AttachSourceVO> selectAttachSourcePage(IPage<AttachSourceVO> page, AttachSourceVO attachSource) {
		return page.setRecords(baseMapper.selectAttachSourcePage(page, attachSource));
	}


	@Override
	public List<AttachVO> getAttachBySource(FileUpLoadVO fileUpLoadVO) {
		AttachSourceEntity attachSource = new AttachSourceEntity();
		attachSource.setSourceId(fileUpLoadVO.getSourceId());
		List<AttachVO> list =this.baseMapper.selectAttachBySource(attachSource);
		//过滤权限
		for (AttachVO vo : list) {
			if (BASE_DATA_YES.equals(vo.getIsPhysicalSave())&&
				BASE_DATA_YES.equals(vo.getIsSecrecy())&&
				!checkPermissionUserAccount(vo.getPermissionUserAccount())
			) {
				vo.setArchiveNo("");
				vo.setFilePhysicalPosition(BASE_FIT_STR);
				vo.setLink("");
				vo.setPermission(false);
			} else {
				vo.setPermission(true);
			}

			if (!this.checkBorrowing(vo)) {
				vo.setPhysicalPositionInfo("");
				vo.setKeeper("");
				vo.setArchiveNo("");
			}
		}
		return list;
	}

	@Override
	@Transactional
	public Boolean submit(AttachSourceDTO attachSource) {
		Date now =new Date();
		List<AttachVO> attachSources = attachSource.getAttachSourceList();
		if (attachSources == null || attachSources.isEmpty()) {
			throw new ServiceException("请上传文件后再提交！");
		}
		List<Attach> newAttachList = new ArrayList<>();

		Map<Long,AttachSourceEntity> dbDataMap = getDbAttachSource(attachSources);
		// 只有第一次新增的时候才允许修改物理储存地址
		List<AttachSourceEntity> attachSourceEntitys = new ArrayList<>();
		//修改权限时记录日志
		List<Long> sourceId=new ArrayList<>();
		for (AttachVO attachVO : attachSources) {
			boolean setPermission = false;
			//对于没有权限的数据，不能修改,
			if (attachVO.getAttachSourceId()!=null) {
				AttachSourceEntity dbData =  dbDataMap.get(attachVO.getAttachSourceId());
				//如果是原数据保密权限为是，并且没有权限修改，则跳过
				if(BASE_DATA_YES.equals(dbData.getIsPhysicalSave())&&
					BASE_DATA_YES.equals(dbData.getIsSecrecy())&&
					!checkPermissionUserAccount(dbData.getPermissionUserAccount())) {
					continue;
				}
				//如果原数据保密权限为否，新改为是，则保密权限改为当前修改用户
				if (BASE_DATA_NO.equals(dbData.getIsSecrecy())
					&& BASE_DATA_YES.equals(attachVO.getIsSecrecy())) {
					setPermission = true;
				}
			}

			//错乱数据
			if (BASE_FIT_STR.equals(attachVO.getFilePhysicalPosition())) {
				continue;
			}
			AttachSourceEntity attachSourceEntity = new AttachSourceEntity();
			attachSourceEntity.setId(attachVO.getAttachSourceId());
			if (attachSourceEntity.getId() ==null) {
				attachSourceEntity.setId(IdWorker.getId());
				attachSourceEntity.setCreateUser(AuthUtil.getUserId());
				attachSourceEntity.setCreateTime(now);
				attachSourceEntity.setSignStatus("signStatus_10");
			}
			attachSourceEntity.setDiyFileName(attachVO.getDiyFileName());
			attachSourceEntity.setAttachExtension(attachVO.getAttachExtension());
			attachSourceEntity.setAttachId(attachVO.getId());
			attachSourceEntity.setSourceId(attachVO.getSourceId());
			attachSourceEntity.setSourceType(attachVO.getSourceType());
			attachSourceEntity.setLink(attachVO.getLink());
			attachSourceEntity.setSort(attachVO.getSort());
			attachSourceEntity.setFilePhysicalPosition(attachVO.getFilePhysicalPosition());
			attachSourceEntity.setIsPhysicalSave(attachVO.getIsPhysicalSave());
			attachSourceEntity.setFileNum(attachVO.getFileNum());
			attachSourceEntity.setSafekeepStatus(attachVO.getSafekeepStatus());
			attachSourceEntity.setIsSecrecy(attachVO.getIsSecrecy());
			attachSourceEntity.setUploadType(attachVO.getUploadType());
			//（原先不保密现在保密的数据）或者是（新增的保密数据），默认创建人为可编辑用户
			if (setPermission||(attachVO.getAttachSourceId()==null&&
				BASE_DATA_YES.equals(attachVO.getIsPhysicalSave())&&
				BASE_DATA_YES.equals(attachVO.getIsSecrecy()))) {
				sourceId.add(attachSourceEntity.getId());
				attachSourceEntity.setPermissionUserAccount(AuthUtil.getUserAccount());
			}
			//如果不保管则将保密置为否
			if (BASE_DATA_NO.equals(attachVO.getIsPhysicalSave())) {
				attachSourceEntity.setIsSecrecy(BASE_DATA_NO);
			}
			//如果是不保密，则将保密用户字段清空
			//20240902取消这个逻辑
//			if (BASE_DATA_NO.equals(attachVO.getIsSecrecy())) {
//				attachSourceEntity.setPermissionUserAccount("");
//			}
			//如果原先是实物保存设置为是之后，那么后面就不能再修改 实物保存 和 是否保密
			if (attachVO.getAttachSourceId()!=null) {
				AttachSourceEntity dbData =  dbDataMap.get(attachVO.getAttachSourceId());
				if (BASE_DATA_YES.equals(dbData.getIsPhysicalSave())) {
					attachSourceEntity.setIsPhysicalSave(dbData.getIsPhysicalSave());
					attachSourceEntity.setIsSecrecy(dbData.getIsSecrecy());
				}
			}

			attachSourceEntitys.add(attachSourceEntity);
			if (Func.isEmpty(attachVO.getAttachSourceId())) {
				newAttachList.add(new Attach() {{
					setId(attachVO.getId());
					setFilePhysicalPosition(attachVO.getFilePhysicalPosition());
				}});
			}
		}
		if (!newAttachList.isEmpty()) {
			attachService.updateBatchById(newAttachList);
		}
		if (!attachSourceEntitys.isEmpty()) {
			this.fixUniqueNum(attachSourceEntitys);
			this.saveOrUpdateBatch(attachSourceEntitys);
		}
		if (!sourceId.isEmpty()){
			attachPermissionLogService.saveLog(sourceId);
		}

		return true;
	}

	private Map<Long,AttachSourceEntity> getDbAttachSource(List<AttachVO> attachVOS) {
		List<Long> attachSourceIds = attachVOS.stream()
			.map(AttachVO::getAttachSourceId).collect(Collectors.toList());
		if (attachSourceIds.isEmpty()) {return Collections.EMPTY_MAP;}
		return this.list(new LambdaQueryWrapper<AttachSourceEntity>()
			.in(AttachSourceEntity::getId,attachSourceIds)).stream()
			.collect(Collectors.toMap(AttachSourceEntity::getId,i->i,(v1,v2)->v1));
	}

	private void checkEditPermission(List<AttachVO> attachRemoveVOS ) {
		List<Long> attachSourceIds = attachRemoveVOS.stream()
			.map(AttachVO::getAttachSourceId).collect(Collectors.toList());
		if (attachSourceIds.isEmpty()) {return;}
		List<AttachSourceEntity> attachSourceEntities = this.list(new LambdaQueryWrapper<AttachSourceEntity>()
			.in(AttachSourceEntity::getId,attachSourceIds));

		for (AttachSourceEntity entity: attachSourceEntities) {
			if (BASE_DATA_YES.equals(entity.getIsPhysicalSave())&&
				BASE_DATA_YES.equals(entity.getIsSecrecy())&&!checkPermissionUserAccount(entity.getPermissionUserAccount())) {
				throw new ServiceException("所选文件中包含保密文件，您没有权限删除！");
			}
		}
	}

	@Override
	@Transactional
	public Boolean removeAttach(AttachSourceDTO attachRemoveVO) {
		if (attachRemoveVO == null || attachRemoveVO.getAttachSourceList() == null || attachRemoveVO.getAttachSourceList().size() == 0) {
			throw new ServiceException("请选择已上传的文件删除！");
		}
		List<AttachVO> attachRemoveVOS = attachRemoveVO.getAttachSourceList();
		List<Long> deleteAttachLogicIds = new ArrayList<>();
		List<Long> deleteAttachSourceLogicIds = new ArrayList<>();

		//文件中是否包含实物保存数据
		long count = this.count(new LambdaQueryWrapper<AttachSourceEntity>()
			.in(AttachSourceEntity::getId,attachRemoveVOS.stream()
				.map(AttachVO::getAttachSourceId).collect(Collectors.toList()))
			.eq(AttachSourceEntity::getIsPhysicalSave, CommonConstant.YES)
			.ne(AttachSourceEntity::getSafekeepStatus,"destoryfile")
		);
		if (count>0) {
			throw new ServiceException("包含实物保存数据，不能删除,请在档案文件收纳系统销毁后删除！");
		}

		//临时上传的文件彻底删除
		List<Long> deleteTemFileIds = new ArrayList<>();
		this.checkEditPermission(attachRemoveVOS);
		for (AttachVO vo : attachRemoveVOS) {
			if (Func.isNotEmpty(vo.getAttachSourceId())) {
				deleteAttachSourceLogicIds.add(vo.getAttachSourceId());
			}
			if (Func.isNotEmpty(vo.getId())) {
				deleteAttachLogicIds.add(vo.getId());
			}

			if (BooleanUtil.isTrue(vo.getTemFile())) {
				deleteTemFileIds.add(vo.getId());
			}
		}



		if (deleteAttachLogicIds.size() > 0) {

			//因为 文件引用功能的存在，所以要判断当删除完了全部的引用之后，再将文件删掉
			List<AttachSourceEntity> attachSourceEntities = this.list(new LambdaQueryWrapper<AttachSourceEntity>()
				.in(AttachSourceEntity::getAttachId, deleteAttachLogicIds));
			Map<Long, Long> map = attachSourceEntities.stream().collect(Collectors.groupingBy(AttachSourceEntity::getAttachId, Collectors.counting()));
			deleteAttachLogicIds = deleteAttachLogicIds.stream()
				.filter(item -> map.get(item) == null || map.get(item) < 2).collect(Collectors.toList());

			if (deleteAttachLogicIds.size() > 0) {
				attachService.deleteLogic(deleteAttachLogicIds);
			}

		}
		if (deleteAttachSourceLogicIds.size() > 0) {
			this.deleteLogic(deleteAttachSourceLogicIds);
		}

		if (deleteTemFileIds.size()>0) {
			attachService.completeRemove(deleteTemFileIds);
		}
		return true;
	}

	@Override
	public Boolean updateAttachSourceDeletedByAttachId(List<Long> idList) {
		List<AttachSourceEntity> attachSourceEntities = this.baseMapper.selectLastUpdateListByAttachIds(idList);
		if (attachSourceEntities.size() == 0) {
			return false;
		}
		List<Long> attachSourceIdList = attachSourceEntities.stream().map(AttachSourceEntity::getId).collect(Collectors.toList());
		//现在的逻辑是 如果有多条被引用的数据只恢复最后一条被引用的数据
		return this.baseMapper.updateAttachSourceDeletedByAttachId(attachSourceIdList);
	}

	@Override
	public List<RegionVo> getRegionList() {
		return this.baseMapper.selectRegionList();
	}

	@Override
	@Transactional
	public boolean changeUserAccountPermission(AttachSourceDTO attachSource) {
		if (attachSource.getId() ==null) {
			throw new ServiceException("附件数据无效，请检查是否上传并保存！");
		}
		String account =attachSource.getPermissionUserAccount();

		String roles = AuthUtil.getUserRole();
		boolean hasPremission = false;
		if ("adminer".equals(AuthUtil.getUserAccount())) {
			hasPremission = true;
		}else if (Func.isNotBlank(roles)) {
			List<String> roleList = Func.toStrList(roles);
			if (roleList.contains(PERMISSION_USER_ROLE_NAME)) {
				hasPremission = true;
			}
		}
		if (!hasPremission) {
			throw new ServiceException("没有权限配置！");
		}
		AttachSourceEntity entity = new AttachSourceEntity(){{
			setId(attachSource.getId());
			setPermissionUserAccount(account==null?"":account);
		}};

		attachPermissionLogService.save(new AttachPermissionLogEntity(){{
			setPermissionAccount(entity.getPermissionUserAccount());
			setAttachSourceId(entity.getId());
			setCreateTime(new Date());
			setCreateBy(AuthUtil.getUserId());
		}});

		return this.updateById(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean postSign(AttachSourceDTO attachRemoveVOS) {

 		if (attachRemoveVOS == null || attachRemoveVOS.getAttachSourceList() == null || attachRemoveVOS.getAttachSourceList().isEmpty()) {
			throw new ServiceException("请选择已上传的文件删除！");
		}
		List<AttachVO> list =attachRemoveVOS.getAttachSourceList();

		List<AttachSourceEntity> attachSourceEntities = this.list(new LambdaQueryWrapper<AttachSourceEntity>()
			.in(AttachSourceEntity::getId,list.stream().map(AttachVO::getAttachSourceId).collect(Collectors.toList()))
			.ne(AttachSourceEntity::getSignStatus,"signStatus_12")
			.ne(AttachSourceEntity::getSignStatus,"signStatus_11")
		) ;


		if (attachSourceEntities.isEmpty()) {
			throw new ServiceException("没有能发起签名的数据！");
		}
		String systemId =ParamCache.getValue("system.id");
		String projectName = ParamCache.getValue("project.name");
		String account = AuthUtil.getUserAccount();
		for (AttachSourceEntity attachSource : attachSourceEntities) {
			SignPdfParam signPdfParam = new SignPdfParam();
			signPdfParam.setSourceSystemId(systemId);
			signPdfParam.setSourceSystemName(projectName);
			signPdfParam.setBelongAccount(account);
			signPdfParam.setFileName(attachSource.getDiyFileName());
			Attach attach = attachService.getById(attachSource.getAttachId());
			signPdfParam.setLink(attach.getLink());
			signPdfParam.setSourceSystemRowId(attachSource.getId().toString());


			attachSource.setSignStatus("signStatus_11");

			R<Object> objectR =  ApiPlatformHttp.commonPost(signPdfParam,ApiPlatformUrlConstant.CREATE_SIGN_URL);
			if (objectR ==null||objectR.getCode()!=200) {
				throw new ServiceException("数据发送失败"+objectR!=null?objectR.getMsg():"null");
			}
		}
		this.updateBatchById(attachSourceEntities);

		return true;
	}

	@Override
	public List<AttachVO> listByOriId(FileUpLoadVO fileUpLoadVO) {
		AttachSourceEntity attachSource = new AttachSourceEntity();
		attachSource.setOriginalId(fileUpLoadVO.getOriginalId());
		List<AttachVO> list =this.baseMapper.selectAttachByOriginalId(attachSource);
		return list;
	}


	private void fixUniqueNum(List<AttachSourceEntity> list) {
		if (list.isEmpty()) {
			return;
		}
		for (AttachSourceEntity attachSource : list) {
			if (Func.isEmpty(attachSource.getFileNum())) {
				attachSource.setFileNum(this.createUniqueNum());
			}
		}


	}

	private String createUniqueNum() {
		return String.valueOf(IdWorker.getId());
//		Random rand = new Random();
//		int number = rand.nextInt(99999999);
//		String randStr =  String.format("%08d",number);
//		long count = this.count(new LambdaQueryWrapper<AttachSourceEntity>().eq(
//			AttachSourceEntity::getFileNum,randStr
//		));
//		if (count>0) {
//			return createUniqueNum();
//		} else {
//			return randStr;
//		}
	}

	/**
	 * 检验借阅权限（是否展示借阅三要素）
	 * @return true 有权限 fasle 没有
	 */
	private boolean checkBorrowing(AttachVO row) {
		if (BASE_DATA_NO.equals(row.getIsPhysicalSave())) {
			return true;
		}
		if (BASE_DATA_NO.equals(row.getIsSecrecy())) {
			if (Func.isBlank(row.getPermissionUserAccount())) {
				return true;
			}else {
				return this.checkPermissionUserAccount(row.getPermissionUserAccount());
			}
		}
		return this.checkPermissionUserAccount(row.getPermissionUserAccount());
	}

	private boolean checkPermissionUserAccount(String permissionUserAccount) {
		String account = AuthUtil.getUserAccount();
		if ("adminer".equals(account)) {
			return true;
		}
		if (Func.isBlank(permissionUserAccount)) {
			return false;
		}
		List<String> pList = Func.toStrList(permissionUserAccount);
		return pList.contains(account);
	}

}
