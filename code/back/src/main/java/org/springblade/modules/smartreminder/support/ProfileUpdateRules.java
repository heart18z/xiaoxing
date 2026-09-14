package org.springblade.modules.smartreminder.support;

import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.smartreminder.dto.SmartReminderDtos.ProfileUpdateRequest;

/** Null contact fields preserve legacy clients; empty strings intentionally clear optional fields. */
public final class ProfileUpdateRules {
    private ProfileUpdateRules() {}
    public static void validate(ProfileUpdateRequest input) {
        if (input == null) throw new ServiceException("请输入个人资料");
        if (input.getName() != null) {
            input.setName(input.getName().trim());
            if (input.getName().isBlank() || input.getName().length() > 20)
                throw new ServiceException("请输入姓名，最多20个字符");
        }
        if (input.getPhone() != null) {
            input.setPhone(input.getPhone().trim());
            if (!input.getPhone().isEmpty() && !input.getPhone().matches("1[3-9][0-9]{9}"))
                throw new ServiceException("请输入正确的11位手机号");
        }
        if (input.getEmail() != null) {
            input.setEmail(input.getEmail().trim());
            if (input.getEmail().length() > 45 || (!input.getEmail().isEmpty()
                && !input.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))
                throw new ServiceException("请输入正确的邮箱，最多45个字符");
        }
    }
}
