package com.bubli.resource.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 프로젝트룸 자료 버전 기록.
 *
 * 테이블: resource_versions
 * 주요 필드: resource_id, version_no, file_id, created_by
 *
 * 같은 자료를 재업로드하면 기존 파일을 덮어쓰지 않고 새 버전으로 저장한다.
 * 최신 version만 기본 표시하고, 이전 버전은 버전 목록에서 선택.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
}
