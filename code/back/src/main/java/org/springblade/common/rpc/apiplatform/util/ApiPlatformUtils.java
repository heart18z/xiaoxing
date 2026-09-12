package org.springblade.common.rpc.apiplatform.util;

import com.fasterxml.jackson.databind.JsonNode;

public class ApiPlatformUtils {

	private static String R_DATA_NODE = "data";

	/**
	 * 获取jsonnode的数据
	 * @param jsonNode
	 * @return
	 */
	public static String getDataFromJsonNode(JsonNode jsonNode){
		if (jsonNode ==null) {return null;}
		Object o = jsonNode.get(R_DATA_NODE);
		if (o == null) {
			return null;
		}
		return o.toString();
	}
}
