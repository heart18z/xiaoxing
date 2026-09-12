package org.springblade.modules.quartz.template;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import org.springblade.common.rpc.apiplatform.constant.ApiPlatformUrlConstant;
import org.springblade.core.http.HttpRequest;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.vo.SyncPeopleVO;
import org.springblade.modules.quartz.mapper.SysJobMapper;
import org.springblade.modules.quartz.template.support.ColumnMatch;
import org.springblade.modules.quartz.template.support.TaskInfo;
import org.springblade.modules.system.entity.InteractiveEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Component
public class TaskBuilder {

	@Autowired
	private SysJobMapper jobMapper;

	@Autowired
	private JdbcTemplate template;

	private TaskInfo getTaskInfo(String tem) {
		JSONObject jsonObject = JSON.parseObject(tem);
		List<JSONObject> headers = jsonObject.getJSONArray("headers").toJavaList(JSONObject.class);
		List<JSONObject> params = jsonObject.getJSONArray("params").toJavaList(JSONObject.class);
		jsonObject.remove("headers");
		jsonObject.remove("params");

		Map<String,String> headersMap = new HashMap<>();
		if (headers != null && !headers.isEmpty()) {
			headers.forEach(h -> {
				headersMap.put(h.getString("key"),h.getString("value"));
			});
		}
		Map<String,Object> paramsMap = new HashMap<>();
		if (params != null && !params.isEmpty()) {
			params.forEach(p -> {
				paramsMap.put(p.getString("key"),p.getString("value"));
			});
		}
		jsonObject.put("headers",headersMap);
		jsonObject.put("params",paramsMap);

		TaskInfo taskInfo =JSON.toJavaObject(jsonObject, TaskInfo.class);
		return taskInfo;
	}
	private void checkTaskInfo(TaskInfo taskInfo) {
		if (taskInfo == null) {
			throw new ServiceException("taskInfo is null");
		}
		if (Func.isBlank(taskInfo.getUrl())) {
			throw new ServiceException("taskInfo url is null");
		}
		if (Func.isBlank(taskInfo.getMethod() )) {
			throw new ServiceException("taskInfo method is null");
		}
		if (Func.isBlank(taskInfo.getTablePk())) {
			throw new ServiceException("taskInfo params is null");
		}
		if (Func.isNull(taskInfo.getColumnMatches()) || taskInfo.getColumnMatches().isEmpty()) {
			throw new ServiceException("taskInfo params is null");
		}
		taskInfo.getColumnMatches().forEach(m -> {
			if (Func.isBlank(m.getTableColumn())) {
				throw new ServiceException("taskInfo columnMatches tableColumn is null");
			}
			if (Func.isBlank(m.getTargetProp())) {
				throw new ServiceException("taskInfo columnMatches targetColumn is null");
			}
		});
//		if (Func.isBlank(taskInfo.getRes())) {
//			throw new ServiceException("taskInfo res is null");
//		}
//		if (Func.isBlank(taskInfo.getCurrentPage())) {
//			throw new ServiceException("taskInfo currentPage is null");
//		}
	}

	/**
	 * 同步数据
	 */
	@Transactional(rollbackFor = Exception.class)
	public void doSyncByTem(String tem) {
		TaskInfo taskInfo = getTaskInfo(tem);
		checkTaskInfo(taskInfo);
		 doSync(taskInfo);
	}

	private void doSync(TaskInfo taskInfo) {
		if (Func.isNotBlank(taskInfo.getCurrentPage())&&
			Func.isNotBlank(taskInfo.getPageSize())) {
			//最多同步1000次
			for (int i = 1; i < 1000; i++) {
				taskInfo.getParams().put(taskInfo.getCurrentPage(),i);
				taskInfo.getParams().put(taskInfo.getPageSize(),2000);
				List<Map> syncData = getSyncDataByPage(taskInfo.getMethod(), taskInfo.getUrl(),
					taskInfo.getParams(), taskInfo.getRes(),taskInfo.getHeaders());
				if (syncData == null || syncData.isEmpty()) {
					break;
				}
				saveOrUpdateData(syncData,taskInfo);
			}

		}else {
			List<Map> syncData = getSyncDataByPage(taskInfo.getMethod(),taskInfo.getUrl(),
				taskInfo.getParams(), taskInfo.getRes(),taskInfo.getHeaders());
			if (syncData == null || syncData.isEmpty()) {
				return;
			}
			saveOrUpdateData(syncData,taskInfo);
		}


	}

