package org.springblade.modules.auth.handler;

import lombok.RequiredArgsConstructor;
import org.springblade.common.cache.CacheNames;
import org.springblade.common.cache.ParamCache;
import org.springblade.core.oauth2.exception.ExceptionCode;
import org.springblade.core.oauth2.provider.OAuth2Validation;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.tool.utils.Func;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的登录失败锁定处理器
 */
@Component
@RequiredArgsConstructor
public class BladeRedisLockHandler {

	public static final Integer DEFAULT_FAIL_COUNT = 5;
	public static final String FAIL_COUNT_VALUE = "account.failCount";
	public static final Integer DEFAULT_LOCK_TIME_MINUTES = 10;
	public static final String FAIL_LOCK_TIME_MINUTES_KEY = "lock.time";

	private final BladeRedis bladeRedis;

	public OAuth2Validation validateAccountLock(String tenantId, String account) {
		int cnt = getFailCount(tenantId, account);
		int failCount = Func.toInt(ParamCache.getValue(FAIL_COUNT_VALUE), DEFAULT_FAIL_COUNT);
		if (cnt >= failCount) {
			int minutes = Func.toInt(ParamCache.getValue(FAIL_LOCK_TIME_MINUTES_KEY), DEFAULT_LOCK_TIME_MINUTES);
			return failure(Func.format("账号或者密码错误，账号已锁定，从锁定之时起需要{}分钟后方能解锁", minutes));
		}
		return new OAuth2Validation();
	}

	public void handleAuthFailure(String tenantId, String account) {
		if (Func.isBlank(tenantId) || Func.isBlank(account)) {
			return;
		}
		int cnt = getFailCount(tenantId, account) + 1;
		int minutes = Func.toInt(ParamCache.getValue(FAIL_LOCK_TIME_MINUTES_KEY), DEFAULT_LOCK_TIME_MINUTES);
		bladeRedis.setEx(CacheNames.tenantKey(tenantId, CacheNames.USER_FAIL_KEY, account), cnt, Duration.ofMinutes(minutes));
	}

	public void handleAuthSuccess(String tenantId, String account) {
		if (Func.isBlank(tenantId) || Func.isBlank(account)) {
			return;
		}
		bladeRedis.del(CacheNames.tenantKey(tenantId, CacheNames.USER_FAIL_KEY, account));
	}

	private int getFailCount(String tenantId, String account) {
		return Func.toInt(bladeRedis.get(CacheNames.tenantKey(tenantId, CacheNames.USER_FAIL_KEY, account)), 0);
	}

	private OAuth2Validation failure(String message) {
		OAuth2Validation validation = new OAuth2Validation();
		validation.setSuccess(false);
		validation.setCode(ExceptionCode.USER_TOO_MANY_FAILS.getCode());
		validation.setMessage(message);
		return validation;
	}
}
