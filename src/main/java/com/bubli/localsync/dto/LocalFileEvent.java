package com.bubli.localsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public record LocalFileEvent(
        @NotBlank @Size(max = 40) String eventType,
        String fileName,
        Long fileSizeBytes,
        @NotBlank @Size(max = 120) @JsonDeserialize(using = ChangeIdDeserializer.class) String localEventId,
        String mimeType,
        java.util.UUID resourceId,
        @Size(max = 120) String syncDeviceId,
        @Positive @Max(9007199254740991L) Long localSequence
) {
    public LocalFileEvent(String eventType, String fileName, Long fileSizeBytes, String localEventId,
                         String mimeType, java.util.UUID resourceId) {
        this(eventType, fileName, fileSizeBytes, localEventId, mimeType, resourceId, null, null);
    }

    public static final class ChangeIdDeserializer extends StdDeserializer<String> {
        public ChangeIdDeserializer() { super(String.class); }

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return (String) context.handleUnexpectedToken(String.class, parser);
            }
            return parser.getText();
        }
    }
}
