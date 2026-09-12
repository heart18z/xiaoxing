/*
 *      Copyright (c) 2018-2028, Chill Zhuang All rights reserved.
 */
package org.springblade.modules.develop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.develop.dto.GeneratorDTO;
import org.springblade.modules.develop.entity.Code;
import org.springblade.modules.develop.service.ICodeService;
import org.springblade.modules.develop.service.IGenerateService;
import org.springframework.web.bind.annotation.*;

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
@PreAuth(RoleConstant.HAS_ROLE_ADMINISTRATOR)
@RequestMapping(AppConstant.APPLICATION_DEVELOP_NAME + "/code")
@Tag(name = "代码生成", description = "代码生成")
public class CodeController extends BladeController {

	private final ICodeService codeService;
	private final IGenerateService generateService;

	/**
	 * 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入code")
	public R<Code> detail(Code code) {
		Code detail = codeService.getOne(Condition.getQueryWrapper(code));
		return R.data(detail);
	}

	/**
	 * 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入code")
	public R<IPage<Code>> list(@RequestParam Map<String, Object> code, Query query) {
		IPage<Code> pages = codeService.page(Condition.getPage(query), Condition.getQueryWrapper(code, Code.class));
		return R.data(pages);
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "新增或修改", description = "传入code")
	public R submit(@Valid @RequestBody Code code) {
		return R.status(codeService.submit(code));
	}

	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(codeService.removeByIds(Func.toLongList(ids)));
	}

	/**
	 * 复制
	 */
	@PostMapping("/copy")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "复制", description = "传入id")
	public R copy(@Parameter(description = "主键", required = true) @RequestParam Long id) {
		Code code = codeService.getById(id);
		code.setId(null);
		code.setCodeName(code.getCodeName() + "-copy");
		return R.status(codeService.save(code));
	}

	/**
	 * 代码生成
	 */
	@PostMapping("/gen-code")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "代码生成", description = "传入ids")
	public R genCode(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		boolean result = generateService.code(Func.toLongList(ids));
		return result ? R.success("代码生成成功") : R.fail("代码生成失败");
	}

	/**
	 * 代码快速生成
	 */
	@PostMapping("/gen-code-fast")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "代码快速生成", description = "传入配置集合")
	public R genCodeFast(@RequestBody GeneratorDTO dto) {
		boolean result = generateService.codeFast(dto);
		return result ? R.success("代码生成成功") : R.fail("代码生成失败");
	}

}
