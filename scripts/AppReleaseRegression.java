import org.springblade.modules.smartreminder.service.AppReleaseService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class AppReleaseRegression {
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println("PASS "+name);}
 static MultipartFile file(byte[] bytes){return new MultipartFile(){
 public String getName(){return "file";} public String getOriginalFilename(){return "test.apk";} public String getContentType(){return "application/vnd.android.package-archive";}
 public boolean isEmpty(){return bytes.length==0;}public long getSize(){return bytes.length;}public byte[] getBytes(){return bytes;}
 public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}public void transferTo(File f)throws IOException{Files.write(f.toPath(),bytes);}
 };}
 public static void main(String[] args)throws Exception {
 var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:releases;MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
 db.execute("create table blade_app_release(id bigint primary key,version_code int unique,version_name varchar(40),notes varchar(4000),file_name varchar(255),file_size bigint,sha256 varchar(64),published int,create_user bigint,create_time timestamp)");
 var service=new AppReleaseService(db);var directory=Files.createTempDirectory("xiaoxing-release-test");
 var field=AppReleaseService.class.getDeclaredField("uploadDir");field.setAccessible(true);field.set(service,directory.toString());
 var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){for(String name:List.of("AndroidManifest.xml","classes.dex")){zip.putNextEntry(new ZipEntry(name));zip.write(new byte[]{1,2,3});zip.closeEntry();}}
 try {
  check(service.latest().isEmpty(),"no release returns empty");
  var result=service.upload(file(out.toByteArray()),117,"1.0.117","test notes");long id=Long.parseLong(result.get("id").toString());
  check(result.get("sha256").toString().length()==64,"sha256 generated");check(service.latest().isEmpty(),"draft invisible");
  boolean rejected=false;try{service.download(id);}catch(Exception e){rejected=true;}check(rejected,"draft cannot download");
  check(db.queryForObject("select file_name from blade_app_release where id=?",String.class,id).equals("test.apk"),"original APK name stored");
  service.publish(id,true);
  rejected=false;try{service.delete(id);}catch(Exception e){rejected=true;}check(rejected,"published release cannot be deleted");
  check(!service.latest().isEmpty(),"published version visible");check(service.download(id).length()==out.size(),"download content size");
  service.publish(id,false);check(service.latest().isEmpty(),"withdraw removes update");
  rejected=false;try{service.upload(file(new byte[]{1,2}),118,"bad","bad");}catch(Exception e){rejected=true;}check(rejected,"invalid APK rejected");
  rejected=false;try{service.upload(file(out.toByteArray()),117,"duplicate","duplicate");}catch(Exception e){rejected=e.getMessage().contains("版本代码已存在")&&!e.getMessage().contains("SQL");}check(rejected,"duplicate version has friendly error");
  try(var files=Files.list(directory.resolve("app-releases"))){check(files.count()==1,"failed uploads cleaned without deleting published artifact");}
  service.delete(id);
  check(service.list().isEmpty()&&!Files.exists(directory.resolve("app-releases").resolve(id+".apk")),"withdrawn release and APK deleted");
  var replacement=service.upload(file(out.toByteArray()),117,"replacement","replacement");
  check(!replacement.get("id").equals(result.get("id")),"deleted version code can be uploaded again");
  rejected=false;try{service.delete(id);}catch(Exception e){rejected=true;}check(rejected,"stale delete fails clearly");
 }finally{try(var files=Files.walk(directory)){for(var path:files.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
 }
}
