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
package org.springblade.modules.develop.service.impl;

import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringPool;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.develop.constant.DevelopConstant;
import org.springblade.develop.support.BladeCodeGenerator;
import org.springblade.modules.develop.dto.GeneratorDTO;
import org.springblade.modules.develop.entity.*;
import org.springblade.modules.develop.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 代码生成 服务实现类
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor
public class GenerateServiceImpl implements IGenerateService {

	private static final String DEFAULT_MENU_PARENT_ID = "1123598815738675203";

	private final ICodeService codeService;
	private final ICodeSettingService codeSettingService;
	private final IDatasourceService datasourceService;
	private final IModelService modelService;
	private final IModelPrototypeService modelPrototypeService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean code(List<Long> ids) {
		Collection<Code> codes = codeService.listByIds(ids);
		codes.forEach(code -> {
			BladeCodeGenerator generator = new BladeCodeGenerator();
			this.generateMenu(generator, code);
			this.generateTemplate(generator, code);
			Model model = modelService.getById(code.getModelId());
			this.generateModel(generator, code, model);
			this.generateDatasource(generator, model);
			generator.run();
			this.fixGeneratedFiles(code, model);
		});
		return true;
	}

	@Override
	public boolean codeFast(GeneratorDTO dto) {
		BladeCodeGenerator generator = new BladeCodeGenerator();
		Code code = Objects.requireNonNull(BeanUtil.copyProperties(dto, Code.class));
		Model model = Objects.requireNonNull(BeanUtil.copyProperties(dto, Model.class));
		String modelForm = dto.getModelForm();
		this.generateMenu(generator, code);
		this.generateForm(generator, modelForm);
		this.generateTemplate(generator, code);
		this.generateModel(generator, code, model);
		this.generateDatasource(generator, model);
		generator.run();
		this.fixGeneratedFiles(code, model);
		return true;
	}

	private void generateMenu(BladeCodeGenerator generator, Code code) {
		String menuId = code.getMenuId() != null ? String.valueOf(code.getMenuId()) : DEFAULT_MENU_PARENT_ID;
		generator.setMenuId(menuId);
		generator.setHasMenuSql(Boolean.TRUE);
	}

	private void generateForm(BladeCodeGenerator generator, String modelForm) {
		if (StringUtil.isNotBlank(modelForm)) {
			CodeSetting codeSetting = codeSettingService.getById(Func.toLong(modelForm));
			if (codeSetting != null) {
				generator.setModelFormOption(codeSetting.getSettings());
			}
		}
	}

	private void generateTemplate(BladeCodeGenerator generator, Code code) {
		generator.setCodeStyle(code.getCodeStyle());
		generator.setCodeName(code.getCodeName());
		generator.setServiceName(code.getServiceName());
		generator.setPackageName(code.getPackageName());
		generator.setPackageDir(code.getApiPath());
		generator.setPackageWebDir(code.getWebPath());
		generator.setTablePrefix(Func.toStrArray(code.getTablePrefix()));
		generator.setIncludeTables(Func.toStrArray(code.getTableName()));
		generator.setTemplateType(Func.toStr(code.getTemplateType(), DevelopConstant.TEMPLATE_CRUD));
		generator.setAuthor(code.getAuthor());
		generator.setSubModelId(code.getSubModelId());
		generator.setSubFkId(code.getSubFkId());
		generator.setTreeId(code.getTreeId());
		generator.setTreePid(code.getTreePid());
		generator.setTreeName(code.getTreeName());
		generator.setHasSuperEntity(code.getBaseMode() != null && code.getBaseMode() == 2);
		generator.setHasWrapper(code.getWrapMode() != null && code.getWrapMode() == 2);
		generator.setHasFeign(code.getFeignMode() != null && code.getFeignMode() == 2);
		generator.setHasServiceName(Boolean.TRUE);
	}

	private void generateModel(BladeCodeGenerator generator, Code code, Model model) {
		generator.setModelCode(model.getModelCode());
		generator.setModelClass(model.getModelClass());
		generator.setModel(JsonUtil.readMap(JsonUtil.toJson(model)));

		if (Func.isNotEmpty(model.getId())) {
			List<ModelPrototype> prototypes = modelPrototypeService.prototypeList(model.getId());
			generator.setPrototypes(JsonUtil.readListMap(JsonUtil.toJson(prototypes)));
			if (StringUtil.isNotBlank(code.getSubModelId()) && StringUtil.equals(code.getTemplateType(), DevelopConstant.TEMPLATE_SUB)) {
				Model subModel = modelService.getById(Func.toLong(code.getSubModelId()));
				List<ModelPrototype> subPrototypes = modelPrototypeService.prototypeList(subModel.getId());
				generator.setSubModel(JsonUtil.readMap(JsonUtil.toJson(subModel)));
				generator.setSubPrototypes(JsonUtil.readListMap(JsonUtil.toJson(subPrototypes)));
			}
		} else {
			TableInfo tableInfo = modelPrototypeService.getTableInfo(model.getModelTable(), model.getDatasourceId());
			List<ModelPrototype> prototypes = convertPrototypes(tableInfo.getFields());
			generator.setPrototypes(JsonUtil.readListMap(JsonUtil.toJson(prototypes)));
		}
	}

