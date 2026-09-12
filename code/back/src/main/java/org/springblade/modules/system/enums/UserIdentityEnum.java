package org.springblade.modules.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springblade.core.tool.utils.Func;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum UserIdentityEnum {


	USER_IDENTITY_FUNC_MAIN("主导_功能需求提出人", "main_f"),
	USER_IDENTITY_FUNC("配合_功能需求提出人", "func"),
	USER_IDENTITY_TEC_MAIN("主导_技术支持负责人", "main_t"),
	USER_IDENTITY_TEC("配合_技术支持负责人", "tec"),
	USER_IDENTITY_ADMIN_MAIN("主导_系统应用负责人", "main_a"),
	USER_IDENTITY_ADMIN("配合_系统应用负责人", "admin"),
	USER_IDENTITY_CUSTOMER("常规_日常系统使用人","customer");


	final String name;
	final String value;

	private final static List<String> mainIdentities=Stream.of(USER_IDENTITY_ADMIN_MAIN.value, USER_IDENTITY_FUNC_MAIN.value, USER_IDENTITY_TEC_MAIN.value).collect(Collectors.toList());

	public static List<String> getMainIdentities() {
		return  mainIdentities;
	}

	public static String getMainPrefix() {
		return "main_";
	}

	public static String getNameByValue(String value) {
		for(UserIdentityEnum userIdentityEnum: UserIdentityEnum.values()) {
			if (userIdentityEnum.getValue().equals(value)) {
				return userIdentityEnum.getName();
			}
		}
		return null;
	}

	public static String getNameByValueFilterMains(String values) {
		if (Func.isBlank(values)) {
			return null;
		}
		String[] valueList = values.split(",");
		if (Func.isEmpty(valueList)) {
			return null;
		}
		Map<String,String> map = Arrays.stream(UserIdentityEnum.values()).filter(item->mainIdentities.contains(item.getValue()))
			.collect(Collectors.toMap(UserIdentityEnum::getValue,UserIdentityEnum::getName));
		List<String> nameList= new ArrayList<>();
		for (String s:valueList) {
			String name = map.get(s);
			if (Func.isNotBlank(name)) {
				nameList.add(name);
			}
		}
		if (nameList.size() ==0) {
			return null;
		}
		return Func.join(nameList);
	}
}
