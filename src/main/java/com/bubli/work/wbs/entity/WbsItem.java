package com.bubli.work.wbs.entity;

import com.bubli.work.wbs.type.WbsStatus;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "wbs_items",
	uniqueConstraints = @UniqueConstraint(name = "uk_wbs_items_room_parent_order", columnNames = {"room_id", "parent_id", "order_no"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WbsItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Column(name = "parent_id")
	private UUID parentId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(name = "order_no", nullable = false)
	private Integer orderNo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private WbsStatus status = WbsStatus.TODO;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static WbsItem create(UUID roomId, UUID parentId, String title, Integer orderNo, WbsStatus status) {
		WbsItem item = new WbsItem();
		item.roomId = roomId;
		item.parentId = parentId;
		item.title = title;
		item.orderNo = orderNo;
		item.status = status == null ? WbsStatus.TODO : status;
		return item;
	}

	public void update(String title, UUID parentId, Integer orderNo, WbsStatus status) {
		if (title != null) {
			this.title = title;
		}
		if (parentId != null) {
			this.parentId = parentId;
		}
		if (orderNo != null) {
			this.orderNo = orderNo;
		}
		if (status != null) {
			this.status = status;
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
