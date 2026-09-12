package org.springblade.common.rpc.apiplatform.util;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.common.rpc.apiplatform.config.ApiPlatformConfig;
import org.springblade.common.rpc.apiplatform.interceptor.TokenRetryInterceptor;
import org.springblade.core.http.HttpRequest;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.SpringUtil;
import org.springblade.modules.system.entity.InteractiveEntity;
import org.springblade.modules.system.service.IInteractiveService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiPlatformHttp {
	private static final ApiPlatformConfig apiPlatformConfig;
	private static final IInteractiveService interactiveService;

	static {
		apiPlatformConfig = SpringUtil.getBean(ApiPlatformConfig.class);
		interactiveService = SpringUtil.getBean(IInteractiveService.class);
	}

	private static final Logger log = LoggerFactory.getLogger(ApiPlatformHttp.class);
	/**
	 * get请求
	 * @param params 请求参数
	 * @param path 请求地址
	 * @return
	 */
	public static R<Object> commonGet(Map<String,Object> params, String path) {
		try {
			//拼接url
			String url = apiPlatformConfig.getBaseUrl()+path;
			String res = HttpRequest.get(url)
				.interceptor(new TokenRetryInterceptor())
				.queryMap(params)
				.execute()
				.onSuccess(responseSpec -> responseSpec.asString());
			//简单解析返回结果
			R<Object> resData = JSON.parseObject(res,R.class);
			return resData;

		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return new R<Object>();
		}
	}

	/**
	 * get请求返回JsonNode类型
	 * @param params 请求参数
	 * @param path 请求地址
	 * @return
	 */
	public static JsonNode commonGetStr(Map<String,Object> params, String path) {
		try {
			//拼接url
			String url = apiPlatformConfig.getBaseUrl()+path;
			return HttpRequest.get(url)
				.interceptor(new TokenRetryInterceptor())
				.queryMap(params)
				.execute()
				.onSuccess(responseSpec -> responseSpec.asJsonNode());
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return null;
		}
	}

	/**
	 * post请求
	 * @param json 请求参数
	 * @param path 请求地址
	 * @return
	 */
	public static R<Object> commonPost(Object json, String path) {
		try {
			//拼接url
			String url = apiPlatformConfig.getBaseUrl()+path;
			String res = HttpRequest.post(url)
				.interceptor(new TokenRetryInterceptor())
				.bodyJson(json)
				.execute()
				.onSuccess(responseSpec -> responseSpec.asString());
			//简单解析返回结果
            return JSON.parseObject(res,R.class);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return new R<Object>();
		}
	}

	public static JsonNode commonPostStr(Object json, String path) {
		try {
			//拼接url
			String url = apiPlatformConfig.getBaseUrl()+path;
			return HttpRequest.post(url)
				.interceptor(new TokenRetryInterceptor())
				.bodyJson(json)
				.execute()
				.onSuccess(responseSpec -> responseSpec.asJsonNode());
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return null;
		}
	}

	/**
	 * 根据interactiveCode来获取远程调用的地址，如果interactiveCode为空或者一下情况，则使用第三个参数path作为远程调用地址
	 * @param params
	 * @param interactiveCode
	 * @param path
	 * @return
	 */
	public static JsonNode commonGetByInteractive(Map<String,Object> params,String interactiveCode,String path) {
		try {
			String url = "";
			if (Func.isNotBlank(interactiveCode)) {
				InteractiveEntity interactiveEntity = interactiveService.getOne(
					new LambdaQueryWrapper<InteractiveEntity>()
						.select(InteractiveEntity::getAddress)
						.eq(InteractiveEntity::getInteractiveCode,interactiveCode)
						.last(" limit 1")
				);
				if (interactiveEntity!=null&&Func.isNotBlank(interactiveEntity.getAddress())
					&& !interactiveEntity.getAddress().startsWith("_")
				) {
					url = interactiveEntity.getAddress();
				}
			}

			if (Func.isBlank(url)) {
				 url = apiPlatformConfig.getBaseUrl()+path;
			}

			return HttpRequest.get(url)
				.interceptor(new TokenRetryInterceptor())
				.queryMap(params)
				.execute()
				.onSuccess(responseSpec -> responseSpec.asJsonNode());
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return null;
		}
	}


	public static R<Object> commonPostByInteractive(Object json, String interactiveCode, String path) {
		try {

			String url = "";
			if (Func.isNotBlank(interactiveCode)) {
				InteractiveEntity interactiveEntity = interactiveService.getOne(
					new LambdaQueryWrapper<InteractiveEntity>()
						.select(InteractiveEntity::getAddress)
						.eq(InteractiveEntity::getInteractiveCode,interactiveCode)
						.last(" limit 1")
				);
				if (interactiveEntity!=null&&Func.isNotBlank(interactiveEntity.getAddress())
					&& !interactiveEntity.getAddress().startsWith("_")
				) {
					url = interactiveEntity.getAddress();
				}
			}

			if (Func.isBlank(url)) {
				url = apiPlatformConfig.getBaseUrl()+path;
			}

			String res = HttpRequest.post(url)
				.interceptor(new TokenRetryInterceptor())
				.bodyJson(json)
				.execute()
				.onSuccess(responseSpec -> responseSpec.asString());
			//简单解析返回结果
			return JSON.parseObject(res,R.class);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return new R<Object>();
		}
	}


	public static JsonNode commonPostStrByInteractive(Object json,String interactiveCode, String path) {
		try {

			String url = "";
			if (Func.isNotBlank(interactiveCode)) {
				InteractiveEntity interactiveEntity = interactiveService.getOne(
					new LambdaQueryWrapper<InteractiveEntity>()
						.select(InteractiveEntity::getAddress)
						.eq(InteractiveEntity::getInteractiveCode,interactiveCode)
						.last(" limit 1")
				);
				if (interactiveEntity!=null&&Func.isNotBlank(interactiveEntity.getAddress())
					&& !interactiveEntity.getAddress().startsWith("_")
				) {
					url = interactiveEntity.getAddress();
				}
			}

			if (Func.isBlank(url)) {
				url = apiPlatformConfig.getBaseUrl()+path;
			}


			return HttpRequest.post(url)
				.interceptor(new TokenRetryInterceptor())
				.bodyJson(json)
				.execute()
				.onSuccess(responseSpec -> responseSpec.asJsonNode());
		} catch (Exception e) {
			log.error(e.getLocalizedMessage());
			return null;
		}
	}
}
