package com.bubli.work.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.work.type.WbsStatus;

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
public class WbsItem extends BaseTimeEntity {

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

}
