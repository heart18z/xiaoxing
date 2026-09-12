package org.springblade.modules.desk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.modules.desk.entity.BizNoticeMessageEntity;

@Data
public class BizNoticeMessageParamVO extends BizNoticeMessageEntity {
	/**
	 * 是否是接收人
	 */
	private Boolean isRecipient;
	/**
	 * 类型
	 */
	private String category;
	/**
	 * 标题
	 */
	private String noticeTitle;
	@Schema(description = "开始时间")
	private String firstTime;
	@Schema(description = "结束时间")
	private String endTime;


}
