package com.bubli.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.rag")
public record AgentRagProperties(
		Boolean enabled,
		Integer topK,
		Double minSimilarity,
		Double suggestMinSimilarity
) {

	public AgentRagProperties {
		if (enabled == null) {
			enabled = true;
		}
		if (topK == null) {
			topK = 5;
		}
		if (minSimilarity == null) {
			minSimilarity = 0.72D;
		}
		if (suggestMinSimilarity == null) {
			suggestMinSimilarity = 0.0D;
		}
	}
}
