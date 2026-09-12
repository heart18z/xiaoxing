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
package org.springblade.modules.aiconfig.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import com.alibaba.fastjson.JSON;

import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.common.config.MaitalkConfig;
import org.springblade.modules.aiconfig.entity.BtnConfigEntity;
import org.springblade.modules.aiconfig.excel.BtnConfigExcel;
import org.springblade.modules.aiconfig.service.IBtnConfigService;
import org.springblade.modules.aiconfig.vo.BtnConfigVO;
import org.springblade.modules.aiconfig.wrapper.BtnConfigWrapper;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * AI按钮接口配置表 控制器
 *
 * @author wxd
 * @since 2026-03-25
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("blade-ai/btnConfig")
@Tag(name = "AI按钮接口配置表接口", description = "AI按钮接口配置表")
public class BtnConfigController extends BladeController {

	private final IBtnConfigService btnConfigService;
	private final MaitalkConfig maitalkConfig;

	/**
	 * AI按钮接口配置表 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入btnConfig")
	public R<BtnConfigVO> detail(BtnConfigEntity btnConfig) {
		BtnConfigEntity detail = btnConfigService.detail(btnConfig);
		BtnConfigVO vo = BtnConfigWrapper.build().entityVO(detail);
		vo.setMaitalkBaseUrl(Func.isBlank(maitalkConfig.getBaseUrl()) ? "" : maitalkConfig.getBaseUrl().trim());
		return R.data(vo);
	}
	/**
	 * AI按钮接口配置表 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入btnConfig")
	public R<IPage<BtnConfigVO>> list(@RequestParam Map<String, Object> btnConfig, Query query) {
		IPage<BtnConfigEntity> pages = btnConfigService.page(Condition.getPage(query), Condition.getQueryWrapper(btnConfig, BtnConfigEntity.class));
		return R.data(BtnConfigWrapper.build().pageVO(pages));
	}

	/**
	 * AI按钮接口配置表 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入btnConfig")
	public R<IPage<BtnConfigVO>> page(BtnConfigVO btnConfig, Query query) {
		IPage<BtnConfigVO> pages = btnConfigService.selectBtnConfigPage(Condition.getPage(query), btnConfig);
		return R.data(pages);
	}

	/**
	 * AI按钮接口配置表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入btnConfig")
	public R save(@Valid @RequestBody BtnConfigEntity btnConfig) {
		return R.status(btnConfigService.save(btnConfig));
	}

	/**
	 * AI按钮接口配置表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入btnConfig")
	public R update(@Valid @RequestBody BtnConfigEntity btnConfig) {
		return R.status(btnConfigService.updateById(btnConfig));
	}

	/**
	 * AI按钮接口配置表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入btnConfig")
	public R submit(@Valid @RequestBody BtnConfigEntity btnConfig) {
		return R.status(btnConfigService.submit(btnConfig));
	}

	/**
	 * AI按钮接口配置表 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(btnConfigService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * AI 测试对话：从请求体读取 aiInterfaceUrl（与按钮配置一致），其余字段去掉该键后原样转发（HttpRequest）。
	 */
	@PostMapping("/ai-test-proxy")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "AI测试对话转发", description = "请求体需含 aiInterfaceUrl，转发至该地址时会移除 aiInterfaceUrl 键")
	public R<Object> aiTestProxy(@RequestBody JsonNode body) {
		if (body == null || !body.isObject()) {
			return R.fail("请求体必须为 JSON 对象");
		}
		ObjectNode root = (ObjectNode) body;
		JsonNode urlNode = root.get("aiInterfaceUrl");
		if (urlNode == null || !urlNode.isTextual() || Func.isBlank(urlNode.asText())) {
			return R.fail("缺少 AI 接口地址：请在按钮配置中填写「AI接口地址」(aiInterfaceUrl)");
		}
		String rawUrl = urlNode.asText().trim();
		String targetUrl = rawUrl;
		if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
			String base = maitalkConfig.getBaseUrl();
			if (Func.isBlank(base)) {
				return R.fail("AI 接口地址为相对路径，但未配置 maitalk-config.base-url");
			}
			base = base.trim();
			if (rawUrl.startsWith("/")) {
				targetUrl = base.endsWith("/") ? (base.substring(0, base.length() - 1) + rawUrl) : (base + rawUrl);
			} else {
				targetUrl = base.endsWith("/") ? (base + rawUrl) : (base + "/" + rawUrl);
			}
		}
		ObjectNode forward = root.deepCopy();
		forward.remove("aiInterfaceUrl");
		String bodyStr = forward.toString();

		try {
			String res = HttpRequest.post(targetUrl)
				.header("content-type", "application/json")
				.body( bodyStr)
				.timeout(1000000)
				.execute()
				.body();

			if (res == null) {
				return R.fail("外部接口无响应");
			}
			try {
				return R.data(JSON.parse(res));
			} catch (Exception ex) {
				return R.data((Object) res);
			}
		} catch (Exception e) {
			log.error("AI 测试转发失败: {}", e.getMessage(), e);
			return R.fail("调用外部接口失败：" + e.getMessage());
		}
	}

	/**
	 * 请求日志查询：由后端代调用 maitalk 的日志分页接口，避免前端跨域。
	 * GET: /blade-supply/btnConfig/requestLog?uuid=...&current=1&size=10
	 */
	@PostMapping("/requestLog")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "请求日志分页", description = "通过 maitalk-config.base-url 代调 /api/blade-requestLog/requestLog")
	public R<Object> requestLog(
		@Parameter(description = "弹窗会话 uuid", required = true) @RequestParam String uuid,
		@Parameter(description = "页码", required = false) @RequestParam(required = false, defaultValue = "1") Integer current,
		@Parameter(description = "每页条数", required = false) @RequestParam(required = false, defaultValue = "10") Integer size
	) {
		if (Func.isBlank(uuid)) {
			return R.fail("uuid 不能为空");
		}
		String base = maitalkConfig.getBaseUrl();
		if (Func.isBlank(base)) {
			return R.fail("未配置 maitalk-config.base-url");
		}
		base = base.trim();
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		String url = base + "/api/blade-requestLog/requestLog/list";

		try {
			Map<String, Object> qs = new LinkedHashMap<>();
			qs.put("sessionId", uuid.trim());
			qs.put("current", current == null ? 1 : current);
			qs.put("size", size == null ? 10 : size);

			String res = HttpRequest.get(url)
				.form(qs)
				.timeout(1000000)
				.execute()
				.body();

			if (res == null) {
				return R.fail("外部接口无响应");
			}
			try {
				return R.data(JSON.parse(res));
			} catch (Exception ex) {
				return R.data((Object) res);
			}
		} catch (Exception e) {
			log.error("请求日志代调失败: {}", e.getMessage(), e);
			return R.fail("调用外部接口失败：" + e.getMessage());
		}
	}

	/**
	 * 导出数据
	 */
	@PostMapping("/export-btnConfig")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "导出数据", description = "传入btnConfig")
	public void exportBtnConfig(@RequestParam Map<String, Object> btnConfig, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<BtnConfigEntity> queryWrapper = Condition.getQueryWrapper(btnConfig, BtnConfigEntity.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(BtnConfig::getTenantId, bladeUser.getTenantId());
		//}
		queryWrapper.lambda().eq(BtnConfigEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<BtnConfigExcel> list = btnConfigService.exportBtnConfig(queryWrapper);
		ExcelUtil.export(response, "AI按钮接口配置表数据" + DateUtil.time(), "AI按钮接口配置表数据表", list, BtnConfigExcel.class);
	}

}
