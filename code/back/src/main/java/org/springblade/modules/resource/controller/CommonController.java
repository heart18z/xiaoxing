package org.springblade.modules.resource.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.modules.system.entity.Menu;
import org.springblade.modules.system.service.IMenuService;
import org.springblade.modules.system.vo.CheckedTreeVO;
import org.springblade.modules.system.vo.GrantTreeVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("components/common")
@Tag(name = "附件来源表接口", description = "附件来源表")
public class CommonController {
	private final IMenuService menuService;
	/**
	 * 获取菜单详情
	 */
	@PostMapping("/getMenuByPath")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入attachSource")
	public R<Menu> detail(String pagePath) {
			Menu menu = menuService.getOne(new LambdaQueryWrapper<Menu>()
				.eq(Menu::getPath,pagePath)
				.eq(Menu::getIsDeleted, BladeConstant.DB_NOT_DELETED)
				.last("limit 1")
			);
//			if (menu == null) {
////				throw new ServiceException("当前功能不允许上传文件，请联系管理员！");
////			}
			return R.data(menu==null?new Menu():menu);

	}

	/**
	 * 获取菜单下拉
	 */
	@PostMapping("/menu-tree-select")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入attachSource")
	public R<GrantTreeVO> menuTreeList() {
		GrantTreeVO vo = new GrantTreeVO();
		vo.setMenu(menuService.menuTreeList());
		return R.data(vo);

	}



}
