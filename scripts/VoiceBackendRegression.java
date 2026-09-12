import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.springblade.modules.smartreminder.service.*;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.*;
import org.springblade.modules.smartreminder.support.SecretCodec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;

public class VoiceBackendRegression {
  static String body="",response="{\"text\":\"明天下午三点提醒我开会\"}";static int requests,code=200;
  static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+label);}
  static void rejects(Runnable call,String label){try{call.run();throw new AssertionError(label);}catch(IllegalArgumentException|org.springblade.core.log.exception.ServiceException expected){check(true,label);}}
  static MultipartFile audio(String name,byte[] data){return new MultipartFile(){public String getName(){return "file";}public String getOriginalFilename(){return name;}public String getContentType(){return "audio/wav";}public boolean isEmpty(){return data.length==0;}public long getSize(){return data.length;}public byte[] getBytes(){return data;}public InputStream getInputStream(){return new ByteArrayInputStream(data);}public void transferTo(File f)throws IOException{java.nio.file.Files.write(f.toPath(),data);}};}
  static AiConfigRequest request(String type,String model,String url){var r=new AiConfigRequest();r.setConfigType(type);r.setConfigName(model);r.setBaseUrl(url);r.setModelName(model);r.setApiKey("isolated-secret");return r;}
  public static void main(String[] args)throws Exception{
    var db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:voice;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""));
    db.execute("create table blade_ai_service_config(id bigint primary key,config_type varchar(20) default 'LLM',model_alias varchar(100),system_default int default 0,config_name varchar(100),base_url varchar(500),api_key varchar(500),model_name varchar(100),context_window int,max_input_tokens int,max_tokens int,temperature decimal(4,2),reasoning_effort varchar(30),show_thinking int,request_timeout int,intent_prompt clob,decision_prompt clob,enabled int,create_user bigint,create_time timestamp,update_user bigint,update_time timestamp,is_deleted int)");
    db.execute("create table blade_smart_user_preference(user_id bigint primary key,ai_avatar varchar(1000),llm_config_id bigint,speech_config_id bigint,update_time timestamp)");
    db.execute("create table blade_smart_model_policy(config_type varchar(20) primary key,intent_prompt varchar(100000),decision_prompt varchar(100000),update_time timestamp)");
    db.update("insert into blade_smart_model_policy(config_type) values('LLM'),('SPEECH')");
    var configs=new AiConfigService(db,new SecretCodec("isolated-test-signing-key"));
    db.execute("alter table blade_ai_service_config add extra_body clob default '{}'");
    for(String column:List.of("llm_mode varchar(20) default 'SYSTEM'","speech_mode varchar(20) default 'SYSTEM'","llm_personal varchar(30000)","speech_personal varchar(30000)","language varchar(10) default 'zh-cn'"))db.execute("alter table blade_smart_user_preference add "+column);
    var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
    server.createContext("/v1/audio/transcriptions",exchange->{requests++;body=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);var bytes=response.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(code,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});server.start();
    try{
      String url="http://127.0.0.1:"+server.getAddress().getPort();
      Long llm1=configs.save(request("LLM","llm-one",url)),llm2=configs.save(request("LLM","llm-two",url)),speech=configs.save(request("SPEECH","paraformer-realtime-v2",url));
      check(configs.choices(false).size()==3&&configs.choices(true).size()==3,"multiple enabled models coexist");
      check(configs.enabledConfigForUser(2L,"LLM").id().equals(llm1),"adding enabled models preserves existing default");configs.setDefault("LLM",llm2);
      check(configs.currentMasked("SPEECH",speech).get("baseUrl").equals(url+"/v1/audio/transcriptions"),"speech endpoint normalization");
      check(db.queryForObject("select api_key from blade_ai_service_config where id=?",String.class,speech).startsWith("v1:"),"key encrypted at rest");
      check(configs.currentMasked("SPEECH",speech).get("apiKey").toString().contains("****"),"admin key masked");
      var preference=new ModelPreferenceRequest();preference.setLlmConfigId(llm2);preference.setSpeechConfigId(speech);
      db.update("insert into blade_smart_user_preference(user_id,ai_avatar) values(1,'keep-avatar')");configs.savePreferences(1L,preference);
      check(configs.enabledConfigForUser(1L,"LLM").id().equals(llm2),"system LLM used for current user");
      check(configs.enabledConfigForUser(2L,"LLM").id().equals(llm2),"another user follows system default");
      check(db.queryForObject("select ai_avatar from blade_smart_user_preference where user_id=1",String.class).equals("keep-avatar"),"saving models preserves avatar");
      check(!configs.preferences(1L).toString().contains("secret")&&!configs.preferences(1L).toString().contains("baseUrl"),"app settings reveal no credentials or provider URL");
      preference.setSpeechConfigId(llm1);rejects(()->configs.savePreferences(1L,preference),"wrong model category rejected");
      db.update("update blade_ai_service_config set enabled=0 where id=?",llm1);check(configs.enabledConfigForUser(1L,"LLM").id().equals(llm2),"disabled personal LLM safely falls back");
      configs.savePrompts(Map.of("intentPrompt","global-intent","decisionPrompt","global-decision"));
      configs.activate("LLM",llm1);
      check(configs.choices(true).size()==3&&configs.enabledConfigForUser(2L,"LLM").id().equals(llm2),"activation leaves other enabled models and default unchanged");
      configs.setDefault("LLM",llm1);check(configs.enabledConfigForUser(1L,"LLM").id().equals(llm2),"explicit user selection survives system default changes");
      check(configs.enabledConfigForUser(2L,"LLM").intentPrompt().equals("global-intent"),"switching models preserves global prompt");
      configs.setDefault("LLM",llm2);
      var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
      try{
        var transaction=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(db.getDataSource()));
        var a=pool.submit(()->transaction.execute(status->{configs.setDefault("LLM",llm1);return true;}));
        var b=pool.submit(()->transaction.execute(status->{configs.setDefault("LLM",llm2);return true;}));
        a.get();b.get();
        check(db.queryForObject("select count(*) from blade_ai_service_config where config_type='LLM' and system_default=1",Integer.class)==1,"concurrent defaults keep exactly one default model");
      }finally{pool.shutdownNow();configs.setDefault("LLM",llm2);}
      rejects(()->configs.activate("SPEECH",llm1),"cross-category activation rejected");
      check(configs.enabledConfigForUser(2L,"SPEECH").id().equals(speech),"LLM activation does not change speech");
      db.update("update blade_ai_service_config set extra_body=? where id=?","{\"language\":\"zh\"}",speech);
      var service=new SpeechService(configs,new ObjectMapper());byte[] wav=new byte[160];System.arraycopy("RIFF".getBytes(),0,wav,0,4);
      check(service.transcribe(1L,audio("recording.wav",wav)).get("text").equals("明天下午三点提醒我开会"),"speech response maps to draft text");
      check(body.contains("name=\"model\"")&&body.contains("paraformer-realtime-v2")&&body.contains("name=\"file\"")&&body.contains("filename=\"recording.wav\""),"upstream request uses multipart model and audio file");
      check(body.contains("name=\"language\"")&&body.contains("zh"),"speech model additional form parameters forwarded");
      int before=requests;rejects(()->service.transcribe(1L,audio("bad.exe",wav)),"unsupported file rejected");rejects(()->service.transcribe(1L,audio("empty.wav",new byte[0])),"empty file rejected");check(requests==before,"invalid audio never reaches provider");
      response="{\"text\":\" \"}";rejects(()->service.transcribe(1L,audio("recording.wav",wav)),"silence result rejected");
      code=401;response="private-provider-secret";try{service.transcribe(1L,audio("recording.wav",wav));throw new AssertionError("401");}catch(org.springblade.core.log.exception.ServiceException e){check(!e.getMessage().contains("private-provider-secret"),"provider errors never expose response secrets");}
      code=200;response="{\"text\":\"retry works\"}";check(service.transcribe(1L,audio("recording.wav",wav)).get("text").equals("retry works"),"retry works after failed request");
      var personal=new ModelPreferenceRequest();personal.setLlmMode("PERSONAL");personal.setSpeechMode("SYSTEM");personal.setLanguage("en-us");
      // Literal documentation address: validation only, never contacted, no external DNS dependency.
      var model=request("LLM","personal-model","https://203.0.113.10/v1");model.setApiKey("isolated-personal-only-key");model.setExtraBody("{\"enable_thinking\":false,\"max_tokens\":null}");personal.setLlm(model);
      configs.savePreferences(1L,personal);
      check(configs.enabledConfigForUser(1L,"LLM").modelName().equals("personal-model"),"personal endpoint and model selected only for owner");
      check(configs.enabledConfigForUser(2L,"LLM").modelName().equals("llm-two"),"another user cannot use personal model");
      String stored=db.queryForObject("select llm_personal from blade_smart_user_preference where user_id=1",String.class);
      check(stored.startsWith("v1:")&&!stored.contains("isolated-personal"),"entire personal configuration encrypted");
      check(!configs.preferences(1L).toString().contains("isolated-personal-only-key")&&!configs.preferences(2L).toString().contains("personal-model"),"personal credentials masked and isolated");
      check(configs.languageInstruction(1L).contains("English"),"language preference available to model prompt");
      model.setApiKey("");configs.savePreferences(1L,personal);check(configs.enabledConfigForUser(1L,"LLM").apiKey().equals("isolated-personal-only-key"),"blank key preserves existing credential");
      model.setBaseUrl("https://127.0.0.1/v1");rejects(()->configs.savePreferences(1L,personal),"personal endpoint cannot access internal services");model.setBaseUrl("https://203.0.113.10/v1");
      model.setExtraBody("{\"messages\":[]}");rejects(()->configs.savePreferences(1L,personal),"extra body cannot replace application messages");model.setExtraBody("{}");
      personal.setLlmMode("SYSTEM");personal.setLlm(null);configs.savePreferences(1L,personal);check(configs.enabledConfigForUser(1L,"LLM").modelName().equals("llm-two"),"system source ignores saved personal config");
      var merged=new LinkedHashMap<String,Object>();merged.put("max_tokens",10);merged.put("stream",true);org.springblade.modules.smartreminder.support.ModelBody.merge(merged,"{\"max_tokens\":null,\"thinking\":{\"type\":\"enabled\"},\"temperature\":0.7}");
      check(!merged.containsKey("max_tokens")&&merged.containsKey("thinking")&&Boolean.TRUE.equals(merged.get("stream")),"extra body supports nested JSON, overrides and null omission");
      rejects(()->org.springblade.modules.smartreminder.support.ModelBody.parse("[]"),"non-object extra body rejected");
      check(!configs.enabledConfigForUser(2L,"LLM").extraBody().contains("reasoning_effort"),"new model has no hardcoded reasoning parameter");
      var edit=request("LLM","llm-one",url);edit.setId(llm1);edit.setModelAlias("轻快助手");edit.setApiKey("");configs.save(edit);
      check(configs.choices(true).stream().anyMatch(x->"轻快助手".equals(x.get("modelAlias"))),"mobile choices include model alias");
      var selected=new ModelPreferenceRequest();selected.setLlmMode("SYSTEM");selected.setSpeechMode("SYSTEM");selected.setLanguage("zh-cn");selected.setLlmConfigId(llm1);configs.savePreferences(4L,selected);
      check(configs.enabledConfigForUser(4L,"LLM").id().equals(llm1),"system mode can select a non-default enabled model");
      edit.setEnabled(false);configs.save(edit);
      check(configs.enabledConfigForUser(4L,"LLM").id().equals(llm2),"disabled selected model falls back to default");
      var defaultEdit=request("LLM","llm-two",url);defaultEdit.setId(llm2);defaultEdit.setEnabled(false);rejects(()->configs.save(defaultEdit),"cannot disable default before choosing replacement");
      rejects(()->configs.setDefault("LLM",llm1),"disabled model cannot be system default");
      check(configs.prompts().get("intentPrompt").equals("global-intent"),"model edits preserve global prompts");
      var registry=new ChatRunRegistry();var run=registry.register(1L,"cancel-regression-001");registry.attach(run);var closed=new java.util.concurrent.atomic.AtomicBoolean();ChatRunRegistry.onCancel(()->closed.set(true));
      check(!registry.stop(2L,"cancel-regression-001"),"other user cannot stop this request");check(registry.stop(1L,"cancel-regression-001")&&closed.get(),"manual stop closes the upstream resource");
      try{ChatRunRegistry.beginActions();throw new AssertionError("cancelled run applied actions");}catch(java.util.concurrent.CancellationException expected){check(true,"cancelled run cannot execute event actions");}finally{registry.finish(run);}
      var applying=registry.register(1L,"applying-regression-01");registry.attach(applying);ChatRunRegistry.beginActions();check(!registry.stop(1L,"applying-regression-01"),"stop cannot falsely undo actions already applying");registry.finish(applying);
      check(!registry.stop(1L,"applying-regression-01"),"finished requests are released");
    }finally{server.stop(0);}
    System.out.println("ALL VOICE BACKEND REGRESSIONS PASSED");
  }
}
