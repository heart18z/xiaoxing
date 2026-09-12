package org.springblade.modules.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserPlatformVO {
	private Long userId;
	private String userType;
	private String userTypeName;
	private String userExt;

	private List<UserPlatformVO> userPlatforms;
}
