package org.springblade.common.rpc.apiplatform;

import org.springblade.common.rpc.apiplatform.constant.ApiPlatformUrlConstant;
import org.springblade.common.rpc.apiplatform.util.ApiAuthUtil;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformHttp;
import org.springblade.common.rpc.apiplatform.util.ApiTokenUtil;
import org.springblade.core.tool.api.R;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;

@Component
public class ApiTest implements CommandLineRunner {



	@Override
	public void run(String... args) throws Exception {

//		R<Object> r = ApiPlatformHttp.commonGet(new HashMap<>(), ApiPlatformUrlConstant.DICT_URL);
//		System.out.println(r.toString());
//		Long t = System.currentTimeMillis();
//		String token = ApiTokenUtil.generateToken(new HashMap<String,Object>(){{
//			put("appKey","0j4tzxdgotlg");
//			put("timestamp",t);
//			put("randomStr",2);
//		}},"mafioegxuumnmuyesizk");
//		System.out.println(token);
//		long stimeA = System.currentTimeMillis();
//		Object ra = ApiPlatformHttp.commonGet(new HashMap<>(), ApiPlatformUrlConstant.DICT_URL);
//		long etimeA = System.currentTimeMillis();
//		// 计算执行时间
//		System.out.printf("执行时长：%d 毫秒.", (etimeA - stimeA));
//		System.out.println("\"token:\"+ a");
//		long stimeB = System.currentTimeMillis();
//		Object rb = ApiPlatformHttp.commonGet(new HashMap<>(), ApiPlatformUrlConstant.DICT_URL);
//		long etimeB = System.currentTimeMillis();
//		// 计算执行时间
//		System.out.printf("执行时长：%d 毫秒.", (etimeB - stimeB));
//		System.out.println("\"token:\"+ b");
//		long stimeC = System.currentTimeMillis();
//		Object rc = ApiPlatformHttp.commonGet(new HashMap<>(), ApiPlatformUrlConstant.DICT_URL);
//		long etimeC = System.currentTimeMillis();
//		// 计算执行时间
//		System.out.printf("执行时长：%d 毫秒.", (etimeC - stimeC));
//		System.out.println("\"token:\"+ c");
//		System.out.println();


//		for (int i = 0; i < 20; i++) {
//			long stimeA = System.currentTimeMillis();
//			Object ra = ApiPlatformHttp.commonGet(new HashMap<>(), ApiPlatformUrlConstant.DICT_URL);
//			long etimeA = System.currentTimeMillis();
//			System.out.println(i);
//			System.out.printf("    执行时长：%d 毫秒.", (etimeA - stimeA));
//
//		}
	}
}
