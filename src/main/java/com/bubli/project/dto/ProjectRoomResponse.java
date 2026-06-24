package com.bubli.project.dto;

import com.bubli.project.type.PaymentStatus;
import com.bubli.project.type.ProjectRoomStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectRoomResponse(
		UUID id,
		UUID createdByUserId,
		String name,
		String clientName,
		BigDecimal contractAmount,
		PaymentStatus paymentStatus,
		LocalDate paymentDueDate,
		LocalDate paidAt,
		ProjectRoomStatus status,
		Instant closedAt,
		Instant createdAt,
		Instant updatedAt
) {
	public static ProjectRoomResponse from(ProjectRoomResult result) {
		return new ProjectRoomResponse(
				result.id(),
				result.createdByUserId(),
				result.name(),
				result.clientName(),
				result.contractAmount(),
				result.paymentStatus(),
				result.paymentDueDate(),
				result.paidAt(),
				result.status(),
				result.closedAt(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
