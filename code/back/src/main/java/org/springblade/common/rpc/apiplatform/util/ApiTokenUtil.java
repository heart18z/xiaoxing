package org.springblade.common.rpc.apiplatform.util;

import org.springblade.core.tool.utils.DigestUtil;
import org.springblade.core.tool.utils.Func;

import java.util.*;

public class ApiTokenUtil {

	public static String generateToken(Map map, String appSecret) {
		if(Func.isEmpty(map) || Func.isEmpty(appSecret)) return "";
		String result = getJoinStr(map);
		if (Func.isBlank(result)) {
			return "";
		}
		String mdStr = DigestUtil.md5Hex(result + appSecret);
		return base64EncodeNoPadding(mdStr).toUpperCase();
	}


	private static String getJoinStr(Map map) {
		String result = "";
		try {
			List<Map.Entry<String, String>> infoIds = new ArrayList<>(map.entrySet());
			// 对所有传入参数按照字段名的 ASCII 码从小到大排序（字典序）
			Collections.sort(infoIds, Comparator.comparing(Map.Entry::getKey));
			// 构造签名键值对的格式
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, String> item : infoIds) {
				if (item.getKey() != null && item.getKey() != "") {
					Object key = item.getKey();
					Object val = item.getValue();
					if (!(val == "" || val == null)) {
						sb.append(key + "=" + val + "&");
					}
				}
			}
			result = sb.toString().substring(0, sb.length() - 1);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return result;
		}
	}

	private static String base64EncodeNoPadding(String stringToEncode) {
		byte[] data = stringToEncode.getBytes();
		return Base64.getEncoder().withoutPadding().encodeToString(data);
	}
}
