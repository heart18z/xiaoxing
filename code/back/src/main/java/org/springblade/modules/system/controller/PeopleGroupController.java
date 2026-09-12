package org.springblade.modules.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.PeopleGroup;

import org.springblade.modules.system.service.IPeopleGroupService;
import org.springblade.modules.system.vo.PeopleGroupVO;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 用户组表 控制器
 *
 * @author BladeX
 * @since 2024-11-14
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-system/peopleGroup")
@Tag(name = "用户组表接口", description = "用户组表")
@SuppressWarnings("all")
public class PeopleGroupController extends BladeController {

	private final IPeopleGroupService peopleGroupService;




	@PostMapping("select")
	public R select() {
		return R.data(peopleGroupService.list());
	}



	/**
	 * 获取当前用户可选的人员列表
	 *
	 * @return
	 */
	@PostMapping("peopleListByCurrentUser")
	public R getPeopleListByCurrentUser() {
		return R.data(peopleGroupService.getPeopleListByCurrentUser());
	}

	/**
	 * 获取用户组名称列表
	 *
	 * @return
	 */
	@PostMapping("/peopleGroupNames")
	public R getPeopleGroupNames(PeopleGroup peopleGroup) {
		return R.data(peopleGroupService.getPeopleGroupNames(peopleGroup));
	}

	/**
	 * 是否存在用户组名
	 *
	 * @param peopleGroup
	 * @return
	 */
	@PostMapping("/exist-people-group-name")
	public R existPeopleGroupName(@RequestBody PeopleGroup peopleGroup) {
		return R.data(peopleGroupService.existPeopleGroupName(peopleGroup));
	}

	/**
	 * 用户组表 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入group")
	public R<PeopleGroupVO> detail(PeopleGroup peopleGroup) {
		return R.data(peopleGroupService.getDetail(peopleGroup.getId()));
	}

	/**
	 * 用户组表 分页
	 */
	@PostMapping("/list-page")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入group")
	public R<IPage<PeopleGroupVO>> getListPage(PeopleGroupVO peopleGroupQo, Query query) {
		return R.data(peopleGroupService.getListPage(new Page<>(query.getCurrent(), query.getSize()), peopleGroupQo));
	}


	/**
	 * 列表
	 *
	 * @param peopleGroupQo
	 * @return
	 */
	@PostMapping("/list")
	public R<List<PeopleGroupVO>> getList(PeopleGroupVO peopleGroupQo) {
		return R.data(peopleGroupService.getList(peopleGroupQo));
	}



	/**
	 * 用户组表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入group")
	public R submit(@Valid @RequestBody PeopleGroup peopleGroup) {
		peopleGroupService.submit(peopleGroup);
		return R.status(true);
	}

	/**
	 * 用户组表 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		peopleGroupService.removeByIds(Func.toLongList(ids));
		return R.status(true);
	}

}
