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
package org.springblade.common.launch;

import org.springblade.common.constant.LauncherConstant;
import org.springblade.core.auto.service.AutoService;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.launch.service.LauncherService;
import org.springblade.core.launch.utils.PropsUtil;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Properties;

/**
 * 启动参数拓展
 *
 * @author smallchil
 */
@AutoService(LauncherService.class)
public class LauncherServiceImpl implements LauncherService {

	public static final String PRE_CODE = "pre";
	public static final String EXTRACT_CODE = "extract";


	@Override
	public void launcher(SpringApplicationBuilder builder, String appName, String profile, boolean isLocalDev) {
		Properties props = System.getProperties();
		// 本地 dev 不使用 Nacos 配置中心（BladeApplication 默认会注入 optional:nacos: 导入）
		if (isLocalDev && AppConstant.DEV_CODE.equals(profile)) {
			Properties localProps = new Properties();
			localProps.setProperty("spring.config.import", "");
			builder.properties(localProps);
		}
		PropsUtil.setProperty(props, "spring.cloud.sentinel.transport.dashboard", LauncherConstant.sentinelAddr(profile));
		PropsUtil.setProperty(props, "spring.datasource.dynamic.enabled", "false");
		// 开启elk日志
		//PropsUtil.setProperty(props, "blade.log.elk.destination", LauncherConstant.elkAddr(profile));
		if (profile.equals(AppConstant.PROD_CODE) || profile.equals(PRE_CODE)|| profile.equals(EXTRACT_CODE)){
			props.put("logging.config", "http://" + props.get("NACOS_ADDR") + "/nacos/v1/cs/configs?group=" + props.get("server.port") + "&tenant=" + profile + "&username="+ props.get("NACOS_USERNAME") + "&password=" + props.get("NACOS_PASSWORD") + "&dataId=" + "logback-" + profile + ".xml");
			PropsUtil.setProperty(props, "spring.cloud.nacos.config.server-addr", String.valueOf(props.get("NACOS_ADDR")));
			PropsUtil.setProperty(props, "spring.cloud.nacos.discovery.server-addr", String.valueOf(props.get("NACOS_ADDR")));

			PropsUtil.setProperty(props, "spring.cloud.nacos.username", String.valueOf(props.get("NACOS_USERNAME")));
			PropsUtil.setProperty(props, "spring.cloud.nacos.password", String.valueOf(props.get("NACOS_PASSWORD")));

			PropsUtil.setProperty(props, "spring.cloud.nacos.namespace", profile);

		}
	}

}
