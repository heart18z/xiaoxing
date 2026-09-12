package org.springblade.modules.auth.provider;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springblade.modules.auth.enums.UserEnum;

import java.util.Arrays;

/**
 * OAuth2 用户类型
 */
@Getter
@AllArgsConstructor
public enum UserType {

	WEB("web", 1),
	APP("app", 2),
	OTHER("other", 3);

	final String name;
	final int category;

	public static UserType of(String name) {
		return Arrays.stream(UserType.values())
			.filter(userType -> userType.getName().equalsIgnoreCase(name != null ? name : "web"))
			.findFirst()
			.orElse(UserType.WEB);
	}

	public UserEnum toUserEnum() {
		return switch (this) {
			case APP -> UserEnum.APP;
			case OTHER -> UserEnum.OTHER;
			default -> UserEnum.WEB;
		};
	}
}
