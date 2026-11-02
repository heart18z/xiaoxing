package org.springblade.modules.smartreminder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;

/** The explicit UI selection bounds this turn, independently of model routing. */
public final class EventReplyScope {
    private EventReplyScope() {}
    public static String instruction(Long eventId) {
        return eventId == null ? "" : "\n本轮为用户显式引用事件回复，唯一允许处理的事件ID=" + eventId
            + "。只解释、澄清、反馈、更新或停止这一个事件；eventAction/eventActions/feedback/feedbacks必须使用该ID。"
            + "不要处理其他待回复提醒、候选事件、好友备注或人员别称，不新建或合并事件。其他要求请用户取消引用后另行发送。"
            + "仍须遵守发起人和接收人的权限；接收人只能反馈自己的任务。\n";
    }
    public static boolean bind(ObjectNode answer, Long eventId) {
        if (eventId == null) return true;
        if (!Set.of("chat", "clarify", "feedback", "update_event", "stop_event").contains(answer.path("intent").asText("chat"))) return false;
        for (String field : new String[]{"personAliases", "friendRemarks", "events"})
            if (answer.hasNonNull(field) && !answer.path(field).isEmpty()) return false;
        for (String field : new String[]{"eventAction", "feedback"}) {
            if (answer.hasNonNull(field) && !bindNode(answer.path(field), eventId)) return false;
        }
        for (String field : new String[]{"eventActions", "feedbacks"}) {
            if (!answer.hasNonNull(field)) continue;
            if (!answer.path(field).isArray()) return false;
            for (JsonNode node : answer.path(field)) if (!bindNode(node, eventId)) return false;
        }
        return true;
    }
    private static boolean bindNode(JsonNode node, Long eventId) {
        if (!(node instanceof ObjectNode object)) return false;
        String id = node.path("eventId").asText("").trim();
        if (!id.isEmpty() && !id.equals("null") && !id.equals(eventId.toString())) return false;
        object.put("eventId", eventId.toString());
        return true;
    }
}
