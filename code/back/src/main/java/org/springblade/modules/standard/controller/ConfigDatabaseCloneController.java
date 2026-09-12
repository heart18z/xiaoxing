package org.springblade.modules.standard.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.modules.standard.service.IConfigDatabaseDataCloneService;
import org.springblade.modules.standard.vo.ConfigDatabaseDataClonePageVO;
import org.springblade.modules.standard.vo.DatabaseCloneVO;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

@RestController
@AllArgsConstructor
@RequestMapping("config-database/config-database-data-clone")
@Tag(name = "源表克隆呈现接口", description = "源表克隆呈现")
public class ConfigDatabaseCloneController extends BladeController{

	private final IConfigDatabaseDataCloneService dataCloneService;

	@SneakyThrows
	@PostMapping("/page")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "分页", description = "传入dataOptimizationDatasource")
	public R<ConfigDatabaseDataClonePageVO> page(
		@Parameter(description = "数据源Id",required = true) @RequestParam Long datasourceId,
		@RequestParam String tableName,Query query) {
		ConfigDatabaseDataClonePageVO page = dataCloneService.page(datasourceId,tableName, query);
		return R.data(page);
	}
	/**
	 * 表数据同步
	 */
	@PostMapping("/submit-sync")
	@ApiOperationSupport(order = 6)
	public R submit(@RequestBody DatabaseCloneVO databaseCloneVO) throws SQLException {

		return R.status(dataCloneService.doSync(databaseCloneVO));
	}
}
