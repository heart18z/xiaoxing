package org.springblade.modules.system.controller;

import lombok.AllArgsConstructor;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.service.IParamService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("blade-system/param/")
@AllArgsConstructor
public class FaviconController {
	private final IParamService paramService;
	private final String DEF_ICON= "favicon.png";
	@RequestMapping(value="/favicon" , method = RequestMethod.GET)
	public String favicon() throws Exception {
		return "redirect:"+Func.toStr(paramService.getValue("web.icon"),DEF_ICON);
	}

}
