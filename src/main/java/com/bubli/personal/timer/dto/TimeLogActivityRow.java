package com.bubli.personal.timer.dto;

import java.time.Instant;

public record TimeLogActivityRow(Instant startedAt, Long durationSeconds) {
}
