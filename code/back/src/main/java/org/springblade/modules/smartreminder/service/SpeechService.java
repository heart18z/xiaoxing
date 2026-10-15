package org.springblade.modules.smartreminder.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springblade.modules.smartreminder.support.DashScopeSpeech;
import org.springblade.modules.smartreminder.support.ModelBody;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Audio is forwarded once, never persisted or inserted into the conversation. */
@Service
@RequiredArgsConstructor
public class SpeechService {
  private final AiConfigService configs;
  private final ObjectMapper mapper;
  private final Set<Long> inFlight=ConcurrentHashMap.newKeySet();
  public Map<String,Object> transcribe(Long userId,MultipartFile file) {
    if(file==null||file.isEmpty()||file.getSize()>20L*1024*1024)throw new ServiceException("请选择 20MB 以内的音频文件");
    String name=file.getOriginalFilename()==null?"":file.getOriginalFilename().toLowerCase(Locale.ROOT);
    String extension=name.substring(name.lastIndexOf('.')+1);
    if(!Set.of("wav","mp3","mp4","m4a","webm","ogg","flac","aac").contains(extension))throw new ServiceException("不支持的音频格式，请使用 WAV、MP3 或 M4A 等音频文件");
    if(!inFlight.add(userId))throw new ServiceException("上一段语音正在识别，请稍后再试");
    try {
      var config=configs.enabledConfigForUser(userId,"SPEECH");
      byte[] bytes=file.getBytes();
      if(bytes.length<32)throw new ServiceException("录音内容太短，请重新录制");
      HttpRequest request=HttpRequest.post(config.baseUrl()).setFollowRedirects(false).header("Authorization","Bearer "+config.apiKey());
      boolean dashscope=DashScopeSpeech.matches(config.baseUrl());
      var extra=ModelBody.parse(config.extraBody());
      if(dashscope) {
        request.header("Content-Type","application/json").header("X-DashScope-SSE","disable")
          .body(mapper.writeValueAsString(DashScopeSpeech.body(config.modelName(),bytes,extension,extra)));
      } else {
        for(var field:extra.entrySet()) {
          if(field.getValue()!=null)request.form(field.getKey(),field.getValue() instanceof String?(String)field.getValue():mapper.writeValueAsString(field.getValue()));
        }
        request.form("model",config.modelName()).form("file",bytes,"recording."+extension);
      }
      try(HttpResponse response=request.timeout(Math.min(120000,Math.max(5000,config.requestTimeout()))).execute()) {
        if(response.getStatus()<200||response.getStatus()>=300)throw new ServiceException("语音识别失败（HTTP "+response.getStatus()+"），请重试或联系管理员检查语音模型");
        var root=mapper.readTree(response.body());
        String text=dashscope?DashScopeSpeech.text(root):root.path("text").asText("").trim();
        if(text.isEmpty())throw new ServiceException("语音服务未返回识别文字；如录音有声音，请联系管理员检查语音模型或服务通道");
        if(text.length()>20000)throw new ServiceException("识别内容过长，请分段录制");
        return Map.of("text",text,"model",config.modelName());
      }
    }catch(ServiceException e){throw e;}
    catch(IllegalArgumentException e){throw new ServiceException(e.getMessage());}
    catch(IllegalStateException e){throw new ServiceException("尚未配置可用的语音模型，请联系管理员");}
    catch(Exception e){throw new ServiceException("语音识别暂时不可用，请稍后重试");}
    finally{inFlight.remove(userId);}
  }
}
