package org.springblade.modules.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.system.entity.PeopleEntity;
import org.springblade.modules.system.entity.PeopleGroup;
import org.springblade.modules.system.entity.PeopleGroupChild;
import org.springblade.modules.system.vo.PeopleGroupVO;

import java.util.List;
import java.util.Map;

public interface IPeopleGroupService extends IService<PeopleGroup> {

	/**
	 * 获取用户所属用户组列表
	 *
	 * @param userId
	 * @return
	 */
	List<Long> getPeopleGroupIdByUserId(Long userId);

	/**
	 * 获取列表
	 *
	 * @param peopleGroupIdList
	 * @return
	 */
	List<PeopleEntity> getPeopleListByGroupId(List<Long> peopleGroupIdList);
	IPage<PeopleEntity> getPeopleListPageByGroupId(Page<PeopleGroupChild> page, PeopleGroupVO peopleGroupQo);

	/**
	 * 获取当前用户可选的人员列表
	 * @return
	 */
	List<PeopleEntity> getPeopleListByCurrentUser();

	/**
	 * 获取用户组用户id
	 *
	 * @param peopleGroupId
	 * @return
	 */
	List<Long> getUserIdsByPeopleGroupId(Long peopleGroupId);

	/**
	 * 获取用户组用户id
	 *
	 * @param peopleGroupIds
	 * @return
	 */
	List<Long> getUserIdsByPeopleGroupIds(List<Long> peopleGroupIds);

	/**
	 * 查询详情
	 *
	 * @param id
	 * @return
	 */
	PeopleGroupVO getDetail(Long id);

	/**
	 * 查询分页列表
	 *
	 * @param page
	 * @param peopleGroupQo
	 * @return
	 */
	IPage<PeopleGroupVO> getListPage(Page<PeopleGroup> page, PeopleGroupVO peopleGroupQo);

	/**
	 * 列表
	 *
	 * @param peopleGroupQo
	 * @return
	 */
	List<PeopleGroupVO> getList(PeopleGroupVO peopleGroupQo);

	List<PeopleGroupVO> getListByIds(PeopleGroupVO peopleGroupQo);

	/**
	 * 获取组名列表
	 *
	 * @return
	 */
	List<Map<String, String>> getPeopleGroupNames(PeopleGroup peopleGroup);

	/**
	 * 是否存在
	 *
	 * @param peopleGroup
	 * @return
	 */
	Boolean existPeopleGroupName(PeopleGroup peopleGroup);

	/**
	 * 提交
	 *
	 * @param peopleGroup
	 */
	void submit(PeopleGroup peopleGroup);

	/**
	 * 删除
	 *
	 * @param ids
	 */
	void removeByIds(List<Long> ids);
}
