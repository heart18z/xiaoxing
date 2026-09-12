package org.springblade.common.rpc.apiplatform.domain;

import lombok.Data;

@Data
public class ApiSystemTokenResult {

	private int code;

	private boolean success;

	private String msg;

	private TokenData data;
}
