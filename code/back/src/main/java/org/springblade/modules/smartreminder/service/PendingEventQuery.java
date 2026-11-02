package org.springblade.modules.smartreminder.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Pattern;

/** A conservative, read-only fast path for personal pending-task questions. */
public final class PendingEventQuery {
    private PendingEventQuery() {}
    public record Range(LocalDate start, LocalDate end, String label) {}
    // Only query vocabulary is accepted. Named people/tasks, quoted text and compound commands
    // stay with the intent model; e.g. "列出待办，再取消会议" must not silently become a read.
    private static final Pattern QUERY_WORDS = Pattern.compile(
        "(?:请问|请|麻烦|帮我|给我|帮忙|能不能|可以|能|我想知道|想知道|告诉我|" +
        "总结|汇总|归总|梳理|整理|盘点|列出|列|查看|查询|查查|查|看看|看|一下|下|" +
        "我自己|自己|本人|我的|我|目前|现在|接下来|最近|这个月|本月|这月|这周|本周|这个星期|本星期|" +
        "下个月|下月|下周|下个星期|今天|今日|明天|全部|所有|剩余|剩下|还有|还要|还需要|还需|" +
        "哪些|那些|什么|多少|有|要|需要|尚未|还没|未|没|没有|待办|待处理|待完成|待跟进|未完成|" +
        "完成|做完|处理完|办完|处理|跟进|办|做|的|事情|事项|事件|任务|事|清单|情况|了|呢|吗|吧)+");
    public static Optional<Range> resolve(String content, LocalDate today) {
        String q=Objects.toString(content, "").replaceAll("\\s+", "").replaceAll("[？?。！!]+$", "");
        if(!QUERY_WORDS.matcher(q).matches())return Optional.empty();
        boolean question=q.matches(".*(哪些|那些|什么|多少|总结|汇总|归总|梳理|整理|盘点|列|查|看|想知道|告诉我).*");
        boolean pending=q.matches(".*(没做|未做|没有做|没完成|没有完成|未完成|没处理|未处理|没办|未办|待办|待处理|待完成|待跟进|还要|还需|剩余|剩下).*")
            ||q.matches(".*还有.*(事|任务).*");
        if(!question||!pending)return Optional.empty();
        List<String> times=List.of("这个月","本月","这月","这周","本周","这个星期","本星期","下个月","下月","下周","下个星期","今天","今日","明天")
            .stream().filter(q::contains).toList();
        if(times.size()>1)return Optional.empty();
        String time=times.isEmpty()?"":times.get(0);
        Range range=switch(time) {
            case "这个月","本月","这月" -> new Range(today,today.with(TemporalAdjusters.lastDayOfMonth()),"本月剩余待办");
            case "这周","本周","这个星期","本星期" -> new Range(today,today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),"本周剩余待办");
            case "下个月","下月" -> new Range(today.plusMonths(1).withDayOfMonth(1),today.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),"下月待办");
            case "下周","下个星期" -> new Range(today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)),today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusDays(6),"下周待办");
            case "今天","今日" -> new Range(today,today,"今日待办");
            case "明天" -> new Range(today.plusDays(1),today.plusDays(1),"明日待办");
            default -> new Range(null,null,"当前待办");
        };
        return Optional.of(range);
    }

    private record Task(String summary, boolean own, LocalDateTime time) {}
    /** Input must come from the existing user-authorized event context, never model-supplied IDs. */
    public static String reply(JsonNode events,long user,Range range,LocalDateTime now) {
        List<Task> tasks=new ArrayList<>();
        for(JsonNode event:events) {
            if(!"ACTIVE".equals(field(event,"eventStatus").asText()))continue;
            boolean creator=Long.toString(user).equals(field(event,"creatorUserId").asText());
            List<JsonNode> active=new ArrayList<>();
            for(JsonNode b:field(event,"branches"))
                if("ACTIVE".equals(field(b,"branchStatus").asText()) && (creator||Long.toString(user).equals(field(b,"recipientUserId").asText())))active.add(b);
            if(active.isEmpty())continue;
            List<JsonNode> own=active.stream().filter(b->Long.toString(user).equals(field(b,"recipientUserId").asText())).toList();
            List<JsonNode> visible=own.isEmpty()?active:own;
            LocalDateTime time=null;
            boolean relevant=false;
            for(JsonNode b:visible) {
                boolean scoped=field(b,"taskTimeScoped").asInt()==1;
                LocalDateTime eventTime=date(scoped?field(b,"taskEventTime"):field(event,"eventTime"));
                LocalDateTime deadline=date(scoped?field(b,"taskDeadlineTime"):field(event,"deadlineTime"));
                LocalDateTime planned=eventTime==null?deadline:deadline==null||eventTime.isBefore(deadline)?eventTime:deadline;
                // Include a task spanning the requested window, plus overdue tasks when asking about now.
                boolean inRange=range.end()==null || planned==null ||
                    (!planned.toLocalDate().isAfter(range.end()) &&
                     (!range.start().isAfter(now.toLocalDate()) || !planned.toLocalDate().isBefore(range.start()) ||
                      deadline!=null&&!deadline.toLocalDate().isBefore(range.start())));
                if(inRange) {relevant=true;if(planned!=null&&(time==null||planned.isBefore(time)))time=planned;}
            }
            if(!relevant)continue;
            String summary=own.isEmpty()?field(event,"latestSummary").asText(""):field(own.get(0),"latestSummary").asText("");
            if(summary.isBlank())summary=own.isEmpty()?field(event,"summary").asText(""):field(own.get(0),"taskContent").asText("");
            if(summary.isBlank())summary="事件 "+field(event,"eventNo").asText(field(event,"id").asText());
            tasks.add(new Task(summary,!own.isEmpty(),time));
        }
        tasks.sort(Comparator.comparing(Task::time,Comparator.nullsLast(Comparator.naturalOrder())));
        if(tasks.isEmpty())return "目前没有查到"+(range.end()==null?"仍在进行中的事项。":range.label()+"范围内仍在进行中的事项。")+"已完成和已停止的事项未计入待办。";
        StringBuilder out=new StringBuilder("### ").append(range.label()).append("\n\n查到 **").append(tasks.size()).append(" 项**仍在进行中的事项");
        if(range.end()!=null)out.append("（").append(range.start()).append(" 至 ").append(range.end())
            .append(range.start().isAfter(now.toLocalDate())?"；另列未定时间事项）":"；另列逾期和未定时间事项）");
        out.append("：\n");
        int shown=0;
        for(String section:List.of("按时间安排","已过计划时间，仍需确认","尚未设置时间")) {
            List<Task> group=tasks.stream().filter(t->section.equals(t.time()==null?"尚未设置时间":t.time().isBefore(now)?"已过计划时间，仍需确认":"按时间安排")).toList();
            if(group.isEmpty()||shown>=30)continue;
            out.append("\n**").append(section).append("**\n");
            for(Task task:group) {
                if(shown++>=30)break;
                out.append("- **").append(safe(task.summary())).append("**\n  ")
                    .append(task.own()?"我需要处理":"我发起，待对方处理")
                    .append(task.time()==null?" · 时间未定":" · "+task.time().format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm")))
                    .append("\n");
            }
        }
        if(tasks.size()>30)out.append("\n先列出 30 项，其余 ").append(tasks.size()-30).append(" 项可在事件列表查看。\n");
        return out.append("\n已完成和已停止的事项未计入；以上以当前事件状态为准。").toString();
    }
    private static LocalDateTime date(JsonNode node) {
        if(node.isNumber())return LocalDateTime.ofInstant(Instant.ofEpochMilli(node.asLong()),ZoneId.of("Asia/Shanghai"));
        try{return LocalDateTime.parse(node.asText().replace(' ','T'));}catch(Exception ignored){return null;}
    }
    private static JsonNode field(JsonNode node,String name) {
        // JDBC drivers may fold unquoted column aliases to lower case.
        return node.has(name)?node.path(name):node.path(name.toLowerCase(Locale.ROOT));
    }
    private static String safe(String text) {
        return text.replaceAll("[\\r\\n]+"," ").replace("\\","\\\\").replace("*","\\*").replace("_","\\_").replace("[","\\[").replace("]","\\]").replace("<","&lt;").replace(">","&gt;");
    }
}
