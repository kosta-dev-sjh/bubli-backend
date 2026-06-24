package com.bubli.project.dto;

import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.type.PaymentStatus;
import com.bubli.project.type.ProjectRoomStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectRoomResult(
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
	public static ProjectRoomResult from(ProjectRoom projectRoom) {
		return new ProjectRoomResult(
				projectRoom.getId(),
				projectRoom.getCreatedByUserId(),
				projectRoom.getName(),
				projectRoom.getClientName(),
				projectRoom.getContractAmount(),
				projectRoom.getPaymentStatus(),
				projectRoom.getPaymentDueDate(),
				projectRoom.getPaidAt(),
				projectRoom.getStatus(),
				projectRoom.getClosedAt(),
				projectRoom.getCreatedAt(),
				projectRoom.getUpdatedAt()
		);
	}
}
