package org.springblade.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.system.entity.PeopleGroup;

import java.util.List;

/**
 * 用户组表 Mapper 接口
 *
 * @author BladeX
 * @since 2024-11-14
 */
public interface PeopleGroupMapper extends BaseMapper<PeopleGroup> {

	List<Long> selectGroupIdByUserId(@Param("userId") Long userId);

}
