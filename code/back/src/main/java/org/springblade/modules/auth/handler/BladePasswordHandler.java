package org.springblade.modules.auth.handler;

import org.springblade.core.oauth2.handler.OAuth2PasswordHandler;
import org.springblade.core.oauth2.props.OAuth2Properties;
import org.springblade.core.tool.utils.DigestUtil;
import org.springblade.core.tool.utils.SM2Util;

/**
 * SM2 解密后使用 DigestUtil.encrypt（SHA1(MD5)）与数据库密码比对
 */
public class BladePasswordHandler extends OAuth2PasswordHandler {

	private final OAuth2Properties oAuth2Properties;

	public BladePasswordHandler(OAuth2Properties properties) {
		super(properties);
		this.oAuth2Properties = properties;
	}

	@Override
	public boolean matches(String rawPassword, String encodedPassword) {
		String decryptPassword = SM2Util.decrypt(rawPassword, oAuth2Properties.getPublicKey(), oAuth2Properties.getPrivateKey());
		return DigestUtil.encrypt(decryptPassword).equals(encodedPassword);
	}

	@Override
	public String encode(String rawPassword) {
		return DigestUtil.encrypt(rawPassword);
	}
}
