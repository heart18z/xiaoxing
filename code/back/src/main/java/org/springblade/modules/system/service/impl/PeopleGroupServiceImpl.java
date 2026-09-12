package org.springblade.modules.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.PeopleEntity;
import org.springblade.modules.system.entity.PeopleGroup;
import org.springblade.modules.system.entity.PeopleGroupChild;
import org.springblade.modules.system.mapper.PeopleMapper;
import org.springblade.modules.system.mapper.PeopleGroupChildMapper;
import org.springblade.modules.system.mapper.PeopleGroupMapper;
import org.springblade.modules.system.service.IPeopleGroupService;
import org.springblade.modules.system.vo.PeopleGroupVO;
import org.springblade.modules.system.wrapper.PeopleGroupWrapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
@Slf4j
@AllArgsConstructor
public class PeopleGroupServiceImpl extends ServiceImpl<PeopleGroupMapper, PeopleGroup> implements IPeopleGroupService {

	private final PeopleGroupChildMapper peopleGroupChildMapper;

	private final PeopleMapper peopleMapper;

	@Override
	public List<Long> getPeopleGroupIdByUserId(Long userId) {
		List<PeopleGroupChild> peopleGroupChildList = peopleGroupChildMapper.selectList(new LambdaQueryWrapper<PeopleGroupChild>().eq(PeopleGroupChild::getPeopleId, userId));
		return peopleGroupChildList.stream().map(item -> item.getPeopleGroupId()).collect(Collectors.toList());
	}

	@Override
	public List<PeopleEntity> getPeopleListByGroupId(List<Long> peopleGroupIdList) {
		List<PeopleEntity> list = new ArrayList<>();
		if (CollectionUtil.isNotEmpty(peopleGroupIdList)) {
			List<PeopleGroupChild> peopleGroupChildList = peopleGroupChildMapper.selectList(new LambdaQueryWrapper<PeopleGroupChild>()
				.in(PeopleGroupChild::getPeopleGroupId, peopleGroupIdList));
			for (PeopleGroupChild peopleGroupChild : peopleGroupChildList) {
				PeopleEntity people = peopleMapper.selectById(peopleGroupChild.getPeopleId());
				if (Objects.nonNull(people) && Objects.isNull(list.stream().filter(item -> Objects.equals(item.getId(), people.getId())).findAny().orElse(null))) {
					list.add(people);
				}
			}
		}
		return list;
	}

	@Override
	public IPage<PeopleEntity> getPeopleListPageByGroupId(Page<PeopleGroupChild> page, PeopleGroupVO peopleGroupQo) {
		if (CollectionUtil.isEmpty(peopleGroupQo.getPeopleGroupIds())) {
			return new Page<>();
		}
		IPage<PeopleEntity> iPage = peopleGroupChildMapper.selectPageByGroupIds(page, peopleGroupQo);
		iPage.getRecords().forEach(item->{
			item.setPhone(StrUtil.isNotBlank(item.getPhone())?DesensitizedUtil.mobilePhone(item.getPhone()):"");
		});
		return iPage;
	}

	@Override
	public List<PeopleEntity> getPeopleListByCurrentUser() {
		if (Func.toStrList(AuthUtil.getUserRole()).contains("administrator") ||
			Func.toStrList(AuthUtil.getUserRole()).contains("admin") ||
			Func.toStrList(AuthUtil.getUserRole()).contains("add")) {
			return peopleMapper.selectList(Wrappers.lambdaQuery());
		} else {
			List<PeopleGroupChild> peopleGroupChildList = peopleGroupChildMapper.selectList(new LambdaQueryWrapper<PeopleGroupChild>()
				.eq(PeopleGroupChild::getPeopleId, AuthUtil.getUserId()));
			if (CollectionUtil.isNotEmpty(peopleGroupChildList)) {
				List<Long> peopleGroupIdList = peopleGroupChildList.stream().map(item -> item.getPeopleGroupId()).collect(Collectors.toList());
				List<PeopleGroupChild> peopleGroupChildList2 = peopleGroupChildMapper.selectList(new LambdaQueryWrapper<PeopleGroupChild>()
					.in(PeopleGroupChild::getPeopleGroupId, peopleGroupIdList));
				List<Long> peopleIdList = peopleGroupChildList2.stream().map(item -> item.getPeopleId()).collect(Collectors.toList());
				return peopleMapper.selectList(new LambdaQueryWrapper<PeopleEntity>().in(PeopleEntity::getId, CollectionUtil.distinct(peopleIdList)));
			}
		}
		return new ArrayList<>();
	}

	@Override
	public List<Long> getUserIdsByPeopleGroupId(Long peopleGroupId) {
		List<PeopleGroupChild> peopleGroupChildList = peopleGroupChildMapper.selectList(new LambdaQueryWrapper<PeopleGroupChild>()
			.eq(PeopleGroupChild::getPeopleGroupId, peopleGroupId));
		return peopleGroupChildList.stream().map(item -> item.getPeopleId()).collect(Collectors.toList());
	}

	@Override
	public List<Long> getUserIdsByPeopleGroupIds(List<Long> peopleGroupIds) {
		if (CollectionUtil.isNotEmpty(peopleGroupIds)) {
			List<PeopleGroupChild> peopleGroupChildList = peopleGroupChildMapper.selectList(new LambdaQueryWrapper<PeopleGroupChild>()
				.in(PeopleGroupChild::getPeopleGroupId, peopleGroupIds));
			List<Long> userIdList = peopleGroupChildList.stream().map(item -> item.getPeopleId()).collect(Collectors.toList());
			return CollectionUtil.distinct(userIdList);
		}
		return new ArrayList<>();
	}

