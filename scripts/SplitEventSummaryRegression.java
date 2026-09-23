import java.util.*;
import java.lang.reflect.*;
public class SplitEventSummaryRegression extends Stage1Regression {
 public static void main(String[] args)throws Exception {
  Stage1Regression.main(args);
  String[] tasks={"9月23日9:53开会","9月23日15:30前交报告","9月24日14:00去客户现场"};
  String all=String.join("，",tasks);
  var tasksMethod=content.getClass().getDeclaredMethod("overviewTasks",Long.class);tasksMethod.setAccessible(true);
  var sourceMethod=content.getClass().getDeclaredMethod("overviewSource",Long.class,List.class);sourceMethod.setAccessible(true);
  var hashMethod=content.getClass().getDeclaredMethod("overviewHash",String.class);hashMethod.setAccessible(true);
  for(int i=0;i<3;i++){
   long id=9501+i;event(id,1,2);
   db.update("update blade_smart_event set source_candidate_id=9500,original_text=?,event_summary=? where id=?",all,tasks[i],id);
   db.update("update blade_smart_event_branch set task_content=? where event_id=?",tasks[i],id);
   String source=(String)sourceMethod.invoke(content,id,tasksMethod.invoke(content,id));String hash=(String)hashMethod.invoke(content,source);
   db.update("update blade_smart_event set overview_summary=?,overview_source_hash=? where id=?",all,hash,id);
   db.update("update blade_smart_event_branch set latest_summary=?,summary_source_hash=? where event_id=?",all,hash,id);
  }
  for(int i=0;i<3;i++){
   long id=9501+i;
   check(tasks[i].equals(content.latestTask(id,2L)),"split recipient ignores broad generated cache "+i);
   String summary=content.latestEventSummary(id,all);
   check(summary.contains(tasks[i]) && !summary.contains(tasks[(i+1)%3]),"split organizer overview stays in current event "+i);
   check(!content.refreshEventOverview(id),"shared utterance never re-summarizes split event "+i);
  }
  db.update("update blade_smart_event_branch set task_content='9月24日16:00去客户现场' where event_id=9503");
  check("9月24日16:00去客户现场".equals(content.latestTask(9503L,2L)),"later confirmed task change stays authoritative");
  db.update("update blade_smart_event set deadline_time='2026-09-23 15:30:00' where id=9502");
  db.update("update blade_smart_event_branch set task_content='今天15:30前交报告' where event_id=9502");
  check("9月23日15:30前交报告".equals(content.latestTask(9502L,2L)),"relative wording uses stored task date rather than today");
  check(count("select count(*) from blade_smart_event where source_candidate_id=9500")==3,"read fix does not merge/delete split events");
  check(all.equals(db.queryForObject("select original_text from blade_smart_event where id=9501",String.class)),"original evidence preserved");
 }
}
