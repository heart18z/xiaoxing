package org.springblade.modules.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Oauth2SettingEnum {

	UNSET("尚未配置", "unset"),
	DISABLE("尚未启用", "disable"),
	ENABLE("已经启用", "enable");

	final String name;
	final String value;
}
