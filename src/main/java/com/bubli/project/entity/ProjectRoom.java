package com.bubli.project.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.project.type.PaymentStatus;
import com.bubli.project.type.ProjectRoomStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "project_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectRoom extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "created_by_user_id", nullable = false)
	private UUID createdByUserId;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "client_name", length = 120)
	private String clientName;

	@Column(name = "contract_amount", precision = 15, scale = 2)
	private BigDecimal contractAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false, length = 30)
	private PaymentStatus paymentStatus = PaymentStatus.NOT_RECORDED;

	@Column(name = "payment_due_date")
	private LocalDate paymentDueDate;

	@Column(name = "paid_at")
	private LocalDate paidAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ProjectRoomStatus status = ProjectRoomStatus.ACTIVE;

	@Column(name = "closed_at")
	private Instant closedAt;

}
