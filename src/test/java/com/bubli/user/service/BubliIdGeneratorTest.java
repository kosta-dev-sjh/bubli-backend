package com.bubli.user.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BubliIdGeneratorTest {

	private final BubliIdGenerator generator = new BubliIdGenerator();

	@Test
	void generateReturnsWordAndFourDigits() {
		String bubliId = generator.generate("google-sub", 0);

		assertThat(bubliId).matches("[a-z]{4}[0-9]{4}");
		assertThat(bubliId).doesNotStartWith("bubli");
		assertThat(bubliId).hasSize(8);
	}

	@Test
	void generateChangesCandidateByAttempt() {
		String first = generator.generate("google-sub", 0);
		String second = generator.generate("google-sub", 1);

		assertThat(second).matches("[a-z]{4}[0-9]{4}");
		assertThat(second).isNotEqualTo(first);
	}
}
