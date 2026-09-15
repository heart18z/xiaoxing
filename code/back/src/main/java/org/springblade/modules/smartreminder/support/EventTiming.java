package org.springblade.modules.smartreminder.support;

import java.util.*;

/** Read-model timing only. Never changes the persisted global/recipient scheduling scope. */
public final class EventTiming {
    private EventTiming() {}
    public static void apply(Map<String,Object> event,List<Map<String,Object>> branches,String timeKey,String deadlineKey) {
        if (branches.isEmpty()) return;
        var active=branches.stream().filter(row->"ACTIVE".equals(row.get("branchStatus"))).toList();
        var relevant=active.isEmpty()?branches:active;
        project(event,relevant,timeKey,"taskEventTime");
        project(event,relevant,deadlineKey,"taskDeadlineTime");
    }
    private static void project(Map<String,Object> event,List<Map<String,Object>> branches,String key,String branchKey) {
        Map<String,Object> values=new LinkedHashMap<>();
        for(var branch:branches) {
            Object value=branch.containsKey(branchKey)?branch.get(branchKey):event.get(key);
            values.put(value==null?"":value.toString(),value);
        }
        event.put(key+"Varies",values.size()>1);
        event.put(key,values.size()==1?values.values().iterator().next():null);
    }
}
