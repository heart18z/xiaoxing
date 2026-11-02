import java.util.*;

/** Execute actual event list queries against isolated H2 fixtures; no network or live AI. */
public class EventOrder142Regression extends Stage1Regression {
    static void fixture(long id,long owner,long recipient,String created,String next,String status) {
        event(id,owner,recipient);
        db.update("update blade_smart_event set create_time=?,event_status=? where id=?",created,status,id);
        db.update("update blade_smart_event_branch set next_evaluate_time=?,branch_status=? where event_id=?",next,status,id);
    }
    static List<String> ids(String type,String status) {
        return service.listEvents(type,status).stream().map(row->row.get("id").toString()).toList();
    }
    public static void main(String[] args)throws Exception {
        Stage1Regression.main(args);
        db.execute("alter table blade_smart_event_branch drop constraint fail_new_branch");
        db.update("insert into blade_user(id,account,name) values(-1,'viewer','查看人')");
        fixture(9201,-1,2,"2099-10-20 10:00:00","2099-10-29 09:00:00","ACTIVE");
        fixture(9202,-1,2,"2099-10-26 10:00:00","2099-11-05 09:00:00","ACTIVE");
        fixture(9203,-1,2,"2099-10-19 10:00:00","2099-10-27 09:00:00","ACTIVE");
        db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,branch_status,next_evaluate_time) values(29203,9203,3,'STOPPED','2099-10-01 09:00:00')");
        fixture(9204,-1,2,"2099-10-26 10:00:00",null,"ACTIVE");
        fixture(9205,-1,2,"2099-10-20 10:00:00","2099-10-01 09:00:00","STOPPED");
        fixture(9206,-1,2,"2099-10-26 10:00:00","2099-10-27 09:00:00","ACTIVE");
        fixture(9207,-1,2,"2099-10-26 10:00:00","2099-10-27 09:00:00","ACTIVE");
        fixture(9208,-1,2,"2099-10-26 10:00:00","2099-10-20 09:00:00","ACTIVE");
        fixture(9209,-1,2,"2099-10-26 10:00:00",null,"ACTIVE");
        db.update("update blade_smart_event_branch set branch_status='COMPLETED',next_evaluate_time='2099-10-01 09:00:00' where event_id=9209");
        check(ids("sent","ACTIVE").equals(List.of("9208","9207","9206","9203","9201","9202","9209","9204")),"sent: nearest evaluation first, nulls last, stable time ties");
        check(ids("sent","").equals(List.of("9208","9207","9206","9203","9201","9202","9209","9204","9205")),"all events: stopped event with stale time stays at end");
        var sent=service.listEvents("sent","");
        check(sent.stream().filter(r->r.get("id").equals("9203")).findFirst().get().get("nextEvaluateTime").toString().startsWith("2099-10-27"),"creator uses earliest ACTIVE branch, ignores cancelled branch");
        check(sent.stream().filter(r->Set.of("9205","9209").contains(r.get("id"))).allMatch(r->r.get("nextEvaluateTime")==null),"terminal branches do not expose phantom evaluation times");
        fixture(9301,2,-1,"2099-10-26 10:00:00","2099-11-05 09:00:00","ACTIVE");
        fixture(9302,2,-1,"2099-10-20 10:00:00","2099-10-29 09:00:00","ACTIVE");
        fixture(9303,2,-1,"2099-10-26 10:00:00",null,"ACTIVE");
        fixture(9304,2,-1,"2099-10-26 10:00:00","2099-10-01 09:00:00","STOPPED");
        fixture(9305,2,-1,"2099-10-26 10:00:00","2099-10-29 09:00:00","ACTIVE");
        check(ids("received","ACTIVE").equals(List.of("9305","9302","9301","9303")),"received uses own branch evaluation time, not creation or other people's events");
        check(ids("received","").equals(List.of("9305","9302","9301","9304","9303")),"received all: null and terminal branch times sort last");
        db.update("update blade_smart_event_branch set next_evaluate_time='2099-10-10 09:00:00' where event_id=9301");
        check(ids("received","ACTIVE").get(0).equals("9301"),"rescheduled event moves immediately on next list refresh");
        System.out.println("ALL EVENT ORDER 142 REGRESSIONS PASSED");
    }
}
