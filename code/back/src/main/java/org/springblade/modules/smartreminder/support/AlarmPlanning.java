package org.springblade.modules.smartreminder.support;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Alarm timing is independent of event time and AI evaluation time. */
public final class AlarmPlanning {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private AlarmPlanning() {}
    public static LocalDateTime time(String value) {
        try { return LocalDateTime.parse(value.replace('T', ' '), FORMAT); }
        catch (Exception ignored) { return null; }
    }
    public static void normalize(ObjectNode event, LocalDateTime now) {
        boolean requested = event.path("alarmRequested").asBoolean(false);
        event.put("alarmRequested", requested);
        LocalDateTime anchor = time(event.path("eventTime").asText(""));
        if (anchor == null && requested) anchor = time(event.path("deadlineTime").asText(""));
        event.remove("alarmTime");
        if (anchor == null || !anchor.isAfter(now.plusSeconds(60))) {
            event.remove("alarmLeadMinutes");
            return;
        }
        int lead = requested ? event.path("alarmLeadMinutes").asInt(-1) : 0;
        if (lead < 0 || lead > 10080) lead = defaultLead(event.path("summary").asText(""));
        // An explicitly requested exact ringing time is not shifted earlier.
        if (requested && event.path("alarmExactTime").asBoolean(false)) lead = 0;
        lead = Math.min(lead, (int)Math.max(0, Duration.between(now.plusMinutes(1), anchor).toMinutes()));
        event.put("alarmLeadMinutes", lead);
        event.put("alarmTime", anchor.minusMinutes(lead).format(FORMAT));
    }
    public static int defaultLead(String summary) {
        String text = summary.toLowerCase(Locale.ROOT);
        if (text.matches(".*(起床|叫醒|吃药|服药|wake|medicine|medication).*")) return 0;
        if (text.matches(".*(航班|飞机|机场|高铁|火车|flight|airport|train).*")) return 120;
        if (text.matches(".*(提交|截止|交付|报告|材料|submit|deadline|report).*")) return 60;
        if (text.matches(".*(会议|开会|面试|预约|meeting|interview|appointment).*")) return 30;
        return 15;
    }
}
