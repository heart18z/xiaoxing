package org.springblade.modules.smartreminder.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/** Extra provider parameters may replace defaults, but not application-owned messages/routing. */
public final class ModelBody {
  private static final ObjectMapper JSON=new ObjectMapper();
  private ModelBody(){}
  public static Map<String,Object> parse(String value){
    try{
      String text=value==null||value.isBlank()?"{}":value;
      if(text.length()>16000||!JSON.readTree(text).isObject())throw new IllegalArgumentException();
      Map<String,Object> result=JSON.readValue(text,new TypeReference<LinkedHashMap<String,Object>>(){});
      if(result.keySet().stream().anyMatch(Set.of("model","messages","stream","file")::contains))throw new IllegalArgumentException();
      return result;
    }catch(Exception e){throw new IllegalArgumentException("额外 body 必须是 JSON 对象，最大 16000 字符，不能覆盖 model、messages、stream");}
  }
  public static void merge(Map<String,Object> target,String json){parse(json).forEach((key,value)->{if(value==null)target.remove(key);else target.put(key,value);});}
}
