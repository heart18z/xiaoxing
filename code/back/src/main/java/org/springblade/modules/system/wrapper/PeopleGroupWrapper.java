package org.springblade.modules.system.wrapper;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springblade.common.cache.UserCache;
import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.SpringUtil;
import org.springblade.modules.system.entity.PeopleEntity;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.entity.PeopleGroup;
import org.springblade.modules.system.entity.PeopleGroupChild;
import org.springblade.modules.system.mapper.PeopleMapper;
import org.springblade.modules.system.mapper.PeopleGroupChildMapper;
import org.springblade.modules.system.vo.PeopleGroupVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 用户组表 包装类,返回视图层所需的字段
 *
 * @author BladeX
 * @since 2024-11-14
 */
public class PeopleGroupWrapper extends BaseEntityWrapper<PeopleGroup, PeopleGroupVO> {

	public static PeopleGroupWrapper build() {
		return new PeopleGroupWrapper();
	}

	@Override
	public PeopleGroupVO entityVO(PeopleGroup peopleGroup) {
		PeopleGroupVO peopleGroupVO = Objects.requireNonNull(BeanUtil.copy(peopleGroup, PeopleGroupVO.class));
		PeopleGroupChildMapper peopleGroupChildMapper = SpringUtil.getBean(PeopleGroupChildMapper.class);
		PeopleMapper peopleMapper = SpringUtil.getBean(PeopleMapper.class);
		List<PeopleGroupChild> peopleGroupChildList = peopleGroupChildMapper.selectList(new LambdaQueryWrapper<PeopleGroupChild>()
			.eq(PeopleGroupChild::getPeopleGroupId, peopleGroup.getId()));


		//User createUser = UserCache.getUser(group.getCreateUser());
		User updateUser = UserCache.getUser(peopleGroup.getUpdateUser());
		//groupVO.setCreateUserName(createUser.getName());
		peopleGroupVO.setUpdateUserName(updateUser.getRealName());
		peopleGroupVO.setGroupNameLabel(peopleGroupVO.getGroupName() + "(" + peopleGroupChildList.size() + "人)");
		List<String> userNameList = new ArrayList<>();
		List<Long> userIds = new ArrayList<>();
		Integer count = 0;
		for (PeopleGroupChild peopleGroupChild : peopleGroupChildList) {
			PeopleEntity people = peopleMapper.selectById(peopleGroupChild.getPeopleId());
			if (Objects.nonNull(people)) {
				userNameList.add(people.getRealName());
				count = count + 1;
				userIds.add(people.getId());
			}
		}
		peopleGroupVO.setUserCount(count);
		peopleGroupVO.setPeopleGroupChildren(CollectionUtil.distinct(userIds));
		peopleGroupVO.setUserNames(CollectionUtil.join(userNameList, ","));
		return peopleGroupVO;
	}


}
