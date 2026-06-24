package com.bubli.widget.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.widget.type.BubbleType;
import com.bubli.widget.type.WidgetItemStateValue;
import com.bubli.widget.type.WidgetItemType;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "widget_item_states",
	uniqueConstraints = @UniqueConstraint(name = "uk_widget_item_states_user_item", columnNames = {"user_id", "bubble_type", "item_type", "item_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WidgetItemState extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "bubble_type", nullable = false, length = 30)
	private BubbleType bubbleType;

	@Enumerated(EnumType.STRING)
	@Column(name = "item_type", nullable = false, length = 30)
	private WidgetItemType itemType;

	@Column(name = "item_id", nullable = false)
	private UUID itemId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private WidgetItemStateValue state = WidgetItemStateValue.VISIBLE;

}
