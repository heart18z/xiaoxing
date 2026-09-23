package org.springblade.modules.smartreminder.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.smartreminder.support.SecretCodec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Database-backed SMTP settings, read on every send; credentials never leave the server. */
@Service
@RequiredArgsConstructor
public class RegistrationMailService {
    private final JdbcTemplate jdbc;
    private final SecretCodec secrets;

    @Data public static class Settings {
        private String host = "mail.sinoaopt.com";
        private int port = 587;
        private String security = "STARTTLS";
        private String username = "fmemo@sinoaopt.com";
        @ToString.Exclude private String password;
        private String fromAddress = "fmemo@sinoaopt.com";
        private String fromName = "AI小醒";
        private boolean enabled;
    }
    public Map<String,Object> detail() {
        var rows=jdbc.queryForList("select host,port,security,username,from_address,from_name,enabled,password_cipher from blade_registration_mail where id=1");
        var result=new LinkedHashMap<String,Object>();
        Settings s=new Settings();
        if(!rows.isEmpty()) {
            var r=rows.get(0);s.setHost((String)r.get("host"));s.setPort(((Number)r.get("port")).intValue());
            s.setSecurity((String)r.get("security"));s.setUsername((String)r.get("username"));
            s.setFromAddress((String)r.get("from_address"));s.setFromName((String)r.get("from_name"));s.setEnabled(((Number)r.get("enabled")).intValue()==1);
            result.put("passwordConfigured", !Objects.toString(r.get("password_cipher"),"").isBlank());
        } else result.put("passwordConfigured",false);
        result.put("host",s.getHost());result.put("port",s.getPort());result.put("security",s.getSecurity());
        result.put("username",s.getUsername());result.put("fromAddress",s.getFromAddress());result.put("fromName",s.getFromName());result.put("enabled",s.isEnabled());
        return result;
    }
    @Transactional(rollbackFor=Exception.class)
    public void save(Settings input) {
        if(input==null)throw new ServiceException("请填写邮件配置");
        String host=Objects.toString(input.getHost(),"").trim();
        if(host.length()>253 || !host.matches("[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?") || input.getPort()<1 || input.getPort()>65535)
            throw new ServiceException("请填写 SMTP 主机名和有效端口，不要填写 https:// 或路径");
        if(!Set.of("STARTTLS","SSL").contains(Objects.toString(input.getSecurity(),"")))throw new ServiceException("请选择 STARTTLS 或 SSL/TLS");
        String username=ContactIdentity.email(input.getUsername()),from=ContactIdentity.email(input.getFromAddress());
        String name=Objects.toString(input.getFromName(),"").trim();
        if(name.isBlank() || name.length()>60 || name.contains("\r") || name.contains("\n"))throw new ServiceException("发件人名称须为1–60个字符");
        if(input.getPassword()!=null && (input.getPassword().length()>512 || input.getPassword().startsWith("v1:")))throw new ServiceException("邮件密码格式不正确");
        var old=jdbc.queryForList("select password_cipher from blade_registration_mail where id=1 for update",String.class);
        String cipher=input.getPassword()==null || input.getPassword().isBlank() ? (old.isEmpty()?"":old.get(0)) : secrets.encrypt(input.getPassword());
        if(input.isEnabled() && cipher.isBlank())throw new ServiceException("启用前请填写 SMTP 密码");
        jdbc.update("insert into blade_registration_mail(id,host,port,security,username,password_cipher,from_address,from_name,enabled) values(1,?,?,?,?,?,?,?,?) on duplicate key update host=values(host),port=values(port),security=values(security),username=values(username),password_cipher=values(password_cipher),from_address=values(from_address),from_name=values(from_name),enabled=values(enabled)",host,input.getPort(),input.getSecurity(),username,cipher,from,name,input.isEnabled()?1:0);
    }
    public void requireEnabled() {
        if(jdbc.queryForObject("select count(*) from blade_registration_mail where id=1 and enabled=1",Integer.class)!=1)
            throw new ServiceException("邮箱验证服务尚未启用，请联系管理员");
    }
    public void sendCode(String email,String code) {
        send(email,"AI小醒注册验证码","你的注册验证码是："+code+"\n\n验证码10分钟内有效，仅用于本邮箱注册。请勿向他人透露。\n如非本人操作，请忽略此邮件。",true);
    }
    public void test(String recipient) { send(ContactIdentity.email(recipient),"AI小醒 SMTP 测试","这是一封 SMTP 配置测试邮件，不含注册验证码。收到此邮件表示发信链路正常。",false); }
    protected void send(String recipient,String subject,String body,boolean requireEnabled) {
        var rows=jdbc.queryForList("select * from blade_registration_mail where id=1");
        if(rows.isEmpty())throw new ServiceException("请先保存 SMTP 配置");
        var r=rows.get(0);
        if(requireEnabled && ((Number)r.get("enabled")).intValue()!=1)throw new ServiceException("邮箱验证服务尚未启用，请联系管理员");
        String password=secrets.decrypt((String)r.get("password_cipher"));
        if(password.isBlank())throw new ServiceException("请先配置 SMTP 密码");
        Properties props=new Properties();
        props.setProperty("mail.smtp.auth","true");props.setProperty("mail.smtp.ssl.checkserveridentity","true");
        props.setProperty("mail.smtp.connectiontimeout","8000");props.setProperty("mail.smtp.timeout","10000");props.setProperty("mail.smtp.writetimeout","10000");
        props.setProperty("mail.smtp.ssl.enable",String.valueOf("SSL".equals(r.get("security"))));
        props.setProperty("mail.smtp.starttls.enable",String.valueOf("STARTTLS".equals(r.get("security"))));
        props.setProperty("mail.smtp.starttls.required",String.valueOf("STARTTLS".equals(r.get("security"))));
        Session session=Session.getInstance(props);session.setDebug(false);
        try(Transport transport=session.getTransport("smtp")) {
            MimeMessage message=new MimeMessage(session);
            message.setFrom(new InternetAddress((String)r.get("from_address"),(String)r.get("from_name"),"UTF-8"));
            message.setRecipient(Message.RecipientType.TO,new InternetAddress(recipient,true));
            message.setSubject(subject,"UTF-8");message.setText(body,"UTF-8");message.setSentDate(new Date());message.saveChanges();
            transport.connect((String)r.get("host"),((Number)r.get("port")).intValue(),(String)r.get("username"),password);
            transport.sendMessage(message,message.getAllRecipients());
        } catch(AuthenticationFailedException error) { throw new ServiceException("邮件服务器认证失败，请联系管理员检查发信账号和密码"); }
        catch(Exception error) { throw new ServiceException("邮件暂未发送成功，请稍后重试或联系管理员检查 SMTP 连接、证书及发信权限"); }
    }
}
