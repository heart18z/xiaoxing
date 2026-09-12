/*
 *      Copyright (c) 2018-2028, Chill Zhuang All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the dreamlu.net developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: Chill 庄骞 (smallchill@163.com)
 */
package org.springblade.common.utils;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.constant.ParamCacheConstant;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.Func;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 通用工具类
 *
 * @author Chill
 */
public class CommonUtil {

 	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
		Map<Object, Boolean> seen = new ConcurrentHashMap<>();
		return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

	public static String getBodyString(ServletRequest request)
	{
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try (InputStream inputStream = request.getInputStream())
		{
			reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			String line = "";
			while ((line = reader.readLine()) != null)
			{
				sb.append(line);
			}
		}
		catch (IOException e)
		{
			//LOGGER.warn("getBodyString出现问题！");
		}
		finally
		{
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (IOException e)
				{
					//LOGGER.error(ExceptionUtils.getMessage(e));
				}
			}
		}
		return sb.toString();
	}

	public static void checkFileType(MultipartFile file) {
		String fileName = file.getOriginalFilename();
		if (Func.isBlank(fileName)) {throw new ServiceException("文件名称不能为空！");}
		String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
		String blackList= ParamCache.getValue(ParamCacheConstant.FILE_TYPE_BLACK_LIST);
		if (Func.isBlank(blackList)) {return;}
		String[] NOT_ALLOWED_FILE_EXTENSION = blackList.split(",");
		if (Arrays.asList(NOT_ALLOWED_FILE_EXTENSION).contains(fileExtension)) {
			throw new ServiceException("不允许的文件类型："+fileExtension);
		}


	}

	public static  String createUniqueCode(int size) {
		String chars = "abcdefghijklmnopqrstuvwxyz";
		StringBuffer value = new StringBuffer();
		for (int i = 0; i < size; i++) {
			value.append(chars.charAt((int)(Math.random() * 26)));
		}
		return value.toString();
	}

}
