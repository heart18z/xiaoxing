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
package org.springblade.modules.system.wrapper;

import org.apache.commons.collections4.CollectionUtils;
import org.springblade.common.cache.BizParamCache;
import org.springblade.common.cache.UserCache;
import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.node.ForestNodeManager;
import org.springblade.core.tool.node.ForestNodeMerger;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.BizParam;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.vo.BizParamVO;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 包装类,返回视图层所需的字段
 *
 * @author Chill
 */
public class BizParamWrapper extends BaseEntityWrapper<BizParam, BizParamVO> {

	public static BizParamWrapper build() {
		return new BizParamWrapper();
	}

	@Override
	public BizParamVO entityVO(BizParam bizParam) {
		BizParamVO bizParamVO = Objects.requireNonNull(BeanUtil.copy(bizParam, BizParamVO.class));
		if (Func.equals(bizParam.getParentId(), BladeConstant.TOP_PARENT_ID)) {
			bizParamVO.setParentName(BladeConstant.TOP_PARENT_NAME);
		} else {
			BizParam parent = BizParamCache.getById(bizParam.getParentId());
			bizParamVO.setParentName(parent.getParamValue());
		}
		if (bizParamVO.getCreateUser()!=null) {
			User user = UserCache.getUser(bizParamVO.getCreateUser());
			if (user !=null) {
				bizParamVO.setAccount(user.getAccount());
			}
		}
		return bizParamVO;
	}

	public BizParamVO entityVO(BizParamVO bizParamVO) {
		if (Func.equals(bizParamVO.getParentId(), BladeConstant.TOP_PARENT_ID)) {
			bizParamVO.setParentName(BladeConstant.TOP_PARENT_NAME);
		} else {
			BizParam parent = BizParamCache.getById(bizParamVO.getParentId());
			bizParamVO.setParentName(parent.getParamValue());
		}
		return bizParamVO;
	}

	public List<BizParamVO> listNodeVO(List<BizParam> list) {
		List<BizParamVO> collect = list.stream().map(bizParam -> BeanUtil.copy(bizParam, BizParamVO.class)).collect(Collectors.toList());
		return ForestNodeMerger.merge(collect);
	}

	private List<BizParamVO> merge(List<BizParamVO> items) {
		ForestNodeManager<BizParamVO> forestNodeManager = new ForestNodeManager(items);
		for (BizParamVO forestNode : items) {
			if (forestNode.getParentId() != 0L) {
				BizParamVO node = (BizParamVO) forestNodeManager.getTreeNodeAt(forestNode.getParentId());
				if (node != null) {
					node.getChildren().add(forestNode);
				} else {
					forestNodeManager.addParentId(forestNode.getId());
				}
			}
		}
		List<BizParamVO> root = forestNodeManager.getRoot();
		if (CollectionUtils.isNotEmpty(root)) {
			for (BizParamVO bizParamVO : root) {
				this.getLevel(bizParamVO, 1);
			}
		}
		return root;
	}

	private void getLevel(BizParamVO bizParamVO, int i) {
		List<BizParamVO> children = bizParamVO.getChildren();
		if (CollectionUtils.isNotEmpty(children)) {
			for (BizParamVO child : children) {
				i += 1;
				this.getLevel(child, i);
				child.setCurrentHierarchy(i);
			}
		}
		bizParamVO.setCurrentHierarchy(i);
	}
}
