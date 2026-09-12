import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springblade.modules.smartreminder.service.*;
import java.net.*;import java.nio.charset.StandardCharsets;import java.math.BigDecimal;import java.util.concurrent.*;

/** Real local HTTP streams, with no external providers or user records. */
public class ChatCancellationRegression {
  public static void main(String[] args)throws Exception{
    var executor=Executors.newCachedThreadPool();var mapper=new ObjectMapper();
    try{
      for(boolean headers:new boolean[]{false,true}){
        var arrived=new CountDownLatch(1);var release=new CountDownLatch(1);var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(executor);
        server.createContext("/v1/chat/completions",exchange->{
          try{var json=mapper.readTree(exchange.getRequestBody().readAllBytes());if(json.has("reasoning_effort")||json.has("max_tokens")||!json.path("enable_thinking").asBoolean())throw new AssertionError("extra body not merged");
            if(headers){exchange.getResponseHeaders().set("Content-Type","text/event-stream");exchange.sendResponseHeaders(200,0);exchange.getResponseBody().write("data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"thinking\"}}]}\n\n".getBytes(StandardCharsets.UTF_8));exchange.getResponseBody().flush();}
            arrived.countDown();release.await(5,TimeUnit.SECONDS);
          }catch(InterruptedException e){Thread.currentThread().interrupt();}finally{exchange.close();}
        });server.start();
        var registry=new ChatRunRegistry();String id="real-http-cancel-"+headers;var run=registry.register(1L,id);
        var config=new AiConfigService.AiRuntimeConfig(1L,"test","http://127.0.0.1:"+server.getAddress().getPort()+"/v1/chat/completions","local-only","test",4096,4096,100,BigDecimal.ZERO,"xhigh",true,10000,"","",true,"{\"enable_thinking\":true,\"max_tokens\":null}");
        var task=executor.submit(()->{try{registry.attach(run);new NewApiClient(null,mapper).chatStream(config,"system","user",x->{},x->{});throw new AssertionError("cancel ignored");}catch(CancellationException expected){return true;}finally{registry.finish(run);}});
        try{if(!arrived.await(4,TimeUnit.SECONDS))throw new AssertionError("request did not arrive");Thread.sleep(150);if(!registry.stop(1L,id)||!task.get(3,TimeUnit.SECONDS))throw new AssertionError("stop failed");System.out.println("PASS upstream stopped "+(headers?"during stream":"before headers")+"; extra body verified");}
        finally{release.countDown();server.stop(0);}
      }
    }finally{executor.shutdownNow();}
    System.out.println("ALL HTTP CANCELLATION REGRESSIONS PASSED");
  }
}
