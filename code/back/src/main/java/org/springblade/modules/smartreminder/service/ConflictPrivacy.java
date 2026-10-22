package org.springblade.modules.smartreminder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Shared event history must never republish other people's schedules. */
public final class ConflictPrivacy {
    public static final String NOTICE = "该时段已有其他安排，可能无法同时参加。请确认本事件是否继续或调整时间；其他安排的内容不予展示。";
    public static final String EVALUATION_RULES = """

        冲突定位：other_anonymous_schedules 中提供的是当前接收人自己的事项和 eventId，仅用于内部判断。
        timeConflict=true 时，必须额外输出 conflictingEventIds 数组，逐一引用确实与当前事项冲突的其他安排 eventId。
        只引用本次输入中存在的 eventId，不能虚构或仅凭提醒时间判断冲突。没有具体冲突对象时不要声称已经确认冲突。
        其他事项的内容只用于判断，不得写入发给当前事件发起人的 reminderContent 或其他公开回复；系统将分别生成本人可见的具体冲突说明和发起人可见的时间冲突提示。
        """;
    private static final ObjectMapper JSON = new ObjectMapper();
    private ConflictPrivacy() { }
    /** Only the authenticated owner of a personal inbox may see the referenced tasks. */
    public static void sanitizePersonal(Map<String,Object> row,Long inboxOwner) {
        try {
            JsonNode payload=JSON.readTree(Objects.toString(row.getOrDefault("payloadJson",row.get("payload_json")),"{}"));
            if(inboxOwner!=null && payload!=null && inboxOwner.toString().equals(payload.path("conflictOwnerId").asText())
                && Set.of("ASSIGNMENT","EVALUATION").contains(payload.path("conflictStage").asText()) && payload.path("conflictingEvents").isArray()) return;
        } catch(Exception ignored) { }
        sanitize(row);
    }
    public static void sanitize(Map<String,Object> row) {
        String type = Objects.toString(row.getOrDefault("messageType", row.get("message_type")), "");
        String node = Objects.toString(row.get("nodeType"), "");
        JsonNode payload;
        try { payload = JSON.readTree(Objects.toString(row.getOrDefault("payloadJson", row.get("payload_json")), "{}")); }
        catch (Exception ignored) { payload = JSON.createObjectNode(); }
        if (("CONFLICT".equals(type) && (payload==null || !payload.has("detail"))) || "TIME_CONFLICT_DETECTED".equals(node) || (payload != null && payload.path("timeConflict").asBoolean(false))) {
            row.put("content", NOTICE);
            var safe = JSON.createObjectNode();
            if (payload != null) for (String key : Set.of("eventId", "branchId", "eventNo")) if (payload.hasNonNull(key)) safe.set(key, payload.get(key));
            safe.put("timeConflict", true);
            row.put("payloadJson", safe.toString()); row.remove("payload_json"); row.remove("payload");
        }
        if (!node.isEmpty() && !Set.of("EVENT_MERGED", "BRANCH_STOPPED", "EVENT_UPDATED").contains(node)) row.remove("payloadJson");
    }
}
