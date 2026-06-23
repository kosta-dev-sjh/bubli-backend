package com.bubli.global.response;

import com.bubli.global.error.ErrorCode;
import com.bubli.global.error.ErrorResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void failResponseCanIncludeValidationFields() throws Exception {
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.COMMON_400_002,
                "trace-123",
                List.of(new ErrorResponse.FieldError("title", "제목은 필수입니다."))
        );

        JsonNode json = objectMapper.valueToTree(ApiResponse.fail(errorResponse));

        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("data").isNull()).isTrue();
        assertThat(json.at("/error/code").asText()).isEqualTo("COMMON_400_002");
        assertThat(json.at("/error/fields/0/field").asText()).isEqualTo("title");
        assertThat(json.at("/error/fields/0/reason").asText()).isEqualTo("제목은 필수입니다.");
    }
}
