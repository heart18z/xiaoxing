package org.springblade.modules.standard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

import java.util.Date;

/**
 * 规范类列表 实体类
 *
 * @author linchaofan
 * @since 2023-06-09
 */
@Data
@TableName("blade_standard")
@Schema(name = "Standard对象", description = "规范类列表")
@EqualsAndHashCode(callSuper = true)
public class StandardEntity extends TenantEntity {

	/**
	 * 父级编号
	 */
	@Schema(description = "父级编号")
	private Long parentId;
	/**
	 * 题目
	 */
	@Schema(description = "题目")
	private String title;
	/**
	 * 上传人
	 */
	@Schema(description = "上传人")
	private String uploadUser;
	/**
	 * 上传时间
	 */
	@Schema(description = "上传时间")
	private Date uploadTime;
	/**
	 * 树型等级，根从0开始
	 */
	@Schema(description = "树型等级，根从0开始")
	private Integer level;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 树序号
	 */
	@Schema(description = "树序号")
	private String serialNumber;

	/**
	 * 类别
	 */
	private String standardType;


}