	private List<Map> getSyncDataByPage(String method, String url, Map<String, Object> params, String resProp,Map<String,String> headers) {
		if ("get".equalsIgnoreCase(method)) {

		JsonNode res = HttpRequest.get(url)
			.queryMap(params)
			.addHeader(headers)
			.addHeader()
			.execute()
			.onSuccess(responseSpec -> responseSpec.asJsonNode());
		if (Func.isBlank(resProp)) {
			return JSON.parseArray(res.toString(),Map.class);
		}else {
			String[] props = Func.split(resProp,".");
			try {
				for (String prop : props) {
					res = res.get(prop);
				}
				return JSON.parseArray(res.toString(),Map.class);
			}catch (Exception e) {
				throw new ServiceException("解析返回数据异常！");
			}
		}
		}else if ("post".equalsIgnoreCase(method)) {
			JsonNode res = HttpRequest.post(url)
				.bodyJson(params)
				.addHeader(headers)
				.execute()
				.onSuccess(responseSpec -> responseSpec.asJsonNode());
			if (Func.isBlank(resProp)) {
				return JSON.parseArray(res.toString(),Map.class);
			}else {
				String[] props = Func.split(resProp,".");
				try {
					for (String prop : props) {
						res = res.get(prop);
					}
					return JSON.parseArray(res.toString(),Map.class);
				}catch (Exception e) {
					throw new ServiceException("解析返回数据异常！");
				}
			}
		}else {
			throw new ServiceException("method is not support");
		}

	}

	private void saveOrUpdateData(List<Map> syncData,TaskInfo taskInfo) {
		// 保存数据
		List<ColumnMatch> columnMatches = taskInfo.getColumnMatches();
		String updateColumn = columnMatches.stream().filter(n -> n.getTargetProp().equals(taskInfo.getUpdateColumn()))
			.map(ColumnMatch::getTableColumn).findFirst().orElse(null);
			if (Func.isBlank(updateColumn)) {
				throw new ServiceException("updateColumn is null");
			}
		String tablePk = taskInfo.getTablePk();
		Boolean isNeedCreatedId = taskInfo.getIsNeedCreatedId();
		List<Map> saveOrUpdateData = syncData.stream().map(d -> {
			Map<String, Object> saveData = new HashMap<>();
			columnMatches.forEach(m -> {
				saveData.put(m.getTableColumn(),d.get(m.getTargetProp()));
			});
			return saveData;
		}).collect(Collectors.toList());

		// 保存.更新数据
		List<String> ids = saveOrUpdateData.stream().map(d -> Func.toStr(d.get(updateColumn))).collect(Collectors.toList());

		List<String> existIds = jobMapper.selectIdsById(ids,taskInfo.getTableName(),updateColumn);
		List<Map> saveData = saveOrUpdateData.stream().filter(d -> !existIds.contains(Func.toStr(d.get(updateColumn)))).collect(Collectors.toList());
		List<Map> updateData = saveOrUpdateData.stream().filter(d -> existIds.contains(Func.toStr(d.get(updateColumn)))).collect(Collectors.toList());


		if (Func.isNotEmpty(saveData)) {
			if (isNeedCreatedId!=null &&isNeedCreatedId) {
				saveData.forEach(d -> {
					d.put(tablePk, IdWorker.getId());
				});
			}
			StringBuilder sql = new StringBuilder("");
			sql.append("insert into ").append(taskInfo.getTableName()).append(" (");
			List<String> dataKey = columnMatches.stream().map(ColumnMatch::getTableColumn)
				.collect(Collectors.toList());
			sql.append(String.join(",",dataKey));
			sql.append(") values ");
			sql.append(saveData.stream().map(d -> {
				StringBuilder dataSql = new StringBuilder("");
				dataSql.append("(");
				String values = dataKey.stream().map(k ->
					"'"+Func.toStr(d.get(k))+"'").collect(Collectors.joining(","));
				dataSql.append(values);
				dataSql.append(")");
				return dataSql.toString();
				}).collect(Collectors.joining(",")));

				//TODO 保存数据
			template.execute(sql.toString());
		}
		if (Func.isNotEmpty(updateData)) {
			StringBuilder sql = new StringBuilder("");
			sql.append("update ").append(taskInfo.getTableName()).append(" set ");
			List<String> dataKey = columnMatches.stream().map(ColumnMatch::getTableColumn)
				.filter(i->(!updateColumn.equals(i)) && (!tablePk.equals(i))).collect(Collectors.toList());
			sql.append(String.join(",",
				dataKey.stream().map(k -> k + " = ?").collect(Collectors.toList())));
			sql.append(" where ").append(updateColumn).append(" = ?");
			updateData.forEach(d -> {
				List<String> strings = dataKey.stream().map(k -> Func.toStr(d.get(k))).collect(Collectors.toList());
				strings.add(d.get(updateColumn).toString());
				String[] arr = strings.toArray(new String[0]);

				template.update(sql.toString(),arr);
			});
		}

	}
}
