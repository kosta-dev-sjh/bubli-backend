package com.bubli.agent.dto;

import java.time.LocalDate;

public record SummarizeDayRequest(
        LocalDate summaryDate
) {
}
