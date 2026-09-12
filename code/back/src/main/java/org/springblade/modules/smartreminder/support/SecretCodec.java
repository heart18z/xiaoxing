package org.springblade.modules.smartreminder.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecretCodec {

	private static final String PREFIX = "v1:";
	private static final SecureRandom RANDOM = new SecureRandom();
	private final SecretKeySpec key;

	public SecretCodec(@Value("${blade.token.sign-key}") String signKey) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(signKey.getBytes(StandardCharsets.UTF_8));
			this.key = new SecretKeySpec(digest, "AES");
		} catch (Exception e) {
			throw new IllegalStateException("初始化密钥加密器失败", e);
		}
	}

	public String encrypt(String plainText) {
		if (plainText == null || plainText.isBlank()) return "";
		if (plainText.startsWith(PREFIX)) return plainText;
		try {
			byte[] iv = new byte[12];
			RANDOM.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
			byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
			byte[] packed = new byte[iv.length + encrypted.length];
			System.arraycopy(iv, 0, packed, 0, iv.length);
			System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
			return PREFIX + Base64.getEncoder().encodeToString(packed);
		} catch (Exception e) {
			throw new IllegalStateException("加密AI服务密钥失败", e);
		}
	}

	public String decrypt(String encryptedText) {
		if (encryptedText == null || encryptedText.isBlank()) return "";
		if (!encryptedText.startsWith(PREFIX)) return encryptedText;
		try {
			byte[] packed = Base64.getDecoder().decode(encryptedText.substring(PREFIX.length()));
			byte[] iv = new byte[12];
			byte[] encrypted = new byte[packed.length - iv.length];
			System.arraycopy(packed, 0, iv, 0, iv.length);
			System.arraycopy(packed, iv.length, encrypted, 0, encrypted.length);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("解密AI服务密钥失败", e);
		}
	}
}
