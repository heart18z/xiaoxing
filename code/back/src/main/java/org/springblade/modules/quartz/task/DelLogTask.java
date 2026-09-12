package org.springblade.modules.quartz.task;

import lombok.AllArgsConstructor;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.constant.ParamCacheConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.service.ILogApiService;
import org.springblade.modules.system.service.ILogErrorService;
import org.springblade.modules.system.service.ILogUsualService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import jakarta.annotation.PostConstruct;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

@Component
@AllArgsConstructor
public class DelLogTask {
	private final ILogApiService logApiService;
	private final ILogErrorService logErrorService;
	private final ILogUsualService logUsualService;

	@Scheduled(cron = "0 0 0 1/1 * ? ")// 每天0点执行一次
	//
	//@Scheduled(cron = "0 0/5 * * * ? ")// 五分钟一次
	//@Transactional(rollbackFor = Exception.class)
	//@PostConstruct
	public void delLogByDays() throws SQLException {
		String days = ParamCache.getValue(ParamCacheConstant.LOG_RETAIN_TIME);
		if (Func.isBlank(days) || "-1".equals(days)) {
			return;
		}
		// 获取当前日期
	    Date currentDate = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(currentDate);
		// 获取七天前的日期
		calendar.add(Calendar.DATE, Integer.parseInt(days) * -1);
		Date sevenDaysAgo = calendar.getTime();

		logApiService.delLogByDate(sevenDaysAgo);
		logErrorService.delLogByDate(sevenDaysAgo);
		logUsualService.delLogByDate(sevenDaysAgo);

	}
}
