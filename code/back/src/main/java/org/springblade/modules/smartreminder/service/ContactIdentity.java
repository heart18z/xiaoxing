package org.springblade.modules.smartreminder.service;
import org.springblade.core.log.exception.ServiceException;
import java.util.Locale;
import java.util.Objects;
public final class ContactIdentity {
    private ContactIdentity() {}
    public static String email(String value) {
        String email=Objects.toString(value,"").trim().toLowerCase(Locale.ROOT);
        if(email.length()>45 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || email.matches(".*[<>;,\\r\\n].*"))throw new ServiceException("请输入正确的邮箱，最多45个字符");
        return email;
    }
    public static String phone(String value) {
        String phone=Objects.toString(value,"").trim();
        if(!phone.isEmpty() && !phone.matches("1[3-9][0-9]{9}"))throw new ServiceException("请输入正确的11位手机号");
        return phone;
    }
}
