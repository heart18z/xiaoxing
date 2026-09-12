import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import java.lang.reflect.*;
import org.springblade.modules.smartreminder.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.*;

public class Stage3FilesRegression {
  static int visions;static String mime;
  static class Vision extends NewApiClient {
    Vision(){super(null,new ObjectMapper());}
    public AiAnswer describeImage(byte[] bytes,String type){visions++;mime=type;return new AiAnswer("图片上有一只白色小狗，背景为蓝色","","fixture","");}
  }
  static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+label);}
  public static void main(String[]args)throws Exception{
    var ds=new DriverManagerDataSource("jdbc:h2:mem:stage3;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
    var db=new JdbcTemplate(ds);
    db.execute("create table blade_smart_file(id bigint,user_id bigint,original_name varchar(100),extracted_text clob)");
    var service=new SmartFileService(db,new Vision());
    Method extract=SmartFileService.class.getDeclaredMethod("extract",Path.class,String.class);extract.setAccessible(true);
    Path folder=Files.createTempDirectory("stage3-files-");
    Path txt=folder.resolve("sample.txt");Files.writeString(txt,"周五下午三点开会",StandardCharsets.UTF_8);
    check(extract.invoke(service,txt,"txt").equals("周五下午三点开会"),"UTF-8 text extracted");
    Files.write(txt,"中文文件内容".getBytes(Charset.forName("GB18030")));
    check(extract.invoke(service,txt,"md").equals("中文文件内容"),"Chinese legacy text encoding supported");
    Path docx=folder.resolve("sample.docx");
    try(var doc=new XWPFDocument();var out=Files.newOutputStream(docx)){doc.createParagraph().createRun().setText("文档会议时间");doc.createTable(1,1).getRow(0).getCell(0).setText("下午三点");doc.write(out);}
    String word=(String)extract.invoke(service,docx,"docx");check(word.contains("文档会议时间")&&word.contains("下午三点"),"Word paragraphs and tables extracted");
    Path pdf=folder.resolve("sample.pdf");
    try(var doc=new PDDocument()){var page=new PDPage();doc.addPage(page);try(var stream=new PDPageContentStream(doc,page)){stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);stream.newLineAtOffset(40,700);stream.showText("Meeting 15:00");stream.endText();}doc.save(pdf.toFile());}
    check(((String)extract.invoke(service,pdf,"pdf")).contains("Meeting 15:00"),"PDF text extracted");
    check(visions==0,"documents do not invoke image model");
    Path image=Path.of("code/front/public/avatars/assistant/A3.png");
    check(((String)extract.invoke(service,image,"png")).contains("白色小狗")&&visions==1&&mime.equals("image/png"),"image invokes multimodal parsing");
    boolean rejected=false;try{extract.invoke(service,txt,"png");}catch(InvocationTargetException ex){rejected=true;}
    check(rejected&&visions==1,"fake image rejected before model call");
    Path webp=folder.resolve("sample.webp");Files.write(webp,Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEAAUAmJaQAA3AA/vuUAAA="));
    extract.invoke(service,webp,"webp");check(mime.equals("image/webp"),"WEBP image detected and sent with correct MIME");
    db.update("insert into blade_smart_file values(1,11,'own.txt','自己的内容'),(2,22,'private.txt','别人的内容')");
    check(service.combinedText(List.of(1L),11L).contains("自己的内容"),"own attachment included in context");
    rejected=false;try{service.combinedText(List.of(2L),11L);}catch(Exception ex){rejected=true;}
    check(rejected,"other user attachment excluded from context");
    rejected=false;try{service.combinedText(List.of(1L,1L),11L);}catch(Exception ex){rejected=true;}
    check(rejected,"duplicate attachments rejected");
    System.out.println("ALL STAGE 3 FILE REGRESSIONS PASSED; fixtures: "+folder);
  }
}
