import java.nio.file.*;
import java.util.*;
import java.math.BigDecimal;
import java.lang.reflect.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springblade.modules.smartreminder.service.*;

public class ImageContext123Regression {
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+label);}
 public static void main(String[] args)throws Exception{
  var json=new ObjectMapper();var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:images123;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""));
  db.execute("create table blade_smart_file(id bigint,user_id bigint,original_name varchar(100),extracted_text varchar(10000),file_path varchar(500),extension varchar(10))");
  db.execute("create table blade_smart_user_preference(user_id bigint,chat_context_start_id bigint)");
  db.execute("create table blade_smart_chat_message(id bigint,user_id bigint,message_role varchar(20),message_type varchar(20),content varchar(1000),payload_json varchar(2000),event_id bigint,create_time timestamp default current_timestamp)");
  Path photo=Files.createTempFile("photo-48mp-",".jpg");var original=new BufferedImage(8000,6000,BufferedImage.TYPE_INT_RGB);ImageIO.write(original,"jpeg",photo.toFile());original.flush();
  var ai=new NewApiClient(null,json);var files=new SmartFileService(db,ai);
  db.update("insert into blade_smart_file values(1,11,'work.jpg','9月24日14点去客户现场',?,'jpg'),(2,22,'private.jpg','private',?,'jpg')",photo.toString(),photo.toString());
  var parts=files.imageParts(List.of(1L),11L);String data=((Map<?,?>)parts.get(0).get("image_url")).get("url").toString();byte[] bytes=Base64.getDecoder().decode(data.substring(data.indexOf(',')+1));var scaled=ImageIO.read(new java.io.ByteArrayInputStream(bytes));
  check(scaled.getWidth()<=3200&&scaled.getHeight()<=3200&&bytes.length<8*1024*1024,"48 megapixel photo becomes bounded JPEG before model request");
  check(files.preview(1L,11L).get("image").toString().startsWith("data:image/jpeg;base64,"),"private thumbnail is valid image data");
  boolean denied=false;try{files.preview(1L,22L);}catch(Exception ex){denied=true;}check(denied,"another account cannot preview photo");
  denied=false;try{files.imageParts(List.of(2L),11L);}catch(Exception ex){denied=true;}check(denied,"another account cannot pass photo to model");
  db.update("insert into blade_smart_chat_message(id,user_id,message_role,message_type,content,payload_json) values(10,11,'user','TEXT','工作安排','{\"fileIds\":[\"1\"]}'),(11,11,'assistant','TEXT','已读取',null),(12,22,'user','TEXT','private','{\"fileIds\":[\"2\"]}')");
  var service=new SmartReminderService(db,json,ai,null,files,null,null,null);
  Method recent=SmartReminderService.class.getDeclaredMethod("recentConversationLines",Long.class,Long.class,int.class);recent.setAccessible(true);
  String history=recent.invoke(service,11L,20L,40).toString();check(history.contains("9月24日14点去客户现场")&&!history.contains("private"),"follow-up retains image facts without another user's content");
  Method ids=SmartReminderService.class.getDeclaredMethod("recentImageIds",Long.class,Long.class);ids.setAccessible(true);
  check(ids.invoke(service,11L,20L).equals(List.of(1L)),"follow-up references previous image from same visible chat");
  db.update("insert into blade_smart_user_preference values(11,11)");check(ids.invoke(service,11L,20L).equals(List.of()),"clearing chat removes previous image context");
  final List<JsonNode> requests=new ArrayList<>();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/chat",exchange->{JsonNode body=json.readTree(exchange.getRequestBody());requests.add(body);boolean streaming=body.path("stream").asBoolean();String response=streaming?"data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"识别图片\",\"content\":\"ok\"}}]}\n\ndata: [DONE]\n\n":"{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}";byte[] result=response.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type",streaming?"text/event-stream":"application/json");exchange.sendResponseHeaders(200,result.length);exchange.getResponseBody().write(result);exchange.close();});server.start();
  try{
   var config=new AiConfigService.AiRuntimeConfig(1L,"test","http://127.0.0.1:"+server.getAddress().getPort()+"/chat","fixture","vision",32000,16000,1000,BigDecimal.ZERO,"",true,10000,"","",true);
   ai.chat(config,"rules","把图片中的工作安排给陈一",parts);StringBuilder reasoning=new StringBuilder();ai.chatStream(config,"rules","把这些安排给陈一",parts,x->{},reasoning::append);
   check(requests.size()==2&&reasoning.toString().equals("识别图片"),"both normal and SSE requests complete");
   for(JsonNode request:requests){var content=request.path("messages").get(1).path("content");check(content.isArray()&&content.get(0).path("text").asText().contains("陈一")&&content.get(1).path("image_url").path("url").asText().equals(data),"wire payload includes actual image and current assignment instruction");}
  }finally{server.stop(0);Files.deleteIfExists(photo);}
  System.out.println("ALL IMAGE CONTEXT REGRESSIONS PASSED");
 }
}
