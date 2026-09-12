package org.springblade.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户组表 实体类
 *
 * @author BladeX
 * @since 2024-11-14
 */
@Data
@TableName("blade_people_group")
@Schema(name = "Group对象", description = "用户组表")
@EqualsAndHashCode
public class PeopleGroup implements Serializable {

	@TableId(value = "id", type = IdType.ASSIGN_ID)
	private Long id;

	@TableField(exist = false)
	private List<Long> excludeIds;

	/**
	 * 组名
	 */
	@Schema(description = "组名")
	private String groupName;
	/**
	 * 组类型
	 */
	@Schema(description = "组类型")
	private String groupType;
	/**
	 * 成组说明
	 */
	@Schema(description = "成组说明")
	private String remark;

	@Schema(description = "创建人")
	private Long createUser;

	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Schema(description = "创建时间")
	private Date createTime;


	@Schema(description = "更新人")
	private Long updateUser;

	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Schema(description = "更新时间")
	private Date updateTime;

	@TableLogic
	@Schema(description = "是否已删除")
	private Integer isDeleted;

	@TableField(exist = false)
	private List<Long> peopleGroupChildren;





}
