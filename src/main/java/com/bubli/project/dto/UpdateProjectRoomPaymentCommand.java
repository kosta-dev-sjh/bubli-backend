package com.bubli.project.dto;

import com.bubli.project.type.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProjectRoomPaymentCommand(
		BigDecimal contractAmount,
		PaymentStatus paymentStatus,
		LocalDate paymentDueDate,
		LocalDate paidAt
) {
}
