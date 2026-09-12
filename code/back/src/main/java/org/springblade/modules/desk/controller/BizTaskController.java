package org.springblade.modules.desk.controller;


import lombok.AllArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.quartz.enums.BizTaskEnum;
import org.springblade.modules.system.entity.DictBiz;
import org.springblade.modules.system.service.IDictBizService;
import org.springblade.modules.system.vo.DictBizVO;
import org.springblade.modules.system.wrapper.DictBizWrapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("blade-task/task")
public class BizTaskController {

//	@PostMapping("/code-name-list")
//	public R<List<KeyValueVO>> codeNameList() {
//		List<KeyValueVO> kvList = Arrays.stream(BizTaskEnum.values()).map(i->
//			new KeyValueVO(i.getName(),i.getCode())
//		).collect(Collectors.toList());
//		return R.data(kvList);
//	}
//
	private final IDictBizService dictBizService;


	@PostMapping("/quartzTaskName-name-list")
	public R<List<DictBizVO>> quartzTaskNameNameList() {
		Map<String,String> map = Arrays.stream(BizTaskEnum.values())
			.filter(i-> Func.isNotBlank(i.getCode())&& Func.isNotBlank(i.getQuartzTaskName()))
			.collect(Collectors.toMap(BizTaskEnum::getCode,BizTaskEnum::getQuartzTaskName,(v1,v2)->v1));

		List<DictBiz> tree = dictBizService.getList("task_type").stream()
			.peek(i->{
				i.setDictKey(map.get(i.getDictKey()));
				if (Func.isBlank(i.getDictKey())){
					i.setDictKey("undefined_value");
				}
			})
			.collect(Collectors.toList());

		return R.data(DictBizWrapper.build().listNodeVO(tree));
	}
}
