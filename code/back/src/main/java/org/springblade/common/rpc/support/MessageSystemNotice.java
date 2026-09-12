package org.springblade.common.rpc.support;

import lombok.Data;

import java.util.List;


@Data
public class MessageSystemNotice {

	private String noticeTitle;
	private String category;
	private String noticeType;
	private List<String> noticeSentTimeRange;
	private String href;
	private String noticeContent;

}
