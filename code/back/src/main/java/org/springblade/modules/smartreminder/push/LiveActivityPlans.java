package org.springblade.modules.smartreminder.push;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

/** Recipient-scoped lock-screen projection. Never uses AI evaluation time or a creator's global summary. */
@Service
public class LiveActivityPlans {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    public static final long LEAD_SECONDS = 3600, GRACE_SECONDS = 1800;
    private static final Pattern CLOCK = Pattern.compile("(?i)(?<!\\d)(?:[01]?\\d|2[0-3])[:：][0-5]\\d|(?:[0-2]?\\d|[零一二两三四五六七八九十]+)\\s*(?:点|時|时)(?!间)|\\b\\d{1,2}\\s*(?:am|pm)\\b|(?:\\d+|[一二两三四五六七八九十半]+)个?(?:小时|分钟|秒钟)后|\\bin \\d+ (?:minutes?|hours?)\\b|午夜|正午");
    private static final Pattern IMMEDIATE = Pattern.compile("(?i)(现在|立即|马上|right now|immediately)");
    private final JdbcTemplate jdbc;
    public LiveActivityPlans(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record Plan(String key,String owner,String eventId,String branchId,String title,String kind,long targetAt,long startAt,long endAt) {
        public Map<String,Object> content(String language, Instant now) {
            return Map.of("key",key,"owner",owner,"eventId",eventId,"branchId",branchId,"title",title,
                "kind",kind,"targetAt",targetAt,"endAt",endAt,"language",language,"phase",now.getEpochSecond()>=targetAt?"DUE":"UPCOMING");
        }
    }
    public List<Plan> candidates(long userId, Instant now) {
        // Both self reminders and assignments use this SAME recipient test. Read receipts do not end a task.
        var rows=jdbc.queryForList("""
            select b.id,b.event_id,b.recipient_user_id,b.task_content,b.task_time_scoped,b.create_time,
              case when b.task_time_scoped=1 then b.task_event_time else e.event_time end as target_time,
              case when b.task_time_scoped=1 then b.task_deadline_time else e.deadline_time end as due_time,
              case when b.task_time_scoped=1 then '' else e.time_description end as time_description
            from blade_smart_event_branch b join blade_smart_event e on e.id=b.event_id
            where b.recipient_user_id=? and b.branch_status='ACTIVE' and e.event_status='ACTIVE'
            """,userId);
        List<Plan> result=new ArrayList<>();
        for(var row:rows) {
            String task=Objects.toString(row.get("task_content"),"").trim();
            if(task.isBlank()) continue; // Never leak a whole-group summary as a fallback.
            String evidence=task+" "+Objects.toString(row.get("time_description"),"");
            Instant event=time(row.get("target_time")), deadline=time(row.get("due_time"));
            boolean exact=CLOCK.matcher(evidence).find();
            if(exact) {
                add(result,row,task,"EVENT",event,now);
                if(!Objects.equals(event,deadline)) add(result,row,task,"DEADLINE",deadline,now);
            } else if(event==null && deadline==null && IMMEDIATE.matcher(task).find()) {
                add(result,row,task,"NOW",time(row.get("create_time")),now);
            }
        }
        result.sort(Comparator.comparingInt((Plan p)->"NOW".equals(p.kind())?0:1).thenComparingLong(Plan::targetAt).thenComparing(Plan::key));
        return result;
    }
    private static void add(List<Plan> out,Map<String,Object> row,String title,String kind,Instant target,Instant now) {
        if(target==null) return;
        long at=target.getEpochSecond(),start="NOW".equals(kind)?at:at-LEAD_SECONDS,end=at+GRACE_SECONDS;
        if(now.getEpochSecond()<start || now.getEpochSecond()>=end) return;
        String branch=row.get("id").toString(), key=PushDeviceService.hash(branch+":"+kind+":"+at).substring(0,40);
        String preview=title.codePointCount(0,title.length())>140?title.substring(0,title.offsetByCodePoints(0,140))+"…":title;
        out.add(new Plan(key,row.get("recipient_user_id").toString(),row.get("event_id").toString(),branch,preview,kind,at,start,end));
    }
    static Instant time(Object value) {
        if(value instanceof Timestamp t) return t.toLocalDateTime().atZone(ZONE).toInstant();
        if(value instanceof LocalDateTime t) return t.atZone(ZONE).toInstant();
        if(value instanceof Instant t) return t;
        return null;
    }
}
