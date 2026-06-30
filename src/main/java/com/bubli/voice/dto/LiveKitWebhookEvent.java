package com.bubli.voice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LiveKitWebhookEvent(
        String event,
        @JsonProperty("room") LiveKitRoom room,
        @JsonProperty("participant") LiveKitParticipant participant
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LiveKitRoom(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LiveKitParticipant(String identity) {}
}
