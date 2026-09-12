package org.springblade.common.rpc.apiplatform.constant;

public interface ApiPlatformConstant {

	String API_TOKEN_REDIS_KEY_NAME = "api-platform-token";
	//最小刷新token时间10分钟
	long mini_refresh_access_token_second= 600;

	String API_ACCESS_TOKEN_NAME = "accessToken";

	int API_ACCESS_TOKEN_ERROR_CODE = 401;

}
