package org.springblade.modules.desk.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BizNoticeSendTypeEnum {
	SEND_IMMEDIATELY("immediately",1),
	SEND_DELAY("delay",2);
	final String name;

	final int category;
}
