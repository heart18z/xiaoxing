package org.springblade.modules.smartreminder.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReminderDrawerService {
  private final JdbcTemplate db;
  public List<Map<String,Object>> list(Long user,boolean received){
    // Scope recipient rows before returning any names, task text or reminder history.
    String scope=received?"b.recipient_user_id=? and e.creator_user_id<>?":"e.creator_user_id=?";
    var args=received?new Object[]{user,user}:new Object[]{user};
    var rows=db.queryForList("""
      select e.id as eventId,b.id as branchId,e.event_status as eventStatus,b.branch_status as branchStatus,
        coalesce(nullif(b.task_content,''),e.event_summary) as summary,
        e.create_time as createTime,
        case when b.task_time_scoped=1 then b.task_event_time else e.event_time end as eventTime,
        case when b.task_time_scoped=1 then b.task_deadline_time else e.deadline_time end as deadlineTime,
        b.next_evaluate_time as nextEvaluateTime,
        recipient.name as recipientName,creator.name as creatorName,
        (select max(t.create_time) from blade_smart_timeline t where t.event_id=e.id and t.branch_id=b.id and t.node_type='REMINDER_SENT') as lastRemindedAt
      from blade_smart_event e join blade_smart_event_branch b on b.event_id=e.id
      join blade_user recipient on recipient.id=b.recipient_user_id
      join blade_user creator on creator.id=e.creator_user_id
      where
      """+scope+" order by e.create_time desc,b.id",args);
    for(var row:rows){
      for(String key:List.of("eventId","branchId"))row.put(key,String.valueOf(row.get(key)));
      for(String key:List.of("createTime","eventTime","deadlineTime","nextEvaluateTime","lastRemindedAt"))if(row.get(key) instanceof java.sql.Timestamp value)row.put(key,value.toLocalDateTime().toString());
    }
    return rows;
  }
}
