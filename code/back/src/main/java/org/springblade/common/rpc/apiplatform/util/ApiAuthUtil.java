package org.springblade.common.rpc.apiplatform.util;

import com.alibaba.fastjson.JSON;
import org.springblade.common.rpc.apiplatform.config.ApiPlatformConfig;
import org.springblade.common.rpc.apiplatform.constant.ApiPlatformConstant;
import org.springblade.common.rpc.apiplatform.constant.ApiPlatformUrlConstant;
import org.springblade.common.rpc.apiplatform.domain.ApiSystemTokenResult;
import org.springblade.common.rpc.apiplatform.domain.TokenDataParam;
import org.springblade.core.http.HttpRequest;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.SpringUtil;
import org.springframework.data.redis.core.RedisTemplate;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ApiAuthUtil {
	private static final ApiPlatformConfig apiPlatformConfig;

	private static final  RedisTemplate<String, Object> redisTemplate;

	static {
		BladeRedis bladeRedis = SpringUtil.getBean(BladeRedis.class);
		apiPlatformConfig = SpringUtil.getBean(ApiPlatformConfig.class);
		redisTemplate = bladeRedis.getRedisTemplate();
	}

	public static String getTokenString(){

		Long expireTime=  redisTemplate.opsForValue().getOperations().getExpire(ApiPlatformConstant.API_TOKEN_REDIS_KEY_NAME);
		if (expireTime == null || expireTime < ApiPlatformConstant.mini_refresh_access_token_second) {
			return initAccessToken();
		}

		String accessToken = String.valueOf(redisTemplate.opsForValue().get(ApiPlatformConstant.API_TOKEN_REDIS_KEY_NAME));
		if (Func.isBlank(accessToken)) {
			accessToken = initAccessToken();
		}
		return accessToken;
	}

	public static void removeAccessToken() {
		redisTemplate.delete(ApiPlatformConstant.API_TOKEN_REDIS_KEY_NAME);
	}



	private static String initAccessToken() {
		String accessToken;
		try {
			ApiSystemTokenResult apiSystemResult = requestToGetAccessToken();
			accessToken = apiSystemResult.getData().getAccessToken();
			int expireTime = apiSystemResult.getData().getExpireTime();
			if (expireTime<ApiPlatformConstant.mini_refresh_access_token_second) {
				throw new Exception();
			}
			redisTemplate.delete(ApiPlatformConstant.API_TOKEN_REDIS_KEY_NAME);
			redisTemplate.opsForValue().set(ApiPlatformConstant.API_TOKEN_REDIS_KEY_NAME,accessToken,expireTime, TimeUnit.SECONDS);

		}catch (Exception exception) {
			throw new ServiceException("远程调用身份认证失败！");
		}
		return accessToken;
	}

	private static ApiSystemTokenResult requestToGetAccessToken(){
		System.out.println("init---token");
		String appKey =apiPlatformConfig.getAppKey();
		Long timestamp = System.currentTimeMillis();
		String randomStr = UUID.randomUUID().toString();

		String token = ApiTokenUtil.generateToken(new HashMap<String,Object>(){{
			put("appKey",appKey);
			put("timestamp",timestamp);
			put("randomStr",randomStr);
		}},apiPlatformConfig.getAppSecret());

		String res = HttpRequest.post(apiPlatformConfig.getBaseUrl()+ApiPlatformUrlConstant.AUTH_TOKEN_URL)
			.formBuilder()
			.add("appKey", appKey)
			.add("token", token)
			.add("randomStr", randomStr)
			.add("timestamp", timestamp)
			.execute()
			.onSuccess(responseSpec -> responseSpec.asString());
		ApiSystemTokenResult r = JSON.parseObject(res, ApiSystemTokenResult.class);
		if (Func.isNull(r) || r.getCode()!=200 || Func.isNull(r.getData()) || Func.isBlank(r.getData().getAccessToken())) {
			throw  new ServiceException("远程调用接口异常：获取token失败！");
		}

		return r;
	}

	private static Map<String, Object> objectToMap(Object obj) {
		return Arrays.stream(obj.getClass().getDeclaredFields())
			.peek(field -> field.setAccessible(true))
			.collect(Collectors.toMap(Field::getName, field -> {
				try {
					return field.get(obj);
				} catch (IllegalAccessException e) {
					return null;
				}
			}));
	}

}
