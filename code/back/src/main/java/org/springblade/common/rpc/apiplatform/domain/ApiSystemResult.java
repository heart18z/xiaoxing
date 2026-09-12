package org.springblade.common.rpc.apiplatform.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ApiSystemResult {

	private int code;

	private boolean success;

	private Object data;

	private String msg;
}
