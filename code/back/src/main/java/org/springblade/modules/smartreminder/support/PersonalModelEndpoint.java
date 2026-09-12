package org.springblade.modules.smartreminder.support;

import java.net.*;

/** Ordinary users cannot turn a personal model proxy into a request to internal services. */
public final class PersonalModelEndpoint {
  private PersonalModelEndpoint(){}
  public static void validate(String url){
    try{
      URI uri=URI.create(url);
      if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)throw new IllegalArgumentException();
      for(InetAddress address:InetAddress.getAllByName(uri.getHost())){
        byte[] bytes=address.getAddress();int first=bytes[0]&255;
        if(address.isAnyLocalAddress()||address.isLoopbackAddress()||address.isLinkLocalAddress()||address.isSiteLocalAddress()||address.isMulticastAddress()
          ||(bytes.length==16&&(first&254)==252)||(bytes.length==4&&(first==0||first>=224||(first==100&&(bytes[1]&255)>=64&&(bytes[1]&255)<=127))))throw new IllegalArgumentException();
      }
    }catch(Exception e){throw new IllegalArgumentException("个人模型地址必须是可解析的公网 HTTPS 地址，不支持本机、内网或带凭据的地址");}
  }
}
