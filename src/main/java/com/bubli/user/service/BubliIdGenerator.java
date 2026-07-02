package com.bubli.user.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class BubliIdGenerator {

	private static final String SPECIAL_CHARS = "!@#$*-_+";
	private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";
	private static final String DIGITS = "0123456789";

	private static final int MAX_ATTEMPTS = 100_000;

	// 특수기호1 + 영문4 + 숫자4 형태로 생성 (예: @abcd1234)
	public String generate(String seed, int attempt) {
		String hash = hash(seed + ":" + attempt);
		char special = pick(hash, 0, SPECIAL_CHARS);
		char l1     = pick(hash, 4, LETTERS);
		char l2     = pick(hash, 8, LETTERS);
		char l3     = pick(hash, 12, LETTERS);
		char l4     = pick(hash, 16, LETTERS);
		char d1     = pick(hash, 20, DIGITS);
		char d2     = pick(hash, 24, DIGITS);
		char d3     = pick(hash, 28, DIGITS);
		char d4     = pick(hash, 32, DIGITS);
		return "" + special + l1 + l2 + l3 + l4 + d1 + d2 + d3 + d4;
	}

	public int maxAttempts() {
		return MAX_ATTEMPTS;
	}

	private char pick(String hash, int offset, String charset) {
		int idx = Math.floorMod(Integer.parseUnsignedInt(hash.substring(offset, offset + 4), 16), charset.length());
		return charset.charAt(idx);
	}

	private String hash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new BusinessException(ErrorCode.COMMON_500_001);
		}
	}
}
