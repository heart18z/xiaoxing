package org.springblade.modules.auth.endpoint;

import lombok.AllArgsConstructor;
import org.springblade.common.secure.utils.SysApiUtil;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth-test")
@AllArgsConstructor
public class TestAuth {

@PostMapping("/test")
	public String test(){
		return "123";
	}
}
