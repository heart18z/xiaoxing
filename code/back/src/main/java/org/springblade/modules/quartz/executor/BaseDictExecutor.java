package org.springblade.modules.quartz.executor;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.rpc.apiplatform.constant.ApiPlatformUrlConstant;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformHttp;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformUtils;
import org.springblade.common.utils.CommonUtil;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.entity.ScheduledLogEntity;
import org.springblade.modules.desk.service.IScheduledLogService;
import org.springblade.modules.desk.vo.AddressSyncVO;
import org.springblade.modules.quartz.constants.ScheduleConstants;
import org.springblade.modules.quartz.enums.BizTaskEnum;
import org.springblade.modules.quartz.support.BaseDictVO;
import org.springblade.modules.system.entity.BizParam;
import org.springblade.modules.system.entity.PropertyRegionEntity;
import org.springblade.modules.system.mapper.BizParamMapper;
import org.springblade.modules.system.service.IBizParamService;
import org.springblade.modules.system.service.IPropertyRegionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.springblade.core.cache.constant.CacheConstant.DICT_CACHE;
import static org.springblade.core.cache.constant.CacheConstant.RESOURCE_CACHE;

@Component
@AllArgsConstructor
public class BaseDictExecutor {


	private final IScheduledLogService scheduledLogService;

	private final IBizParamService bizParamService;

	private final BizParamMapper bizParamMapper;

	private final HashMap<String,String> sjlyIsPublicMap=new HashMap<String,String>(){{
		put("读取","1");
		put("创建","0");
	}};


	@Transactional
	public void doSyncBizParamByInterface(String triggerMode) {

		Long zeroL = 0L;

		List<BaseDictVO> baseDictVOS;
		Date now = new Date();
		try {
			//全量同步，先清除初始数据
			bizParamMapper.deleteAllData();
			String systemCode = ParamCache.getValue("system.code");
			String systemId = ParamCache.getValue("system.id");
			baseDictVOS = getBaseDictData(new HashMap<String,Object>(){{
				put("code_no",systemCode);
				put("xtbh",systemId);
			}});



			if (Func.isNotBlank(systemCode) && Func.isNotBlank(systemCode.trim()) ) {
				baseDictVOS.addAll(
					getBaseDictData(new HashMap<String,Object>())
				);
				baseDictVOS = baseDictVOS.stream().filter(CommonUtil.distinctByKey(BaseDictVO::getZdzj)).collect(Collectors.toList());
			}

			if (baseDictVOS ==null) {
				throw new ServiceException("获取接口数据失败！");
			}
			if (!baseDictVOS.isEmpty()) {
				// 1、创建祖父级
				// 	1)去重

				List<BizParam> uniqueByZdbm = baseDictVOS.stream()
						.filter(CommonUtil.distinctByKey(BaseDictVO::getZdbm))
						.map(i->{
							BizParam entity = buildBaseParam(i,now);
							entity.setId(IdWorker.getId());
							entity.setParamValue(i.getZdbm());
							entity.setParamName(i.getZdmc());
							entity.setSort(9999);
							entity.setParentId(zeroL);
							return entity;
						})
						.collect(Collectors.toList());
				bizParamService.saveBatch(uniqueByZdbm);
				Map<String,Long> zdbmIdMap =  uniqueByZdbm.stream().collect(Collectors.toMap(
						BizParam::getParamKey,BizParam::getId,(v1,v2)->v1
				));


				List<BizParam> saveData = baseDictVOS.stream()
					.sorted(Comparator.comparing(BaseDictVO::getZdzdm))
					.map(vo->{
					BizParam entity =buildBaseParam(vo,now);
					if (zeroL.equals(entity.getParentId()) || entity.getParentId()==null) {
						entity.setParentId(zdbmIdMap.get(entity.getParamKey()));
					}
					entity.setSort(9999);
					return entity;
				}).collect(Collectors.toList());

				if ((!uniqueByZdbm.isEmpty())|| !saveData.isEmpty()) {
					CacheUtil.clear(DICT_CACHE, Boolean.FALSE);
				}
				if (!saveData.isEmpty()) {
//					syncSize =saveData.size();
					bizParamService.saveBatch(saveData);

				}
			}
/*			log.setUpdateRows(syncSize);
			log.setTaskContent("共执行"+syncSize+"条数据！");
			log.setSuccess(CommonConstant.YES);*/

		}catch (Exception e) {
//			log.setTaskContent("数据同步任务执行失败！"+e);
//			log.setSuccess(CommonConstant.NO);
			throw new ServiceException("数据同步任务执行失败！"+e.getMessage());

		}finally {
//			log.setEndTime(new Date());
//			scheduledLogService.saveData(log);
		}

	}

	private BizParam buildBaseParam (BaseDictVO vo,Date now) {
		String isPublicParam = this.getIsPublicParamByMap(vo.getZdly());
		return  new BizParam() {{
			setId(vo.getZdzj());
			setParentId(vo.getFjzj());
			setParamValue(vo.getBjgdm());
			setParamName(vo.getZdzmc());
			setParamKey(vo.getZdbm());
			setCurrentHierarchy(vo.getZdzcj());
			setHierarchical(vo.getCjxz());
			setStatus(vo.getZdkfzt());
			setSystemSource(vo.getXtmc());
			setIsPublicParam(isPublicParam);
			setIsSync(ScheduleConstants.DATA_SOURCE_SYNC);
			setCreateTime(now);
		}};
	}

	private String getIsPublicParamByMap(String zdly){
		return this.sjlyIsPublicMap.get(zdly);
	};


	private List<BaseDictVO> getBaseDictData(HashMap<String,Object> params) {
		JsonNode res = ApiPlatformHttp.commonGetByInteractive(params,
			ApiPlatformUrlConstant.GET_BASE_INTERACTIVE_CODE,
			ApiPlatformUrlConstant.GET_BASE_DICT);
		List<BaseDictVO> result =  JSON.parseArray(ApiPlatformUtils.getDataFromJsonNode(res),BaseDictVO.class);
		//解析返回结果
		return result ==null?Collections.emptyList():result;
	}
}