	@Override
	public PeopleGroupVO getDetail(Long id) {
		PeopleGroup peopleGroup = baseMapper.selectById(id);
		return PeopleGroupWrapper.build().entityVO(peopleGroup);
	}

	@Override
	public IPage<PeopleGroupVO> getListPage(Page<PeopleGroup> page, PeopleGroupVO peopleGroupQo) {
		LambdaQueryWrapper<PeopleGroup> lqw = Wrappers.lambdaQuery();
		lqw.in(CollectionUtil.isNotEmpty(peopleGroupQo.getGroupNameList()), PeopleGroup::getGroupName, peopleGroupQo.getGroupNameList())
			.eq(StrUtil.isNotBlank(peopleGroupQo.getGroupType()), PeopleGroup::getGroupType, peopleGroupQo.getGroupType()).orderByDesc(PeopleGroup::getCreateTime);
		IPage<PeopleGroup> iPage = baseMapper.selectPage(page, lqw);
		return PeopleGroupWrapper.build().pageVO(iPage);
	}

	@Override
	public List<PeopleGroupVO> getList(PeopleGroupVO peopleGroupQo) {
		LambdaQueryWrapper<PeopleGroup> lqw = Wrappers.lambdaQuery();
		lqw.like(StrUtil.isNotBlank(peopleGroupQo.getGroupName()), PeopleGroup::getGroupName, peopleGroupQo.getGroupName())
			.orderByDesc(PeopleGroup::getUpdateTime);
		if (CollectionUtil.isNotEmpty(peopleGroupQo.getExcludeIds())) {
			lqw.notIn(PeopleGroup::getId, peopleGroupQo.getExcludeIds());
		}
		if (CollectionUtil.isNotEmpty(peopleGroupQo.getIds())) {
			lqw.in(PeopleGroup::getId, peopleGroupQo.getIds());
		}
		List<PeopleGroup> list = baseMapper.selectList(lqw);
		return PeopleGroupWrapper.build().listVO(list);
	}

	@Override
	public List<PeopleGroupVO> getListByIds(PeopleGroupVO peopleGroupQo) {
		LambdaQueryWrapper<PeopleGroup> lqw = Wrappers.lambdaQuery();
		if (CollectionUtil.isNotEmpty(peopleGroupQo.getIds())) {
			lqw.in(PeopleGroup::getId, peopleGroupQo.getIds());
			List<PeopleGroup> list = baseMapper.selectList(lqw);
			return PeopleGroupWrapper.build().listVO(list);
		} else {
			return new ArrayList<>();
		}
	}

	@Override
	public List<Map<String, String>> getPeopleGroupNames(PeopleGroup peopleGroup) {
		List<Map<String, String>> result = new ArrayList<>();
		LambdaQueryWrapper<PeopleGroup> lqw = Wrappers.lambdaQuery();
		if (CollectionUtil.isNotEmpty(peopleGroup.getExcludeIds())) {
			lqw.notIn(PeopleGroup::getId, peopleGroup.getExcludeIds());
		}
		List<PeopleGroup> list = baseMapper.selectList(lqw);
		for (PeopleGroup item : list) {
			Map<String, String> map = new HashMap<>();
			map.put("label", item.getGroupName());
			map.put("value", item.getGroupName());
			result.add(map);
		}
		return result;
	}

	@Override
	public Boolean existPeopleGroupName(PeopleGroup peopleGroup) {
		PeopleGroup temp = baseMapper.selectOne(new LambdaQueryWrapper<PeopleGroup>().eq(PeopleGroup::getGroupName, peopleGroup.getGroupName())
			.ne(Objects.nonNull(peopleGroup.getId()), PeopleGroup::getId, peopleGroup.getId()));
		return Objects.nonNull(temp);
	}

	@Override
	public void submit(PeopleGroup peopleGroup) {
		PeopleGroup temp = baseMapper.selectOne(new LambdaQueryWrapper<PeopleGroup>().eq(PeopleGroup::getGroupName, peopleGroup.getGroupName())
			.ne(Objects.nonNull(peopleGroup.getId()), PeopleGroup::getId, peopleGroup.getId()));
		if (Objects.nonNull(temp)) {
			throw new ServiceException("用户组名请勿重复");
		}
		if (Objects.isNull(peopleGroup.getId())) {
			peopleGroup.setCreateTime(new Date());
			peopleGroup.setCreateUser(AuthUtil.getUserId());
			peopleGroup.setUpdateTime(new Date());
			peopleGroup.setUpdateUser(AuthUtil.getUserId());
			baseMapper.insert(peopleGroup);
		} else {
			peopleGroup.setUpdateTime(new Date());
			peopleGroup.setUpdateUser(AuthUtil.getUserId());
			baseMapper.updateById(peopleGroup);
			peopleGroupChildMapper.delete(new LambdaQueryWrapper<PeopleGroupChild>().eq(PeopleGroupChild::getPeopleGroupId, peopleGroup.getId()));
		}
		for (Long userId : peopleGroup.getPeopleGroupChildren()) {
			PeopleGroupChild peopleGroupChild = new PeopleGroupChild();
			peopleGroupChild.setPeopleGroupId(peopleGroup.getId());
			peopleGroupChild.setPeopleId(userId);
			peopleGroupChildMapper.insert(peopleGroupChild);
		}
	}

	@Override
	public void removeByIds(List<Long> ids) {
		baseMapper.deleteBatchIds(ids);
	}
}
