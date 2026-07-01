package com.bubli.voice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "livekit")
public record LiveKitProperties(
        String apiKey,
        String apiSecret,
        String serverUrl
) {}
