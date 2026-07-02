package com.bubli.localsync.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.Map;

public record LocalFileAnalysisKeySentence(
        @Min(0) int index,
        @PositiveOrZero double score,
        @Min(0) int startOffset,
        @Min(0) int endOffset,
        @NotBlank @Size(max = 2_000) String text
) {

    public Map<String, Object> toJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("index", index);
        json.put("score", score);
        json.put("startOffset", startOffset);
        json.put("endOffset", endOffset);
        json.put("text", text == null ? null : text.trim());
        return json;
    }
}
