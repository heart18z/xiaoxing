package org.springblade.modules.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.modules.resource.entity.Oss;

/**
 * OssVO
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "OssVO对象", description = "对象存储表")
public class OssVO extends Oss {
	private static final long serialVersionUID = 1L;

	/**
	 * 分类名
	 */
	private String categoryName;

	/**
	 * 是否启用
	 */
	private String statusName;

}
