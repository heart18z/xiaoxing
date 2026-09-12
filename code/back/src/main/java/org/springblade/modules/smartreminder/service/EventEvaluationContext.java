package org.springblade.modules.smartreminder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springblade.core.log.exception.ServiceException;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Raw event conversations, not the global chat window or reconstructed feedback summaries. */
public final class EventEvaluationContext {
    private EventEvaluationContext() {}
    public static List<Map<String,Object>> window(List<Map<String,Object>> messages) {
        Map<String,Map<String,Object>> unique=new LinkedHashMap<>();
        for(var row:messages) {
            Map<String,Object> lean=new LinkedHashMap<>();
            for(String key:List.of("id","messageRole","messageType","content","createTime"))
                lean.put(key,Objects.toString(row.get(key),""));
            unique.put(lean.get("id").toString(),lean);
        }
        List<Map<String,Object>> rows=new ArrayList<>(unique.values());
        rows.sort(Comparator.<Map<String,Object>,String>comparing(r->r.get("createTime").toString()).thenComparing(r->new java.math.BigInteger(r.get("id").toString())));
        if(rows.size()<=50)return rows;
        List<Map<String,Object>> selected=new ArrayList<>(rows.subList(0,10));selected.addAll(rows.subList(rows.size()-40,rows.size()));return selected;
    }
    public static String fit(ObjectMapper mapper,String system,AiConfigService.AiRuntimeConfig config,Map<String,Object> state,
            List<Map<String,Object>> creator,List<Map<String,Object>> recipient,List<Map<String,Object>> timeline,Object otherSchedules) throws Exception {
        long output=config.maxTokens();
        var extra=mapper.readTree(Objects.toString(config.extraBody(),"{}"));
        for(String key:List.of("max_tokens","max_completion_tokens","max_output_tokens"))
            if(extra!=null&&extra.path(key).canConvertToLong())output=Math.max(output,extra.path(key).asLong());
        long budget=Math.min((long)config.maxInputTokens(),(long)config.contextWindow()-output)-256;
        var owner=new ArrayList<>(window(creator));var target=new ArrayList<>(window(recipient));
        Map<String,Object> input=new LinkedHashMap<>();input.put("current_state",state);
        input.put("creator_event_conversation",owner);input.put("recipient_event_conversation",target);
        input.put("timeline",timeline);input.put("other_anonymous_schedules",otherSchedules);
        input.put("conversation_window","每方最早10条及最近40条，去重后按时间排列；没有全局聊天上下文。空对话只表示没有取到记录，不证明从未通知。");
        while(true) {
            String text=mapper.writeValueAsString(input);
            // Same conservative estimate used by chat; preserve full records, never silently cut a message.
            if((long)system.getBytes(StandardCharsets.UTF_8).length+text.getBytes(StandardCharsets.UTF_8).length<=budget)return text;
            List<Map<String,Object>> larger=owner.size()>=target.size()?owner:target;
            if(larger.size()<=2)throw new ServiceException("事件评估必要背景及首尾对话超出模型容量，请增大上下文配置；本次未执行通知决策");
            // Preserve earliest requirement and newest exchange. Remove the middle before either end.
            larger.remove(larger.size()>11?10:1);
            input.put("conversation_window","受模型容量限制，已从对话窗口中间裁剪完整消息；保留双方最初要求和最新记录。缺失记录不是未发生的证据，当前状态和时间轴保持完整。");
        }
    }
}
