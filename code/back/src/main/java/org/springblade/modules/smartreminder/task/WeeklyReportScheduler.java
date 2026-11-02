package org.springblade.modules.smartreminder.task;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.modules.smartreminder.service.EventReportService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.concurrent.*;
@Component
@Slf4j
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="smart-reminder.scheduler-enabled",havingValue="true",matchIfMissing=true)
public class WeeklyReportScheduler {
    private final EventReportService reports;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"weekly-reports");t.setDaemon(true);return t;});
    @EventListener(ApplicationReadyEvent.class) public void start(){worker.scheduleWithFixedDelay(()->{try{reports.scheduled();}catch(Exception e){log.warn("周报调度失败",e);}},60,120,TimeUnit.SECONDS);}
    @PreDestroy public void close(){worker.shutdownNow();}
}
