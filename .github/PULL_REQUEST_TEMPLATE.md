## 작업 내용
<!-- 이 PR에서 무엇을 했는지 간단히 적어주세요 -->


## 변경 사항
- [ ] 새 기능
- [ ] 버그 수정
- [ ] 리팩토링
- [ ] 설정/문서
- [ ] 기타

## 확인 사항
- [ ] 로컬에서 빌드 성공 (`./gradlew build`)
- [ ] 관련 테스트 작성 또는 기존 테스트 통과 확인
- [ ] ArchUnit 테스트 통과 (`./gradlew test --tests '*ArchitectureTest'`)
- [ ] Entity를 API 응답으로 직접 반환하지 않음
- [ ] 다른 도메인의 Repository를 직접 호출하지 않음
- [ ] 다른 도메인의 Entity를 직접 참조하지 않음
- [ ] Service public method가 Controller Request DTO를 직접 받지 않음
- [ ] 필요한 도메인 간 접근은 `*PublicService` 또는 공개 service 경계로 처리함
- [ ] 기존 `V1` Flyway migration을 수정하지 않음
- [ ] enum/status 값은 최신 `09_Data-Model.md`와 `10_API-Design.md` 기준을 따름
- [ ] 민감정보(secret, API key)가 포함되지 않음

## 스크린샷 / 참고
<!-- API 테스트 결과, 로그 등 필요하면 첨부 -->
