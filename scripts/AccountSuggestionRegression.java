import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springblade.modules.smartreminder.service.AppAccountService;

public class AccountSuggestionRegression {
  public static void main(String[] args)throws Exception {
    var source=new DriverManagerDataSource("jdbc:h2:mem:suggestions;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","sa","");
    var db=new JdbcTemplate(source);var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
    db.execute("create table blade_user(tenant_id varchar(12),account varchar(32),unique(tenant_id,account))");
    db.execute("create table blade_app_account_sequence(account_day char(8) primary key,next_value bigint not null default 0)");
    String day=LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.BASIC_ISO_DATE);
    db.update("insert into blade_user values('000000',?)",day+"01");
    var service=new AppAccountService(db,null);
    String first=tx.execute(s->service.suggestAccount());if(!first.equals(day+"02"))throw new AssertionError("existing account not skipped: "+first);
    var pool=Executors.newFixedThreadPool(6);var ids=new HashSet<String>();var jobs=new ArrayList<Future<String>>();
    try {
      for(int i=0;i<30;i++)jobs.add(pool.submit(()->tx.execute(s->new AppAccountService(db,null).suggestAccount())));
      for(var job:jobs)if(!ids.add(job.get()))throw new AssertionError("concurrent suggestion collision");
    }finally{pool.shutdownNow();}
    db.update("update blade_app_account_sequence set next_value=99 where account_day=?",day);
    if(!tx.execute(s->service.suggestAccount()).equals(day+"100"))throw new AssertionError("counter overflow at 99");
    if(!tx.execute(s->new AppAccountService(db,null).suggestAccount()).equals(day+"101"))throw new AssertionError("restart lost counter");
    System.out.println("PASS existing accounts skipped, 30 concurrent distinct suggestions, >99/day and restart persistence");
  }
}
