package com.bubli.user.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Component
public class BubliIdGenerator {

	private static final List<String> WORDS = List.of(
			"milo", "nora", "lumi", "juno", "tavo", "runi", "sora", "navi",
			"zelo", "luna", "kiri", "mori", "ravi", "tomi", "yuna", "pico",
			"nilo", "dori", "vivi", "kano", "mira", "rino", "tori", "zuni",
			"lino", "momo", "naru", "puri", "ruka", "sumi", "tala", "yori"
	);

	private static final int NUMBER_LIMIT = 10_000;

	public String generate(String seed, int attempt) {
		String hash = hash(seed);
		int wordIndex = Math.floorMod(Integer.parseUnsignedInt(hash.substring(0, 8), 16) + attempt, WORDS.size());
		int numberSeed = Integer.parseUnsignedInt(hash.substring(8, 12), 16);
		return WORDS.get(wordIndex) + String.format(Locale.ROOT, "%04d", (numberSeed + attempt) % NUMBER_LIMIT);
	}

	public int maxAttempts() {
		return WORDS.size() * NUMBER_LIMIT;
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
