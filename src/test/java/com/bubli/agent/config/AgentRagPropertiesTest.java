package com.bubli.agent.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRagPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfig.class);

	@Test
	void bindsRecordConfigurationProperties() {
		contextRunner
				.withPropertyValues(
						"agent.rag.enabled=true",
						"agent.rag.top-k=7",
						"agent.rag.candidate-top-k=48",
						"agent.rag.min-similarity=0.73",
						"agent.rag.suggest-min-similarity=0.69",
						"agent.rag.personal-min-similarity=0.81"
				)
				.run(context -> {
					assertThat(context).hasSingleBean(AgentRagProperties.class);
					AgentRagProperties properties = context.getBean(AgentRagProperties.class);
					assertThat(properties.enabled()).isTrue();
					assertThat(properties.topK()).isEqualTo(7);
					assertThat(properties.candidateTopK()).isEqualTo(48);
					assertThat(properties.minSimilarity()).isEqualTo(0.73D);
					assertThat(properties.suggestMinSimilarity()).isEqualTo(0.69D);
					assertThat(properties.personalMinSimilarity()).isEqualTo(0.81D);
				});
	}

	@Test
	void appliesDefaultsWhenValuesAreMissing() {
		contextRunner.run(context -> {
			AgentRagProperties properties = context.getBean(AgentRagProperties.class);
			assertThat(properties.enabled()).isTrue();
			assertThat(properties.topK()).isEqualTo(5);
			assertThat(properties.candidateTopK()).isEqualTo(40);
			assertThat(properties.minSimilarity()).isEqualTo(0.72D);
			assertThat(properties.suggestMinSimilarity()).isEqualTo(0.0D);
			assertThat(properties.personalMinSimilarity()).isEqualTo(0.72D);
		});
	}

	@Configuration
	@EnableConfigurationProperties(AgentRagProperties.class)
	static class TestConfig {
	}
}
