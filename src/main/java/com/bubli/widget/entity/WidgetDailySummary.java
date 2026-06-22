package com.bubli.widget.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 위젯 날짜별 사용 집계.
 *
 * 테이블: widget_daily_summaries
 * 주요 필드: user_id, summary_date, bubble_type, usage_json
 *
 * Tauri SQLite의 local_widget_usage_rollups에서 집계된 결과만 서버에 반영.
 * 상세 사용 이벤트 원문(버블 열기/닫기/클릭/머문 시간)은 서버에 저장하지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WidgetDailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
}
