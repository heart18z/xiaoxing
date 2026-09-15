import java.util.*;
import org.springblade.modules.smartreminder.support.EventTiming;

public class EventTimingRegression {
  static Map<String,Object> branch(String status,String time){var row=new HashMap<String,Object>();row.put("branchStatus",status);row.put("taskEventTime",time);row.put("taskDeadlineTime",null);return row;}
  static Map<String,Object> event(){return new HashMap<>(Map.of("event_time","2030-09-15 15:00:00"));}
  static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+label);}
  public static void main(String[] args){
    var a=branch("ACTIVE","2030-09-15 13:00:00");var stopped=branch("STOPPED","2030-09-15 15:00:00");var e=event();
    EventTiming.apply(e,List.of(a,stopped),"event_time","deadline_time");
    check("2030-09-15 13:00:00".equals(e.get("event_time"))&&!Boolean.TRUE.equals(e.get("event_timeVaries")),"creator uses current active task time, not old global or stopped branch");
    var b=branch("ACTIVE","2030-09-15 16:00:00");e=event();EventTiming.apply(e,List.of(a,b),"event_time","deadline_time");
    check(e.get("event_time")==null&&Boolean.TRUE.equals(e.get("event_timeVaries")),"different recipient times explicitly marked as varied");
    check("2030-09-15 13:00:00".equals(a.get("taskEventTime"))&&"2030-09-15 16:00:00".equals(b.get("taskEventTime")),"projection does not overwrite recipient schedules");
    e=event();EventTiming.apply(e,List.of(branch("ACTIVE",null)),"event_time","deadline_time");
    check(e.get("event_time")==null&&!Boolean.TRUE.equals(e.get("event_timeVaries")),"explicit cleared branch time never falls back to obsolete master time");
    e=event();EventTiming.apply(e,List.of(stopped),"event_time","deadline_time");check("2030-09-15 15:00:00".equals(e.get("event_time")),"stopped-only event preserves historical branch time");
  }
}
