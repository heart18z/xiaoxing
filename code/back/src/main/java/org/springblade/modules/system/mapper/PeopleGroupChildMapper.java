package org.springblade.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.system.entity.PeopleEntity;
import org.springblade.modules.system.entity.PeopleGroupChild;
import org.springblade.modules.system.vo.PeopleGroupVO;

/**
 * 用户组子表 Mapper 接口
 *
 * @author BladeX
 * @since 2024-11-14
 */
public interface PeopleGroupChildMapper extends BaseMapper<PeopleGroupChild> {

	IPage<PeopleEntity> selectPageByGroupIds(Page<PeopleGroupChild> page, @Param("peopleGroupQo") PeopleGroupVO peopleGroupQo);

}