	private void generateDatasource(BladeCodeGenerator generator, Model model) {
		Datasource datasource = datasourceService.getById(model.getDatasourceId());
		generator.setDriverName(datasource.getDriverClass());
		generator.setUrl(datasource.getUrl());
		generator.setUsername(datasource.getUsername());
		generator.setPassword(datasource.getPassword());
	}

	private void fixGeneratedFiles(Code code, Model model) {
		fixMenuSql(code, model);
		fixPojoGeneratedFiles(code, model);
	}

	private void fixMenuSql(Code code, Model model) {
		Path menuSql = resolveMenuSql(code, model);
		if (menuSql == null || !Files.exists(menuSql)) {
			return;
		}
		try {
			String content = Files.readString(menuSql, StandardCharsets.UTF_8);
			String modelCode = StringUtil.isNotBlank(model.getModelCode()) ? model.getModelCode() : model.getModelClass();
			String menuPath = StringPool.SLASH + modelCode + StringPool.SLASH + modelCode;
			String parentId = code.getMenuId() != null ? String.valueOf(code.getMenuId()) : DEFAULT_MENU_PARENT_ID;
			String fixed = content
				.replaceFirst("VALUES \\('([^']+)',\\s*'?null'?,", "VALUES ('$1', '" + parentId + "',")
				.replace(StringPool.SLASH + modelCode + StringPool.SLASH + modelCode + "/index", menuPath);
			if (!content.equals(fixed)) {
				Files.writeString(menuSql, fixed, StandardCharsets.UTF_8);
			}
		} catch (IOException ignored) {
			// 菜单 SQL 修正失败不影响主体代码生成。
		}
	}

	private Path resolveMenuSql(Code code, Model model) {
		String modelCode = StringUtil.isNotBlank(model.getModelCode()) ? model.getModelCode() : model.getModelClass();
		if (StringUtil.isBlank(code.getApiPath()) || StringUtil.isBlank(modelCode)) {
			return null;
		}
		String fileName = modelCode.toLowerCase() + ".menu.sql";
		Path apiPath = Paths.get(code.getApiPath());
		Path defaultPath = apiPath.resolve(Paths.get("src", "main", "java", "sql", fileName));
		if (Files.exists(defaultPath)) {
			return defaultPath;
		}
		return apiPath.resolve(Paths.get("sql", fileName));
	}

	private void fixPojoGeneratedFiles(Code code, Model model) {
		Path packagePath = resolvePackagePath(code);
		if (packagePath == null || !Files.exists(packagePath)) {
			return;
		}
		moveWrapperToPojo(packagePath, model.getModelClass());
		try (Stream<Path> paths = Files.walk(packagePath)) {
			paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
				.forEach(path -> fixPojoImports(path, code.getPackageName()));
		} catch (IOException ignored) {
			// 后处理失败不阻断代码生成。
		}
	}

	private Path resolvePackagePath(Code code) {
		if (StringUtil.isBlank(code.getApiPath()) || StringUtil.isBlank(code.getPackageName())) {
			return null;
		}
		return Paths.get(code.getApiPath())
			.resolve(Paths.get("src", "main", "java"))
			.resolve(code.getPackageName().replace(".", StringPool.SLASH));
	}

	private void moveWrapperToPojo(Path packagePath, String modelClass) {
		Path source = packagePath.resolve(Paths.get("wrapper", modelClass + "Wrapper.java"));
		if (!Files.exists(source)) {
			return;
		}
		Path target = packagePath.resolve(Paths.get("pojo", "wrapper", modelClass + "Wrapper.java"));
		try {
			Files.createDirectories(target.getParent());
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ignored) {
			// 移动失败时不删除原文件。
		}
	}

	private void fixPojoImports(Path path, String packageName) {
		try {
			String content = Files.readString(path, StandardCharsets.UTF_8);
			String fixed = content
				.replace(packageName + ".entity.", packageName + ".pojo.entity.")
				.replace(packageName + ".vo.", packageName + ".pojo.vo.")
				.replace(packageName + ".wrapper.", packageName + ".pojo.wrapper.");
			if (!content.equals(fixed)) {
				Files.writeString(path, fixed, StandardCharsets.UTF_8);
			}
		} catch (IOException ignored) {
			// 单文件修正失败不影响其他文件修正。
		}
	}

	private static List<ModelPrototype> convertPrototypes(List<TableField> tableFields) {
		return tableFields.stream().map(tableField -> {
			ModelPrototype prototype = new ModelPrototype();
			prototype.setJdbcName(tableField.getName());
			if (tableField.getColumnType() != null) {
				prototype.setJdbcType(tableField.getColumnType().getType());
				prototype.setPropertyType(tableField.getColumnType().getType());
			}
			prototype.setJdbcComment(tableField.getComment());
			prototype.setPropertyName(tableField.getPropertyName());
			prototype.setComponentType("input");
			return prototype;
		}).collect(Collectors.toList());
	}

}
