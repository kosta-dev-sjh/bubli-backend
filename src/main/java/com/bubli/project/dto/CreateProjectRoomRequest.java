package com.bubli.project.dto;

import com.bubli.project.type.PaymentStatus;
import com.bubli.project.type.ProjectRoomStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProjectRoomRequest(
		@NotBlank(message = "프로젝트룸 이름은 필수입니다.")
		@Size(min = 1, max = 120, message = "프로젝트룸 이름은 1자 이상 120자 이하여야 합니다.")
		String name,

		@Size(max = 120, message = "클라이언트명은 120자 이하여야 합니다.")
		String clientName,

		@DecimalMin(value = "0", message = "계약 금액은 0 이상이어야 합니다.")
		BigDecimal contractAmount,

		PaymentStatus paymentStatus,
		LocalDate paymentDueDate,
		LocalDate paidAt,
		ProjectRoomStatus status
) {
	public CreateProjectRoomCommand toCommand() {
		return new CreateProjectRoomCommand(
				name == null ? null : name.trim(),
				clientName == null ? null : clientName.trim(),
				contractAmount,
				paymentStatus,
				paymentDueDate,
				paidAt,
				status
		);
	}
}
