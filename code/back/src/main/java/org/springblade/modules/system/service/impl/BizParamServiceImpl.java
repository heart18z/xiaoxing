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

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.springblade.common.cache.BizParamCache;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.rpc.apiplatform.constant.ApiPlatformUrlConstant;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformHttp;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformUtils;
import org.springblade.common.utils.CommonUtil;
import org.springblade.common.utils.TreeUtils;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.node.ForestNodeMerger;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringPool;
import org.springblade.modules.quartz.constants.ScheduleConstants;
import org.springblade.modules.quartz.support.BaseDictVO;
import org.springblade.modules.system.entity.BizParam;
import org.springblade.modules.system.entity.InteractiveEntity;
import org.springblade.modules.system.mapper.BizParamMapper;
import org.springblade.modules.system.service.IBizParamService;
import org.springblade.modules.system.service.IInteractiveService;
import org.springblade.modules.system.vo.BizParamParam;
import org.springblade.modules.system.vo.BizParamVO;
import org.springblade.modules.system.wrapper.BizParamWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static org.springblade.core.cache.constant.CacheConstant.DICT_CACHE;

/**
 * 业务参数表 服务实现类
 *
 * @author BladeX
 * @since 2022-03-14
 */
@Service
public class BizParamServiceImpl extends BaseServiceImpl<BizParamMapper, BizParam> implements IBizParamService {
    private final Integer MAX_LEVEL = 10;
    @Resource
    private BizParamMapper bizParamMapper;
	@Autowired
	private IInteractiveService iInteractiveService;




    @Override
    public List<BizParamVO> tree() {
        return ForestNodeMerger.merge(baseMapper.tree());
    }



    @Override
    public BizParamVO treeByPath(String path) {
        String tenantId = AuthUtil.getTenantId();
        // 递归拼接sql
        List<String> paramValues = Arrays.asList(path.split("\\."));
        String tableName = BizParam.class.getAnnotation(TableName.class).value();

        // 按照等级查询 所有创建租户为自己 和 性质为平台的参数
        String sql = deepSql(tenantId, tableName, paramValues, 0);
        BizParam bizParam = baseMapper.selectOneBySql(sql);

        if (bizParam == null) {
            return null;
        }

        // 查询树形子级列表
        List<BizParam> child = new ArrayList<>();
        child.add(bizParam);
        findChildren(child, Arrays.asList(bizParam.getId()), 0);
        List<BizParamVO> paramVOS = TreeUtils.parseTree(child,
                BizParamVO.class,
                BizParamVO::getId,
                BizParamVO::getParentId,
                BizParamVO::getChildren,
                BizParamVO::setChildren,
                Objects::deepEquals,
                Comparator.comparing(BizParamVO::getSort));
        return paramVOS.get(0);
    }

    public void findChildren(List<BizParam> root, List<Long> parentIds, int level) {
        Wrapper queryWrapper = new QueryWrapper<BizParam>().lambda()
                .in(BizParam::getParentId, parentIds)
                .eq(BizParam::getIsDeleted, BladeConstant.DB_NOT_DELETED)
                .eq(BizParam::getStatus,BladeConstant.DB_STATUS_NORMAL);

        List<BizParam> children = super.list(queryWrapper);
        if (CollectionUtils.isEmpty(children) || level >= MAX_LEVEL) return;
        root.addAll(children);
        List<Long> ids = children.stream().map(BizParam::getId).collect(Collectors.toList());

        int finalLevel = ++level;
        findChildren(root, ids, finalLevel);
    }

    static String deepSql(String tenantId, String tableName, List<String> paramValues, int index) {
        String select = index == 0 ? "*" : "id";
        String deep = index == paramValues.size() - 1 ? "'0'" : deepSql(tenantId, tableName, paramValues, index + 1);
        return String.format("SELECT %s FROM %s WHERE param_value = '%s' AND parent_id IN (%s) AND is_deleted = 0 and status=1",
                select,
                tableName,
                paramValues.get(paramValues.size() - index - 1),
                deep
                );
    }

    @Override
    public List<BizParamVO> parentTree() {
        return ForestNodeMerger.merge(baseMapper.parentTree());
    }

