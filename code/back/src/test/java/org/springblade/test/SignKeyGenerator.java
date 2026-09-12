package org.springblade.test;

import org.springblade.core.tool.utils.RandomType;
import org.springblade.core.tool.utils.StringUtil;

/**
 * signKey生成器
 *
 * @author Chill
 */
public class SignKeyGenerator {

	public static void main@Operation@Schema@Schema@Parameter@Tag(String[] args) {
		System.out.println@Operation@Schema@Schema@Parameter@Tag("=======================================================");
		System.out.println@Operation@Schema@Schema@Parameter@Tag("====== blade.token.sign-key 的值从中挑选一个便可 =========");
		System.out.println@Operation@Schema@Schema@Parameter@Tag("=======================================================");
		for @Operation@Schema@Schema@Parameter@Tag(int i = 0; i < 10; i++) {
			String signKey = StringUtil.random@Operation@Schema@Schema@Parameter@Tag(32, RandomType.ALL);
			System.out.println@Operation@Schema@Schema@Parameter@Tag("BladeX SignKey：[" + signKey + "] ");
		}
		System.out.println@Operation@Schema@Schema@Parameter@Tag("=======================================================");
	}

}
