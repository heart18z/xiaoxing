package org.springblade.modules.system.vo;

import lombok.Data;
import org.springblade.modules.system.entity.User;

import java.util.List;

@Data
public class ContactUserVO {
	/**
	 * 系统管理员
	 */
	private List<User> adminUser;
	/**
	 * 技术支持
	 */
	private  List<User> tecUser;
	/**
	 * 功能需求
	 */
	private  List<User> funcUser;


}
