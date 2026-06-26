package com.bubli.project.dto;

import com.bubli.project.type.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProjectRoomPaymentRequest(
		@DecimalMin(value = "0", message = "계약 금액은 0 이상이어야 합니다.")
		BigDecimal contractAmount,

		PaymentStatus paymentStatus,
		LocalDate paymentDueDate,
		LocalDate paidAt
) {
	public UpdateProjectRoomPaymentCommand toCommand() {
		return new UpdateProjectRoomPaymentCommand(
				contractAmount,
				paymentStatus,
				paymentDueDate,
				paidAt
		);
	}
}
