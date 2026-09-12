package org.springblade.modules.desk.vo;

import lombok.Data;
import org.springblade.modules.desk.entity.BizNoticeEntity;
import org.springblade.modules.resource.vo.AttachVO;

import java.util.List;

@Data
public class NoticeIdsVO {

	private BizNoticeEntity notice;
	private List<Long> ids;
	List<AttachVO> attachSourceList;

}
