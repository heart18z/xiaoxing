package org.springblade.modules.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.modules.system.entity.PeopleGroup;

import java.util.List;

/**
 * 用户组表 视图实体类
 *
 * @author BladeX
 * @since 2024-11-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PeopleGroupVO extends PeopleGroup {
	private static final long serialVersionUID = 1L;

	private String updateUserName;
	private Integer userCount;
	private String groupNameLabel;
	private String userNames;



	/**
	 * 组名
	 */
	@Schema(description = "组名")
	private List<String> groupNameList;

	private List<Long> excludeIds;

	private List<Long> ids;
	private List<Long> peopleGroupIds;

	/**
	 * 组类型
	 */
	@Schema(description = "组类型")
	private String groupType;

	private String groupName;

	private String realName;
	private String phone;

}
