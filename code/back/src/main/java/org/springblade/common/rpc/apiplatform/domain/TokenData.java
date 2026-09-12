package org.springblade.common.rpc.apiplatform.domain;

import lombok.Data;

@Data
public class TokenData {
	private int expireTime;

	private String accessToken;
}
