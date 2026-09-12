package org.springblade.common.rpc.apiplatform.domain;

import cn.hutool.core.date.DateTime;
import lombok.Data;

@Data
public class TokenDataParam {
	private String appKey;

	private Long timestamp;

	private String randomStr;

	private String token;
}
