package com.bubli.widget.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 버블별 항목 활성 상태.
 *
 * 테이블: widget_item_states
 * 주요 필드: user_id, bubble_type, item_id, display_state, selected_room_id
 *
 * 위젯 항목 상태의 원본은 서버 widget_item_states다.
 * 각 버블(TODO, 에이전트, 소통, 타이머, 메모, 일정/WBS, 자료 제안, 알림)의
 * 사용자별 활성 여부와 표시 설정을 관리한다.
 *
 * 원본 데이터(TODO, 일정 등)는 복사 저장하지 않고 각 도메인에서 조회한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WidgetItemState {
}
