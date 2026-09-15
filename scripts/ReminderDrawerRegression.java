import org.springblade.modules.smartreminder.service.ReminderDrawerService;
public class ReminderDrawerRegression extends EventTimingIntegrationRegression {
  public static void main(String[] args)throws Exception{
    EventTimingIntegrationRegression.main(args);
    var drawer=new ReminderDrawerService(db);
    var received=drawer.list(2L,true);
    check(received.stream().noneMatch(r->"独立任务".equals(r.get("summary"))),"recipient drawer never exposes another recipient task");
    var creator=drawer.list(-1L,false);
    check(creator.stream().anyMatch(r->"99181".equals(r.get("branchId"))),"creator sees own recipient branches");
    var scoped=creator.stream().filter(r->"99181".equals(r.get("branchId"))).findFirst().orElseThrow();
    check(scoped.get("eventTime").toString().contains("16:00"),"drawer uses branch-specific schedule");
    check(drawer.list(999L,true).isEmpty()&&drawer.list(999L,false).isEmpty(),"unrelated user sees no events");
    check(creator.stream().allMatch(r->r.get("lastRemindedAt")==null),"evaluation timeline is not treated as reminder delivery");
    db.update("insert into blade_smart_timeline(id,event_id,branch_id,node_type,content) values(99185,81,99181,'REMINDER_SENT','fixture reminder')");
    check(drawer.list(-1L,false).stream().anyMatch(r->"99181".equals(r.get("branchId"))&&r.get("lastRemindedAt")!=null),"actual reminder timestamp exposed");
  }
}
