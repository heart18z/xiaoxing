package org.springblade.modules.smartreminder.service;

import org.springframework.stereotype.Component;
import java.util.concurrent.*;

/** A stop is accepted only before business actions begin; it never claims to undo committed actions. */
@Component
public class ChatRunRegistry {
  private final ConcurrentHashMap<String,Run> runs=new ConcurrentHashMap<>();
  private static final ThreadLocal<Run> CURRENT=new ThreadLocal<>();
  public Run register(Long user,String requestId){
    if(requestId==null||!requestId.matches("[A-Za-z0-9-]{16,80}"))throw new IllegalArgumentException("无效的对话请求编号");
    String key=user+":"+requestId;Run run=new Run(key);
    if(runs.putIfAbsent(key,run)!=null)throw new IllegalArgumentException("该对话请求正在处理");
    return run;
  }
  public boolean stop(Long user,String requestId){Run run=runs.get(user+":"+requestId);return run!=null&&run.cancel();}
  public boolean hasActive(Long user){return runs.keySet().stream().anyMatch(key->key.startsWith(user+":"));}
  public void attach(Run run){CURRENT.set(run);run.check();}
  public void finish(Run run){CURRENT.remove();runs.remove(run.key,run);run.clear();}
  public static void check(){Run run=CURRENT.get();if(run!=null)run.check();}
  public static void onCancel(Runnable action){Run run=CURRENT.get();if(run!=null)run.resource(action);}
  public static void beginActions(){Run run=CURRENT.get();if(run!=null)run.beginActions();}
  public static final class Run {
    private final String key;private boolean cancelled,applying;private Runnable close;
    Run(String key){this.key=key;}
    synchronized void check(){if(cancelled)throw new CancellationException("已停止生成");}
    synchronized void beginActions(){check();applying=true;close=null;}
    synchronized void resource(Runnable action){if(cancelled){action.run();throw new CancellationException("已停止生成");}close=action;}
    boolean cancel(){Runnable action;synchronized(this){if(applying)return false;cancelled=true;action=close;close=null;}if(action!=null)try{action.run();}catch(Exception ignored){}return true;}
    synchronized void clear(){close=null;}
  }
}
