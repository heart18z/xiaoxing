package org.springblade.modules.smartreminder.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.modules.smartreminder.service.SmartReminderService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartReminderScheduler {

	private final SmartReminderService reminderService;
	private final ScheduledExecutorService workers=Executors.newScheduledThreadPool(2,r->{Thread t=new Thread(r,"reminder-evaluation");t.setDaemon(true);return t;});
	@Value("${smart-reminder.scheduler-delay:30000}")
	private long scheduledDelay=30000;

	@EventListener(ApplicationReadyEvent.class)
	public void start() {
		// Separate workers: a slow scheduled model call must not block feedback evaluation.
		workers.scheduleWithFixedDelay(this::evaluate,5,Math.max(1000,scheduledDelay)/1000,TimeUnit.SECONDS);
		workers.scheduleWithFixedDelay(()->{try{reminderService.evaluateRequestedBranches();}catch(Exception e){log.warn("发起人反馈评估调度失败",e);}},5,1,TimeUnit.SECONDS);
	}
	@PreDestroy
	public void close(){workers.shutdownNow();}

	public void evaluate() {
		try {
			reminderService.evaluateDueBranches();
		} catch (Exception e) {
			log.warn("智能提醒调度轮询失败", e);
		}
	}
}
