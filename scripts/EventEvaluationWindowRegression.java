import org.springblade.modules.smartreminder.service.*;
import java.util.*;
public class EventEvaluationWindowRegression extends Stage1Regression {
 public static void main(String[] args)throws Exception {
  List<Map<String,Object>> rows=new ArrayList<>();
  for(int i=1;i<=100;i++)rows.add(Map.of("id",String.valueOf(i),"messageRole",i%2==0?"assistant":"user","messageType","TEXT","content","原文-"+i+"文".repeat(100),"createTime","2026-09-11 09:00:00"));
  rows.add(rows.get(0));Collections.reverse(rows);
  var window=EventEvaluationContext.window(rows);
  check(window.size()==50&&window.get(0).get("id").equals("1")&&window.get(9).get("id").equals("10")&&window.get(10).get("id").equals("61")&&window.get(49).get("id").equals("100"),"earliest 10/latest 40 deduplicated and chronologically sorted");
  var config=new AiConfigService.AiRuntimeConfig(1L,"fixture","","","fixture",12000,11000,1000,java.math.BigDecimal.ONE,"",false,1000,"","",true);
  var tree=mapper.readTree(EventEvaluationContext.fit(mapper,"SYSTEM",config,Map.of("current_task","当前安排"),rows,rows,List.of(Map.of("content","时间轴保留")),List.of()));
  check(tree.path("conversation_window").asText().contains("裁剪"),"model capacity triggers explicit whole-message trimming");
  for(String key:List.of("creator_event_conversation","recipient_event_conversation")){
   var list=tree.path(key);check(list.get(0).path("id").asText().equals("1")&&list.get(list.size()-1).path("id").asText().equals("100"),"each participant keeps first and latest messages");
   check(list.get(0).path("content").asText().equals("原文-1"+"文".repeat(100)),"message text is not rewritten or silently clipped");
  }
  check(tree.path("timeline").size()==1&&tree.path("current_state").path("current_task").asText().equals("当前安排"),"required state and timeline retained");
  boolean denied=false;try{EventEvaluationContext.fit(mapper,"S".repeat(20000),config,Map.of(),rows,rows,List.of(),List.of());}catch(org.springblade.core.log.exception.ServiceException expected){denied=true;}
  check(denied,"oversized required context fails safely instead of overflowing or making a blind decision");
 }
}