    @Override
    public List<BizParamVO> privatePlatformTree() {
        List<BizParam> bizParams = baseMapper.deepChild(0L, "and is_deleted=0 " );
        return TreeUtils.parseTree(bizParams,
                BizParamVO.class,
                BizParamVO::getId,
                BizParamVO::getParentId,
                BizParamVO::getChildren,
                BizParamVO::setChildren,
                Objects::deepEquals,
                Comparator.comparing(BizParamVO::getSort));
    }

    @Override
    public String getValue(String paramName, String paramKey) {
        return Func.toStr(baseMapper.getValue(paramName, paramKey), StringPool.EMPTY);
    }

    @Override
    public List<BizParam> getList(String paramName) {
		if (Func.isBlank(paramName)) {
			return Collections.emptyList();
		}
		return baseMapper.getList(paramName);

    }


    @Override
    public BizParam submit(BizParam bizParam) {
//        LambdaQueryWrapper<BizParam> lqw = Wrappers.<BizParam>query().lambda().eq(BizParam::getParamName, bizParam.getParamName()).eq(BizParam::getParamKey, bizParam.getParamKey());
//        Long cnt = baseMapper.selectCount((Func.isEmpty(bizParam.getId())) ? lqw : lqw.notIn(BizParam::getId, bizParam.getId()));
//        if (cnt > 0) {
//            throw new ServiceException("当前业务参数键值已存在!");
//        }

        if (bizParam.getParentId() == null) {
			LambdaQueryWrapper<BizParam> lqw = Wrappers.<BizParam>query().lambda().eq(BizParam::getParamKey, bizParam.getParamKey()).eq(BizParam::getParentId, 0);
			Long cnt = baseMapper.selectCount((Func.isEmpty(bizParam.getId())) ? lqw : lqw.notIn(BizParam::getId, bizParam.getId()));
            if (cnt > 0) {
                throw new ServiceException("当前业务参数键已存在!");
            }
        }

        // 修改顶级业务参数后同步更新下属业务参数的编号
        if (Func.isNotEmpty(bizParam.getId()) && bizParam.getParentId().longValue() == BladeConstant.TOP_PARENT_ID) {
            BizParam parent = BizParamCache.getById(bizParam.getId());
            this.update(Wrappers.<BizParam>update().lambda()
                    .set(BizParam::getHierarchical, bizParam.getHierarchical())
                    .set(BizParam::getParamKey, bizParam.getParamKey())
                    .eq(BizParam::getParamKey, parent.getParamKey())
                    .ne(BizParam::getParentId, BladeConstant.TOP_PARENT_ID));
        }
        if (Func.isEmpty(bizParam.getParentId())) {
            bizParam.setParentId(BladeConstant.TOP_PARENT_ID);
        }
        bizParam.setIsDeleted(BladeConstant.DB_NOT_DELETED);
		bizParam.setIsPublicParam("0");
        CacheUtil.clear(DICT_CACHE, Boolean.FALSE);
        boolean success = saveOrUpdate(bizParam);
        return success ? bizParam : null;
    }

    @Override
    public boolean removeBizParam(String ids) {
        Long cnt = baseMapper.selectCount(Wrappers.<BizParam>query().lambda().in(BizParam::getParentId, Func.toLongList(ids)));
        if (cnt > 0) {
            throw new ServiceException("请先删除子节点!");
        }

		return removeByIds(Func.toLongList(ids));
    }

    @Override
    public IPage<BizParamVO> parentList(BizParamVO bizParam, Query query) {
		IPage<BizParamVO> page = Condition.getPage(query);
		return this.baseMapper.selectBizParentPage(page, bizParam);

//        IPage<BizParam> page = this.page(Condition.getPage(query),
//			Condition.getQueryWrapper(bizParam, BizParam.class).lambda()
//				.eq(BizParam::getParentId, CommonConstant.TOP_PARENT_ID)
//				.orderByAsc(BizParam::getSort,BizParam::getId));
//        return BizParamWrapper.build().pageVO(page);
    }

    @Override
    public List<BizParamVO> childList(Map<String, Object> bizParam, Long parentId) {
        bizParam.remove("parentId");
		LambdaQueryWrapper<BizParam> queryWrapper = Condition.getQueryWrapper(bizParam, BizParam.class)
			.lambda()
			.orderByAsc(BizParam::getSort);
		if (parentId > -1) {
			BizParam parentBizParam = BizParamCache.getById(parentId);
			queryWrapper =queryWrapper.ne(BizParam::getId, parentId).eq(BizParam::getParamKey, parentBizParam.getParamKey());
        }

        List<BizParam> list = this.list(queryWrapper);
        return BizParamWrapper.build().listNodeVO(list);
    }

