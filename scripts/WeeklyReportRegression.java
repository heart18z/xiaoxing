import java.util.*;
import java.time.*;
import java.nio.file.*;
import java.io.*;
import org.springblade.modules.smartreminder.service.*;
import org.springblade.modules.smartreminder.config.ReportSchema;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class WeeklyReportRegression extends Stage1Regression {
  static int calls;
  static class ReportAi extends NewApiClient {
    ReportAi(){super(null,mapper);}
    @Override public AiAnswer chat(AiConfigService.AiRuntimeConfig config,String system,String input){
      calls++;return new AiAnswer("{\"chatSummary\":\"这段时间你已完成**材料提交**，会议还在准备中。建议下一步先核对参会安排。\",\"overview\":\"本周完成了材料提交，会议仍在准备中。\",\"insight\":\"下周先确认会议安排。\",\"progress\":[{\"eventId\":\"96001\",\"title\":\"会议准备\",\"detail\":\"确认参会安排。\"},{\"eventId\":\"999999\",\"title\":\"外部事件\",\"detail\":\"不可见\"}],\"nextWeek\":[{\"eventId\":\"96001\",\"category\":\"prepare\",\"title\":\"准备会议\",\"detail\":\"建议核对材料。\"}]}","","fixture","");
    }
  }
  public static void main(String[] args)throws Exception{
    Stage1Regression.main(args);
    db.execute("alter table blade_user add status int default 1");
    db.execute("create table blade_menu(id bigint primary key,parent_id bigint,code varchar,name varchar,alias varchar,path varchar,source varchar,sort int,category int,action int,is_open int,component varchar,remark varchar,is_deleted int)");
    db.execute("create table blade_role_menu(id bigint primary key,role_id bigint,menu_id bigint)");
    new ReportSchema(db).migrate();new ReportSchema(db).migrate();
    db.update("update blade_user set is_deleted=1");
    db.update("insert into blade_user(id,account,name) values(901,'owner','发起人'),(902,'reader','接收人'),(903,'private','其他接收人')");
    event(96001,901,902);db.update("insert into blade_smart_event_branch(id,event_id,recipient_user_id,current_fact,task_content) values(296001,96001,903,'秘密绩效','秘密任务')");
    db.update("update blade_smart_event_branch set task_content='参加会议',latest_summary='参加会议' where id=106001");
    db.update("insert into blade_smart_timeline(id,event_id,branch_id,node_type,content,create_time) values(960001,96001,106001,'RECIPIENT_FEEDBACK','已确认会议地点','2026-10-28 09:00:00'),(960002,96001,296001,'RECIPIENT_FEEDBACK','秘密反馈','2026-10-28 10:00:00')");
    event(96002,902,902);event(96003,902,902);event(96004,901,902);
    db.update("update blade_smart_event set create_time='2026-10-26 09:00:00' where id between 96001 and 96004");
    db.update("update blade_smart_event_branch set branch_status='STOPPED',stop_time='2026-10-30 09:00:00',current_fact='材料已完成提交' where event_id=96002");
    db.update("update blade_smart_event_branch set branch_status='STOPPED',stop_time='2026-10-30 09:00:00',current_fact='会议取消，不用参加' where event_id=96003");
    db.update("update blade_smart_event_branch set branch_status='STOPPED',stop_time='2026-10-20 09:00:00',current_fact='旧事项已完成' where event_id=96004");
    var reports=new EventReportService(db,mapper,new ReportAi(),new FakeConfig());var period=EventReportService.period("2026-10-26","2026-10-31");
    var snapshot=reports.snapshot(902,period);var serialized=mapper.writeValueAsString(snapshot);
    check(!serialized.contains("秘密"),"recipient report excludes other branches' tasks and feedback");
    check(serialized.contains("已确认会议地点"),"report includes authorized progress within the reporting period");
    check(!serialized.contains("96004"),"old finished recipient branch is excluded");
    var counts=(Map<?,?>)snapshot.get("counts");check(counts.get("completed").equals(1)&&counts.get("stopped").equals(1)&&counts.get("active").equals(1),"completion and cancellation counted separately");
    var first=reports.generate(902,period,"MANUAL","test-report-1");long report=Long.parseLong(first.get("id").toString());
    reports.generate(902,period,"MANUAL","test-report-1");check(calls==1,"generation idempotency retains immutable snapshot");
    check(((List<?>)first.get("progress")).size()==1,"model cannot add unauthorized event links");
    boolean rejected=false;try{reports.detail(report,903,false);}catch(Exception e){rejected=true;}check(rejected,"report ownership enforced");
    reports.send(report,901L,"INITIAL");reports.send(report,901L,"INITIAL");check(count("select count(*) from blade_smart_report_delivery where report_id=?",report)==1,"duplicate send does not duplicate chat card");
    check(count("select count(*) from blade_smart_chat_message where user_id=902 and message_type='WEEKLY_REPORT'")==1,"card reaches selected user only");
    reports.saveSettings(903,false,1,9);reports.scheduledAt(LocalDateTime.of(2026,10,31,22,0));check(count("select count(*) from blade_smart_report where source='AUTO'")==0,"first deployment does not backfill outside configured weekday");
    reports.scheduledAt(LocalDateTime.of(2026,11,2,8,59));check(count("select count(*) from blade_smart_report where source='AUTO'")==0,"scheduled time respected");
    reports.scheduledAt(LocalDateTime.of(2026,11,2,9,0));reports.scheduledAt(LocalDateTime.of(2026,11,2,9,5));
    check(count("select count(*) from blade_smart_report where source='AUTO' and user_id=902")==1,"weekly scheduler is idempotent");
    check(count("select count(*) from blade_smart_report where source='AUTO' and user_id=903")==0,"personal weekly switch respected");
    var docs=new ReportDocuments();byte[] pdf=docs.export(first,"pdf"),word=docs.export(first,"docx");
    Files.createDirectories(Path.of("output/mobile142/weekly-documents"));Files.write(Path.of("output/mobile142/weekly-documents/sample.pdf"),pdf);Files.write(Path.of("output/mobile142/weekly-documents/sample.docx"),word);
    try(var document=Loader.loadPDF(pdf)){check(new PDFTextStripper().getText(document).contains("本周完成了材料提交"),"PDF embeds readable Chinese font");}
    try(var document=new XWPFDocument(new ByteArrayInputStream(word))){check(document.getParagraphs().stream().anyMatch(p->p.getText().contains("本周完成了材料提交")),"Word document matches report snapshot");}
    check(first.get("chart").toString().startsWith("data:image/png;base64,"),"native donut image generated from real counts");
    Files.writeString(Path.of("output/mobile142/weekly-documents/fixture.json"),mapper.writeValueAsString(first));
    var field=SmartReminderService.class.getDeclaredField("reportService");field.setAccessible(true);field.set(service,reports);
    var reply=(Map<?,?>)complete.invoke(service,prepared.newInstance(902L,90102L,"归总本周事件并生成报告",null,null,"",List.of()),"{\"intent\":\"generate_report\",\"report\":{\"start\":\"2026-10-26\",\"end\":\"2026-10-31\"},\"eventActions\":[{\"type\":\"stop_event\",\"eventId\":\"96001\"}]}","");
    check("REPORT_LINK".equals(reply.get("messageType"))&&reply.containsKey("reportId"),"query returns report hyperlink payload");
    check(reply.get("reply").equals(first.get("chatSummary")),"chat answers with the AI conversational summary");
    check(!reply.get("reply").toString().contains("事件明细") && first.get("markdown").toString().contains("事件明细"),"full report stays in detail and downloads, not chat");
    check(reports.chatReply(Map.of("insight","旧报告一句总结","markdown","不应返回全文")).equals("旧报告一句总结"),"legacy report uses short insight fallback");
    check(count("select count(*) from blade_smart_event_branch where event_id=96001 and branch_status='ACTIVE'")==2,"report request never executes event mutations");
    boolean duplicateRejected=false;try{reports.generate(902,new EventReportService.Period(LocalDate.of(2026,10,25),LocalDate.of(2026,10,31)),"MANUAL","test-report-1");}catch(Exception ex){duplicateRejected=true;}check(duplicateRejected,"generation key cannot silently return another period");
    check("2026-11-02".equals(first.get("nextStart")),"next-week suggestions start on Monday even for a partial current week");
    long automatic=count("select count(*) from blade_smart_report where source='AUTO'");reports.saveSettings(0,false,1,9);reports.scheduledAt(LocalDateTime.of(2026,11,9,9,0));
    check(count("select count(*) from blade_smart_report where source='AUTO'")==automatic,"global switch prevents automatic generation");
    db.update("insert into blade_user(id,account,name) values(904,'empty','无事件用户')");
    var empty=reports.generate(904,period,"AUTO","AUTO:904:2026-10-26");check("EMPTY".equals(empty.get("status")),"empty weeks are not sent as automatic cards");
    var failing=new EventReportService(db,mapper,new NewApiClient(null,mapper){@Override public AiAnswer chat(AiConfigService.AiRuntimeConfig c,String s,String u){throw new RuntimeException("fixture upstream unavailable");}},new FakeConfig());
    try{failing.generate(902,period,"MANUAL","retry-fixture");}catch(Exception expected){}
    check(count("select count(*) from blade_smart_report where generation_key='retry-fixture' and status='FAILED' and lease_until is not null")==1,"model failure is recorded with retry backoff");
    boolean backoff=false;try{reports.generate(902,period,"MANUAL","retry-fixture");}catch(Exception expected){backoff=true;}check(backoff,"retry lease prevents immediate repeated model requests");
    db.update("update blade_smart_report set lease_until='2020-01-01 00:00:00' where generation_key='retry-fixture'");
    check("READY".equals(reports.generate(902,period,"MANUAL","retry-fixture").get("status")),"failed generation can recover after its backoff");
    var longReport=new LinkedHashMap<String,Object>(first);longReport.put("markdown","## 多页测试\n\n"+"- 会议进展与材料核对记录。\n".repeat(160)+"最后一条完整记录");
    try(var document=Loader.loadPDF(docs.export(longReport,"pdf"))){check(document.getNumberOfPages()>1&&new PDFTextStripper().getText(document).contains("最后一条完整记录"),"long PDF paginates without dropping final content");}
    System.out.println("PASS WeeklyReportRegression");
  }
}
