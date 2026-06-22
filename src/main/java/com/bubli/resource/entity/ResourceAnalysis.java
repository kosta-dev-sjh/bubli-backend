package com.bubli.resource.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 에이전트의 자료 분석 결과.
 *
 * 테이블: resource_analysis
 * 주요 필드: resource_id, summary_json, checklist_json, status
 *
 * 에이전트가 생성한 요약, 확인할 항목, 요구사항 후보를 JSON으로 저장한다.
 * 같은 파일 checksum은 반복 분석하지 않는다 (분석 캐시).
 * status: ANALYZING → ANALYZED / FAILED
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceAnalysis {
}
