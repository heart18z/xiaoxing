import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springblade.modules.smartreminder.support.AlarmPlanning;
import org.springblade.modules.smartreminder.support.DashScopeSpeech;
import java.time.LocalDateTime;
import java.util.Map;
import org.springblade.modules.smartreminder.service.AiConfigService;
import org.springblade.modules.smartreminder.service.SpeechService;
import org.springframework.web.multipart.MultipartFile;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import java.math.BigDecimal;

public class LanguageAlarmSpeech137Regression {
    static int checks;
    static void check(boolean value) { if(!value)throw new AssertionError("check "+checks); checks++; }
    public static void main(String[] args) throws Exception {
        var mapper=new ObjectMapper();
        var now=LocalDateTime.of(2026,10,15,9,0);
        ObjectNode meeting=mapper.createObjectNode().put("summary","下午三点开会").put("eventTime","2026-10-15 15:00:00").put("alarmRequested",true);
        AlarmPlanning.normalize(meeting,now);
        check(meeting.path("alarmTime").asText().equals("2026-10-15 14:30:00"));
        check(meeting.path("eventTime").asText().equals("2026-10-15 15:00:00"));
        meeting.put("alarmExactTime",true);AlarmPlanning.normalize(meeting,now);
        check(meeting.path("alarmTime").asText().equals("2026-10-15 15:00:00"));
        var deadline=mapper.createObjectNode().put("summary","提交报告").put("deadlineTime","2026-10-15 17:00:00").put("alarmRequested",true);
        AlarmPlanning.normalize(deadline,now);check(deadline.path("alarmTime").asText().equals("2026-10-15 16:00:00"));
        var missing=mapper.createObjectNode().put("alarmRequested",true).put("firstEvaluateTime","2026-10-15 15:00:00");
        AlarmPlanning.normalize(missing,now);check(!missing.has("alarmTime"));
        var near=mapper.createObjectNode().put("summary","航班").put("eventTime","2026-10-15 09:05:00").put("alarmRequested",true);
        AlarmPlanning.normalize(near,now);check(AlarmPlanning.time(near.path("alarmTime").asText()).isAfter(now));
        var old=meeting.deepCopy().put("eventTime","2026-10-14 15:00:00");AlarmPlanning.normalize(old,now);check(!old.has("alarmTime"));
        for(String body:new String[]{"{\"output\":{\"text\":\"hello\"}}","{\"output\":{\"output\":{\"sentence\":{\"text\":\"hello\"}}}}"})check(DashScopeSpeech.text(mapper.readTree(body)).equals("hello"));
        var request=DashScopeSpeech.body("fun-asr-flash",new byte[]{1,2,3},"mp3",Map.of("parameters",Map.of("sample_rate",16000)));
        var json=mapper.valueToTree(request);check(json.path("parameters").path("format").asText().equals("mp3"));check(!json.path("parameters").has("sample_rate"));
        check(json.path("input").path("messages").get(0).path("content").get(0).path("input_audio").path("data").asText().equals("data:audio/mpeg;base64,AQID"));
        check(DashScopeSpeech.matches("https://example.invalid"+DashScopeSpeech.ENDPOINT));
        check(!DashScopeSpeech.matches("https://example.invalid/v1/audio/transcriptions"));
        try { DashScopeSpeech.body("x",new byte[7*1024*1024+1],"wav",Map.of());throw new AssertionError(); } catch(IllegalArgumentException expected) {checks++;}
        var configuration=new AiConfigService(null,null);
        var normalize=AiConfigService.class.getDeclaredMethod("normalizeUrl",String.class,String.class);normalize.setAccessible(true);
        check(normalize.invoke(configuration,"https://example.invalid"+DashScopeSpeech.ENDPOINT,"SPEECH").equals("https://example.invalid"+DashScopeSpeech.ENDPOINT));
        check(normalize.invoke(configuration,"https://example.invalid/v1","SPEECH").equals("https://example.invalid/v1/audio/transcriptions"));
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var response=new AtomicReference<>("{\"output\":{\"text\":\"明天开会\"}}");
        var input=new AtomicReference<String>();var contentType=new AtomicReference<String>();
        server.createContext("/",exchange->{
            input.set(new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] bytes=response.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        try {
            var endpoint=new AtomicReference<>("http://127.0.0.1:"+server.getAddress().getPort()+DashScopeSpeech.ENDPOINT);
            var configs=new AiConfigService(null,null) {
                @Override public AiRuntimeConfig enabledConfigForUser(Long id,String type) {
                    return new AiRuntimeConfig(1L,"fixture",endpoint.get(),"fixture-key","fun-asr-flash",100,100,100,BigDecimal.ZERO,"",false,5000,"","",true,"{}");
                }
            };
            MultipartFile file=new MultipartFile() {
                public String getName(){return "file";} public String getOriginalFilename(){return "recording.mp3";}
                public String getContentType(){return "audio/mpeg";} public boolean isEmpty(){return false;} public long getSize(){return 64;}
                public byte[] getBytes(){return new byte[64];} public java.io.InputStream getInputStream(){return new java.io.ByteArrayInputStream(getBytes());}
                public void transferTo(java.io.File dest) throws java.io.IOException {java.nio.file.Files.write(dest.toPath(),getBytes());}
            };
            var speech=new SpeechService(configs,mapper);
            check(speech.transcribe(1L,file).get("text").equals("明天开会"));
            check(contentType.get().startsWith("application/json"));
            check(mapper.readTree(input.get()).path("parameters").path("format").asText().equals("mp3"));
            response.set("{\"output\":{\"output\":{\"sentence\":{\"text\":\"hello\"}}}}");
            check(speech.transcribe(1L,file).get("text").equals("hello"));
            response.set("{\"code\":\"InvalidApiKey\"}");
            try {speech.transcribe(1L,file);throw new AssertionError();}catch(org.springblade.core.log.exception.ServiceException expected){checks++;}
            response.set("{\"text\":\"legacy\"}");endpoint.set("http://127.0.0.1:"+server.getAddress().getPort()+"/audio/transcriptions");
            check(speech.transcribe(1L,file).get("text").equals("legacy"));
            check(contentType.get().startsWith("multipart/form-data"));
        } finally {server.stop(0);}
        System.out.println("PASS: "+checks+" alarm timing and speech protocol checks");
    }
}
