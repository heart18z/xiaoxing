package org.springblade.modules.smartreminder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Shared event history must never republish other people's schedules. */
public final class ConflictPrivacy {
    public static final String NOTICE = "该时段已有其他安排，可能无法同时参加。请确认本事件是否继续或调整时间；其他安排的内容不予展示。";
    private static final ObjectMapper JSON = new ObjectMapper();
    private ConflictPrivacy() { }
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
