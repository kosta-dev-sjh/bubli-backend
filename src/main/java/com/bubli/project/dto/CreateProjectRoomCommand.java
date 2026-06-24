package com.bubli.project.dto;

import com.bubli.project.type.PaymentStatus;
import com.bubli.project.type.ProjectRoomStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProjectRoomCommand(
		String name,
		String clientName,
		BigDecimal contractAmount,
		PaymentStatus paymentStatus,
		LocalDate paymentDueDate,
		LocalDate paidAt,
		ProjectRoomStatus status
) {
}
