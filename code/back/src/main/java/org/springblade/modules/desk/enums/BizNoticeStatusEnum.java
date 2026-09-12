package org.springblade.modules.desk.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BizNoticeStatusEnum {
	NOTICE_BUILD("build",1),
	NOTICE_SEND("send",2);
	final String name;
	final int category;
}
