package com.bubli.user.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BubliIdGeneratorTest {

	private final BubliIdGenerator generator = new BubliIdGenerator();

	@Test
	void generateReturnsSpecialLettersDigitsFormat() {
		String bubliId = generator.generate("google-sub", 0);

		assertThat(bubliId).matches("[!@#$*\\-_+][a-z]{4}[0-9]{4}");
		assertThat(bubliId).hasSize(9);
	}

	@Test
	void generateChangesCandidateByAttempt() {
		String first = generator.generate("google-sub", 0);
		String second = generator.generate("google-sub", 1);

		assertThat(second).matches("[!@#$*\\-_+][a-z]{4}[0-9]{4}");
		assertThat(second).isNotEqualTo(first);
	}
}