    @Override
    public BizParamVO getOne(BizParam bizParam) {
        return baseMapper.getOne(bizParam);
    }



    private Collection<BizParam> getNoPermissionParams(Collection<BizParam> bizParams, boolean isRemove) {
        // 过滤出不是当前用户创建的参数
        Set<BizParam> notOwnCreateParams = bizParams.stream()
                .filter(bizParam -> !Objects.deepEquals(bizParam.getCreateUser(), AuthUtil.getUserId()))
                .collect(Collectors.toSet());
        if (notOwnCreateParams.isEmpty()) {
            return notOwnCreateParams;
        }

        if (isRemove) {
            // 判断要删除的参数的父级参数是否是他自己创建的，如果父级参数是他创建的则可以删除，否则不可以删除
            // 查询所有父级参数
            Set<Long> parentParamIds = notOwnCreateParams.stream()
                    .map(BizParam::getParentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            List<BizParam> parentParams = list(Wrappers.<BizParam>lambdaQuery().in(BizParam::getId, parentParamIds));

            // 得到父级参数的创建者
            List<BizParam> noPermissionParams = new ArrayList<>();
            Map<Long, BizParam> idParentParamMap = parentParams.stream().collect(Collectors.toMap(BizParam::getId, bizParam -> bizParam));
            for (BizParam bizParam : notOwnCreateParams) {
                BizParam parentParam = idParentParamMap.get(bizParam.getParentId());
                if (parentParam == null || parentParam.getCreateUser() == null) {
                    continue;
                }
                if (!Objects.deepEquals(AuthUtil.getUserId(), parentParam.getCreateUser())) {
                    // 如果当前用户不是父级参数的创建者，说明没有权限
                    noPermissionParams.add(bizParam);
                }
            }
            return noPermissionParams;
        } else if (AuthUtil.isAdministrator()) {
            // 不是删除，当前用户是最高管理租户
            return Collections.emptyList();
        }
        return bizParams;
    }


    @Override
    public String getParamNameByParamKeyAndParamValue(String ParamKey, String ParamValue) {
        QueryWrapper<BizParam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("param_key", ParamKey);
        queryWrapper.eq("param_value", ParamValue);
        BizParam bizParam = bizParamMapper.selectOne(queryWrapper);
        return Optional.ofNullable(bizParam).map(biz -> bizParam.getParamName()).orElse(null);
    }


    @Override
    public Long getParamIdByParamKeyAndParamValue(String ParamKey, String ParamValue) {
        QueryWrapper<BizParam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("param_key", ParamKey);
        queryWrapper.eq("param_value", ParamValue);
        BizParam bizParam = bizParamMapper.selectOne(queryWrapper);
        return Optional.ofNullable(bizParam).map(biz -> bizParam.getId()).orElse(null);
    }

    @Override
    public String getParamNameById(Long id) {
        BizParam bizParam = bizParamMapper.selectById(id);
        return Optional.ofNullable(bizParam).map(biz -> bizParam.getParamName()).orElse(null);
    }

	@Override
	public String getParamNameByParamValue(String paramValue) {
    	if (StringUtils.isEmpty(paramValue)){
    		return null;
		}
		BizParam bizParam = bizParamMapper.selectOne(new QueryWrapper<BizParam>().eq("param_value",paramValue));
		return Optional.ofNullable(bizParam).map(biz -> bizParam.getParamName()).orElse(null);
	}

	@Override
	public List<BizParam> getParamsByParamValues(String paramValues) {
		if (StringUtils.isEmpty(paramValues)){
			return null;
		}
		return bizParamMapper.selectList(new QueryWrapper<BizParam>().in("param_value", Func.toStrList(paramValues)));
	}

	@Override
    public boolean getNoPermissionParams(BizParam bizParam) {
        // 新增
        if (bizParam.getId() == null) {
            return true;
        }
		if (AuthUtil.getUser().getRoleName().contains("admin")) {
			return true;
		}
        // 修改
        List<BizParam> bizParams = Collections.singletonList(bizParam);
        return getNoPermissionParams(bizParams, false).isEmpty();
    }

    @Override
    public Collection<BizParam> getNoPermissionParams(String bizParamIds, boolean isRemove) {
        List<String> bizParamIdList = Arrays.asList(bizParamIds.split(","));
        List<BizParam> bizParams = list(Wrappers.<BizParam>lambdaQuery().in(BizParam::getId, bizParamIdList));
		if (AuthUtil.isAdministrator()) {
			return Collections.emptyList();
		}
        return getNoPermissionParams(bizParams, isRemove);
    }

	@Override
	public List<BizParamVO> dictionary(BizParamParam bizParamParam) {
		String paramValue = bizParamParam.getParamValue();
		boolean isTree = bizParamParam.getIsTree() != null && bizParamParam.getIsTree();
//		Boolean isPublic = bizParamParam.getIsPublic() != null && bizParamParam.getIsPublic();
		String isRemote = ParamCache.getValue(CommonConstant.REMOTE_BASE_DICT_PARAM_KEY);
		List<BizParamVO> result=Collections.emptyList();
		String systemCode = ParamCache.getValue("system.code");
		String systemId = ParamCache.getValue("system.id");
		if ("true".equals(isRemote)) {
			//System.out.println("remote----->");
			JsonNode res = ApiPlatformHttp.commonGetByInteractive(new HashMap<String,Object>(){{
				put("zdzdm",paramValue);
				put("code_no",systemCode);
				put("xtbh",systemId);

			}},
				ApiPlatformUrlConstant.GET_BASE_INTERACTIVE_CODE,
				ApiPlatformUrlConstant.GET_BASE_DICT);
			//解析返回结果
			List<BaseDictVO> baseDictVOS =  JSON.parseArray(ApiPlatformUtils.getDataFromJsonNode(res),BaseDictVO.class);

			result = baseDictVOS.stream()
				.map(this::buildBaseParam)
				.collect(Collectors.toList());
		} else {
			System.out.println("local----->");
			BizParam bizParam = this.getOne(new QueryWrapper<BizParam>().eq("param_value", paramValue));
			if (Optional.ofNullable(bizParam).isPresent()){
				result = this.baseMapper.deepChild(bizParam.getId()," limit 10000 ").stream().map(i->{
					BizParamVO v = BeanUtil.copy(i, BizParamVO.class);
					v.setLabel(i.getParamName());
					v.setValue(i.getParamValue());
					return v;
				}).collect(Collectors.toList());
			}

		}




		if (isTree) {
			result = ForestNodeMerger.merge(result);
		}
		return result;


	}

	@Override
	public void cleanAllData() {
		this.baseMapper.deleteAllData();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void cleanAllSyncData() {
		List<BizParam> bizParams = this.list();
		List<BizParamVO> tree = ForestNodeMerger.merge(
			bizParams.stream().map(i->BeanUtil.copy(i,BizParamVO.class))
				.collect(Collectors.toList())
		);
		List<BizParam> localParams = bizParams.stream()
			.filter(i->ScheduleConstants.DATA_SOURCE_ADD.equals(i.getIsSync())).collect(Collectors.toList());

		Set<BizParamVO> localParamParentsSet = new HashSet<>();
		localParams.forEach(i->{
			List<BizParamVO> vos = TreeUtils.getPath(tree,BizParamVO::getChildren,item->i.getId().equals(item.getId()));
			if (vos!=null) {
				localParamParentsSet.addAll(vos);
			}
		});
		this.cleanAllData();
		this.saveBatch(localParamParentsSet.stream().map(i->{
			BizParam ent =BeanUtil.copy(i,BizParam.class);
			ent.setIsSync(ScheduleConstants.DATA_SOURCE_ADD);
			return  ent;
		}).collect(Collectors.toList()));

	}


	private BizParamVO buildBaseParam (BaseDictVO vo) {
		return  new BizParamVO() {{

			setLabel(vo.getZdzmc());
			setValue(vo.getBjgdm());
			setId(vo.getZdzj());
			setParentId(vo.getFjzj());
			setParamValue(vo.getBjgdm());
			setParamName(vo.getZdzmc());
			setParamKey(vo.getZdbm());
			setCurrentHierarchy(vo.getZdzcj());
			setHierarchical(vo.getCjxz());
			setStatus(vo.getZdkfzt());
			setIsSync(ScheduleConstants.DATA_SOURCE_SYNC);
		}};
	}
}
