package com.bubli.project.entity;

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
public class ProjectRoom {

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

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static ProjectRoom create(UUID createdByUserId, String name, String clientName, BigDecimal contractAmount,
			PaymentStatus paymentStatus, LocalDate paymentDueDate, LocalDate paidAt, ProjectRoomStatus status) {
		ProjectRoom projectRoom = new ProjectRoom();
		projectRoom.createdByUserId = createdByUserId;
		projectRoom.name = name;
		projectRoom.clientName = clientName;
		projectRoom.contractAmount = contractAmount;
		projectRoom.paymentStatus = paymentStatus == null ? PaymentStatus.NOT_RECORDED : paymentStatus;
		projectRoom.paymentDueDate = paymentDueDate;
		projectRoom.paidAt = paidAt;
		projectRoom.status = status == null ? ProjectRoomStatus.ACTIVE : status;
		return projectRoom;
	}

	public void updateBasicInfo(String name, String clientName, ProjectRoomStatus status, Instant closedAt) {
		if (name != null) {
			this.name = name;
		}
		if (clientName != null) {
			this.clientName = clientName;
		}
		if (status != null) {
			this.status = status;
			this.closedAt = status == ProjectRoomStatus.CLOSED ? closedAt : null;
		}
	}

	public void updatePayment(BigDecimal contractAmount, PaymentStatus paymentStatus, LocalDate paymentDueDate,
			LocalDate paidAt) {
		this.contractAmount = contractAmount;
		this.paymentStatus = paymentStatus == null ? PaymentStatus.NOT_RECORDED : paymentStatus;
		this.paymentDueDate = paymentDueDate;
		this.paidAt = paidAt;
	}

	public void close(Instant closedAt) {
		this.status = ProjectRoomStatus.CLOSED;
		if (this.closedAt == null) {
			this.closedAt = closedAt;
		}
	}

	@PrePersist
	private void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	private void onUpdate() {
		this.updatedAt = Instant.now();
	}

}
