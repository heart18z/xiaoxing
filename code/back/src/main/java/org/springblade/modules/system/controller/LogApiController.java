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
package org.springblade.modules.system.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.log.model.LogApi;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.system.service.ILogApiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;

/**
 * 控制器
 *
 * @author Chill
 */
@NonDS
@Hidden
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_LOG_NAME + "/api")
public class LogApiController {

	private final ILogApiService logService;

	/**
	 * 查询单条
	 */
	@PostMapping("/detail")
	public R<LogApi> detail(LogApi log) {
		LogApi logApi = logService.getOne(Condition.getQueryWrapper(log));
		String params = logApi.getParams();
		// 过滤掉密码参数 tenantId=000000&username=admin&password=54db909dfc4ff8de7e7e2c33b5c1d710&grant_type=password&scope=all&type=account
		if (params != null) {
			params = params.replaceAll("(?<=password=)[^&]*", "******");
			logApi.setParams(params);
		}
		return R.data(logApi);
	}

	/**
	 * 查询多条(分页)
	 */
	@PostMapping("/list")
	public R<IPage<LogApi>> list(@RequestParam Map<String, Object> log, Query query) {
		IPage<LogApi> pages = logService.page(Condition.getPage(query.setDescs("create_time")), Condition.getQueryWrapper(log, LogApi.class));
		return R.data(pages);
	}

}
