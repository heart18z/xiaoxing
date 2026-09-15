import java.util.*;

/** Actual event-detail/list read paths on isolated H2 fixtures, with model calls forbidden. */
public class EventTimingIntegrationRegression extends EventPrivacyRegression {
  public static void main(String[] args)throws Exception {
    EventPrivacyRegression.main(args);
    db.update("update blade_smart_event set event_time='2099-01-01 15:00:00' where id=81");
    db.update("update blade_smart_event_branch set task_time_scoped=1,task_event_time='2099-01-01 13:00:00' where event_id=81");
    var detail=service.eventDetail(81L);var card=(Map<?,?>)detail.get("event");
    check(card.get("event_time").toString().startsWith("2099-01-01 13:00:00"),"creator detail returns current recipient time");
    var list=service.listEvents("sent","").stream().filter(row->"81".equals(row.get("id"))).findFirst().orElseThrow();
    check(list.get("eventTime").toString().startsWith("2099-01-01 13:00:00"),"creator event list matches detail task time");
    check(db.queryForObject("select event_time from blade_smart_event where id=81",java.sql.Timestamp.class).toString().startsWith("2099-01-01 15:00"),"display projection does not mutate global schedule");
    db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,task_content,task_time_scoped,task_event_time,next_evaluate_time) values(99181,81,3,'独立任务',1,'2099-01-01 16:00:00','2099-01-01 12:00:00')");
    card=(Map<?,?>)service.eventDetail(81L).get("event");
    check(card.get("event_time")==null&&Boolean.TRUE.equals(card.get("event_timeVaries")),"partial reschedule never presents a false universal time");
    event(83,1,-1);
    db.update("update blade_smart_event set event_time='2099-01-01 15:00:00' where id=83");
    db.update("update blade_smart_event_branch set task_time_scoped=1,task_event_time='2099-01-01 13:00:00' where event_id=83");
    card=(Map<?,?>)service.eventDetail(83L).get("event");
    check(card.get("event_time").toString().startsWith("2099-01-01 13:00:00"),"recipient detail retains own scoped time");
    list=service.listEvents("received","").stream().filter(row->"83".equals(row.get("id"))).findFirst().orElseThrow();
    check(list.get("eventTime").toString().startsWith("2099-01-01 13:00:00"),"recipient list also matches own scoped time");
  }
}
