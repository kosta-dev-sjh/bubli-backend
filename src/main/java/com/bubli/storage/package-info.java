/**
 * [storage] 파일 저장소 연동 모듈.
 *
 * 책임:
 * - StorageService 인터페이스 제공 (save, getDownloadUrl, delete)
 * - LocalFileStorageService 구현 (개발 검증용)
 * - S3StorageService 구현 (운영 기준)
 * - 다운로드 URL 발급 (서버 권한 확인 후)
 * - 파일 크기 제한 검증
 * - 저장 용량 계산에 필요한 파일 메타데이터 제공
 *
 * 주요 클래스:
 * - service/StorageService           : 파일 저장 인터페이스
 * - service/LocalFileStorageService  : 로컬 파일 저장 구현 (개발용)
 * - service/S3StorageService         : AWS S3 저장 구현 (운영용)
 * - dto/FileUploadResult             : 저장 결과 (storageKey, size, checksum)
 * - type/StorageType                 : LOCAL, S3
 *
 * 규칙:
 * - S3 객체는 public으로 열지 않는다.
 * - 같은 파일 checksum은 반복 분석하지 않는다.
 */
package com.bubli.storage;
