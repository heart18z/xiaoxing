package org.springblade.modules.system.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.system.entity.PeopleGroupChild;
import org.springblade.modules.system.vo.PeopleGroupChildVO;

import java.util.Objects;

/**
 * 用户组子表 包装类,返回视图层所需的字段
 *
 * @author BladeX
 * @since 2024-11-14
 */
public class PeopleGroupChildWrapper extends BaseEntityWrapper<PeopleGroupChild, PeopleGroupChildVO>  {

	public static PeopleGroupChildWrapper build() {
		return new PeopleGroupChildWrapper();
 	}

	@Override
	public PeopleGroupChildVO entityVO(PeopleGroupChild peopleGroupChild) {
		PeopleGroupChildVO peopleGroupChildVO = Objects.requireNonNull(BeanUtil.copy(peopleGroupChild, PeopleGroupChildVO.class));

		//User createUser = UserCache.getUser(groupChild.getCreateUser());
		//User updateUser = UserCache.getUser(groupChild.getUpdateUser());
		//groupChildVO.setCreateUserName(createUser.getName());
		//groupChildVO.setUpdateUserName(updateUser.getName());

		return peopleGroupChildVO;
	}


}
