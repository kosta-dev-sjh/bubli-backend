# Tech Architecture

## 12. 기술 구성

### 12.1 기술 스택

|영역|기술|선택 이유|적용 기준|
|---|---|---|---|
|웹|Next.js 16, TypeScript|공개 사이트와 회원 웹 앱을 나누고, Tauri 앱에서도 같은 화면 재사용|공개 사이트, 대시보드, 프로젝트룸, 자료보드, 채널, 설정|
|앱|Tauri 2|회원 웹 앱을 감싸고 개인 위젯, 로컬 파일, 개인 폴더, 활동 감지 추가|앱 실행, WebView, 버블 위젯, 드래그앤드롭, 폴더 스캔|
|서버|Spring Boot (EC2 단일 서버)|인증, API, 에이전트 모듈을 한 서버에서 관리. EC2 t3.small 1대로 충분한 규모|모듈형 모놀리식, Docker Compose 운영|
|보안|Spring Security, JWT|로그인 사용자와 프로젝트룸 권한 확인|이메일 로그인, API 보호|
|에이전트|Spring AI ChatClient, Structured Output, Advisor, Tool Calling|문서 요약, 후보 추출, WBS, 도구 호출을 Spring Boot 내부 에이전트 모듈에서 처리|자료 분석, 관련 문서 추천, 후보 생성, agent_jobs|
|모델|AWS Bedrock 우선, 직접 API 폴백|서버에서 모델 호출 관리, 키 미노출|1개 모델 우선|
|문서 처리|PDFBox, Apache POI, Tika, TextSplitter|PDF, DOCX, TXT, MD 텍스트 추출 및 chunk 분리|1차 지원 형식|
|DB|PostgreSQL + pgvector (RDS t3.micro)|일반 데이터와 문서 임베딩을 한 DB에서 함께 관리. Private Subnet 배치|프로젝트, 자료, 분석 결과, chunk|
|캐시|Redis (EC2 컨테이너 직접 운영)|모델 호출 Rate Limit, 분석 캐시, WebSocket 상태 관리. 관리형 ElastiCache 대신 EC2에 직접 구성해 비용 절감|호출 제한, 분석 캐시|
|파일 저장|LocalStorage 검증 → S3 저장|초기 빠른 검증 후 S3로 전환. StorageService 인터페이스로 구현체 분리|S3 버킷/IAM 인프라 준비, presigned URL, 막히면 서버 중계 업로드 폴백|
|로컬 기록과 검색|SQLite, FTS5 (Tauri 클라이언트)|개인 관리 폴더 변경분, 프로젝트룸 채팅 캐시, 개인 에이전트 단기기억, 위젯 표시 캐시, 위젯 상세 이벤트, 타이머 복구 상태, 로컬 백업 manifest를 클라이언트에 저장|로컬 파일명 검색, 변경 감지, 채팅 빠른 조회, 위젯 빠른 표시, 개인 SQLite 백업|
|실시간|WebSocket, STOMP|채팅, 새 자료, 댓글, 버전 알림. Redis로 상태 공유|채팅, 알림|
|보이스챗|LiveKit Cloud, WebRTC|직접 음성 서버 구축 없이 방/참가자 관리. API key·secret 서버 전용 관리|프로젝트룸 보이스챗 우선, 1:1은 확장|
|구글 캘린더 연동|Google Calendar MCP|외부 일정을 읽어 Bubli 일정과 함께 확인|읽기 중심 연동|
|배포|EC2 t3.small 1대 (Docker Compose)|초기 운영 규모에서는 단일 서버가 배포, 장애 확인, 로그 추적이 단순하다. 필요 시 같은 EC2 안에서 agent 컨테이너를 분리한다|web/api 중심 배포, 이후 agent 컨테이너 분리 가능|
|모니터링|Spring Boot Actuator + Prometheus + Grafana, 로그는 CloudWatch Logs|EC2 t3.small(RAM 2GB) 메모리 제약상 ELK는 제외. Grafana로 앱 메트릭 시각화, CloudWatch로 인프라/로그 확인|기본 운영 확인 + 메트릭 대시보드|

### 12.1.1 모듈형 모놀리스 구현 기준

Bubli는 Spring Boot 단일 서버를 기준으로 구현한다. 프로젝트룸, 자료보드, 에이전트, 채팅, 위젯이 서로 자주 연결되기 때문에 처음부터 서버를 여러 개로 나누면 기능 흐름보다 배포와 운영이 더 복잡해진다.

그래서 서버는 하나로 운영하되 내부 패키지는 도메인별로 나눈다. 인증, 사용자, 프로젝트룸, 자료, 에이전트, 작업, 알림, 채팅, 보이스, 저장소 경계를 코드 안에서 분리하면 기능 위치가 선명해진다. 에이전트 모듈은 나중에 별도 컨테이너로 분리할 수 있게 API 서버와 핵심 업무 모듈과 책임을 나눠 둔다.

이 구조의 기준은 세 가지다.

|기준|설명|
|---|---|
|DB 트랜잭션 단순화|프로젝트룸 생성, 자료 업로드, 후보 승인, TODO 반영처럼 한 흐름에서 같이 저장되는 데이터가 많으므로 서버 안에서 한 번에 처리한다.|
|권한 검증 일원화|개인 자료, 프로젝트룸 자료, 친구, 채팅, 위젯 데이터 접근 권한을 한 서버에서 같은 기준으로 확인한다.|
|배포와 장애 확인 단순화|EC2 한 대와 Docker Compose 기준으로 배포하고, Spring Boot 로그와 메트릭에서 문제를 확인한다.|

```text
backend
├─ auth
├─ user
├─ project
├─ room
├─ resource
├─ agent
├─ task
├─ notification
├─ chat
├─ voice
└─ storage
```

### RAG 기본 설정

Bubli는 업로드된 자료를 chunk로 나눈 뒤 embedding을 생성하고, PostgreSQL + pgvector에 저장한다.

사용자가 자료 검색이나 관련 문서 추천을 요청하면 같은 프로젝트룸과 권한 범위 안에서 먼저 검색한 뒤, 검색된 문서를 에이전트 답변의 근거로 사용한다.

|항목|기준|
|---|---|
|Chat Model|Claude Haiku 4.5|
|Embedding Model|Amazon Titan Text Embeddings V2|
|Vector Dimension|1024|
|Chunk Size|500~1000 tokens|
|TopK|5~8개|
|Similarity Threshold|초기에는 설정하지 않고 결과를 보고 조정|
|Vector DB|PostgreSQL + pgvector|

### 12.2 전체 아키텍처

초기 구현은 Spring Boot 단일 백엔드 안에 `agent` 모듈을 포함한 모듈형 모놀리식으로 개발한다. 프론트와 Tauri 앱은 에이전트를 직접 호출하지 않고 항상 API 서버만 호출한다. 에이전트 호출량, RAG 복잡도, 프롬프트 실험 관리가 커지면 같은 EC2 안에서 Docker Compose 기준 별도 `agent` 컨테이너로 분리할 수 있게 설계한다. EC2 여러 대 분리는 현재 구조가 아니라 운영 확장 단계로 둔다.

```
사용자
├─ 브라우저 공개 사이트
│  └─ Next.js 공개 페이지: 소개, 기능 안내, 다운로드, 로그인 진입
├─ 브라우저 회원 웹 앱
│  └─ Next.js 회원 화면: 대시보드, 프로젝트룸, 자료보드, WBS, 소통, 설정
└─ Bubli 데스크탑 앱
   ├─ Tauri WebView: Next.js 회원 웹 앱
   ├─ 개인 위젯과 버블
   ├─ 개인 관리 폴더 스캔
   ├─ 프로젝트룸 채팅 캐시
   ├─ 개인 에이전트 로컬 단기기억
   ├─ 개인 SQLite 백업과 복구
   ├─ 파일 드래그앤드롭
   └─ 활성 앱, 창 제목 감지

Next.js 회원 웹 앱 / Tauri
-> HTTPS API 요청
-> WebSocket 연결

EC2 t3.small (Docker Compose, Public Subnet) - 초기 구조
├─ Nginx: 리버스 프록시, HTTPS 종료
├─ Spring Boot Backend API
│  ├─ 핵심 업무 모듈
│  │  ├─ Auth: 회원가입, 로그인, 인증
│  │  ├─ Project: 프로젝트, 프로젝트룸, 멤버
│  │  ├─ Personal: 대시보드, 개인 TODO, 메모, 타이머, 개인 제안
│  │  ├─ Resource: 개인 자료, 프로젝트룸 자료, 댓글, 버전
│  │  ├─ Storage: Local/S3 업로드, 파일 검증
│  │  ├─ Chat: 개인, 프로젝트룸 채팅
│  │  ├─ Memory: 프로젝트룸 장기기억, 사용자 확인 하루정리 요약
│  │  ├─ Voice: LiveKit 방 생성, 토큰 발급, 참가 권한 확인
│  │  ├─ Notification: 알림 저장과 발행
│  │  ├─ Calendar: 구글 캘린더 읽기
│  │  └─ Dashboard: 작업 현황 계산
│  └─ 에이전트 모듈
│     └─ 계약 문서 분석, 요구사항 후보, 관련 문서, WBS, 문서 초안, 하루 정리
├─ Redis: 모델 호출 Rate Limit, 분석 캐시, WebSocket 상태
├─ Prometheus: 메트릭 수집
└─ Grafana: 메트릭 시각화 대시보드

EC2 1대 안의 이후 확장 구조
├─ web container: Next.js 공개 사이트와 회원 웹 앱
├─ api container: 인증, 권한, 원본 데이터, 후보 승인, WebSocket
├─ agent container: 문서 분석, RAG, 모델 호출, 후보 JSON 생성
├─ redis container: rate limit, 분석 캐시, 비동기 작업 보조
└─ monitoring containers: Prometheus, Grafana

데이터 저장
├─ RDS PostgreSQL + pgvector (Private Subnet)
│  : 프로젝트, 자료, 작업, 채팅 원본, 장기요약, 문서 chunk
├─ SQLite: Tauri 로컬 파일 색인, 프로젝트룸 채팅 캐시, 개인 에이전트 단기기억, 위젯 표시 캐시, 위젯 상세 이벤트, 타이머 복구 상태, 동기화 대기열, 백업 manifest
├─ S3: 업로드 파일 원본 저장 (개발 검증 단계에서는 LocalStorage 사용 가능)
└─ LiveKit Cloud: 보이스챗 미디어 연결

운영/관측
└─ CloudWatch: EC2/RDS 인프라 메트릭, 애플리케이션 로그
```

![Bubli 회원앱 데이터 흐름](assets/member-app-data-flow-v14.png)

### 12.2.1 PostgreSQL 백업과 복구 전략

서버 원본 데이터는 RDS PostgreSQL + pgvector를 기준으로 보호한다. RDS에는 프로젝트룸, 자료 메타데이터, 작업, 채팅 원본, 장기요약, 문서 chunk와 embedding이 들어가므로 로컬 SQLite 백업과 별도로 운영 DB 백업 정책을 둔다.

백업은 RDS 자동 백업을 기본으로 하고, 배포와 마이그레이션처럼 위험이 큰 작업 전에는 수동 스냅샷을 추가로 만든다.

|구분|기준|
|---|---|
|RDS 자동 백업|개발/초기 운영은 7일 보관으로 시작하고, 실제 운영 데이터가 쌓이면 14일 또는 30일로 늘린다.|
|특정 시점 복구|자동 백업과 WAL을 이용해 장애 또는 실수 직전 시점으로 복구할 수 있게 한다.|
|수동 스냅샷|운영 배포 전, Flyway 마이그레이션 전, 대량 데이터 수정 전, pgvector 인덱스나 embedding 재처리 전에 생성한다.|
|최종 스냅샷|운영 환경에서는 DB 삭제 시 최종 스냅샷을 남긴다. `skip_final_snapshot`은 운영에서 `false`로 둔다.|
|삭제 보호|운영 환경에서는 `deletion_protection`을 켜서 실수로 RDS가 삭제되지 않게 한다.|
|논리 백업|주기적으로 `pg_dump`를 생성해 S3에 암호화 저장한다. RDS 전체 복구가 아니라 일부 데이터 확인, 이관, 로컬 검증에 사용한다.|
|복구 테스트|월 1회 이상 별도 DB에 스냅샷 또는 `pg_dump`를 복원하고, Flyway 마이그레이션과 Spring Boot 기동까지 확인한다.|

초기 Terraform은 비용과 실습 편의를 위해 개발 환경 기준으로 둘 수 있다. 다만 운영 전환 시에는 아래 설정을 반드시 반영한다.

```hcl
backup_retention_period = 7
skip_final_snapshot     = false
deletion_protection     = true
```

복구 우선순위는 장애 유형에 따라 나눈다. 전체 DB 장애나 RDS 손상은 RDS 스냅샷 또는 특정 시점 복구를 우선 사용한다. 테이블 일부 삭제, 잘못된 배치 작업, 데이터 비교가 필요한 경우에는 `pg_dump` 백업을 별도 DB에 복원한 뒤 필요한 데이터만 확인하거나 재반영한다.

백업 대상에는 일반 테이블뿐 아니라 pgvector 확장, embedding 컬럼, vector 인덱스 생성 기준도 포함한다. 복구 후에는 단순히 DB가 열리는지만 보지 않고 로그인, 프로젝트룸 조회, 채팅 조회, 자료 검색/RAG 조회가 동작하는지 확인한다.

### 12.3 Spring AI 처리 흐름

초기 구현에서는 아래 흐름이 Spring Boot 백엔드 내부 에이전트 모듈에서 동작한다. 별도 `agent` 컨테이너로 분리하더라도 API 계약은 유지하고, 프론트와 Tauri 앱은 계속 API 서버만 호출한다.

```
1. 사용자가 파일을 업로드한다.
   → [EC2 / Spring Boot] 로그인, 권한, 개인 자료·프로젝트룸 자료 범위, 크기·형식 검사
2. 원본 파일은 저장소에 저장한다.
   → [S3 / 개발 검증 단계에서는 LocalStorage 사용 가능]
3. DB에는 파일명, 저장 위치, projectId, roomId, ownerId, visibility를 저장한다.
   → [RDS PostgreSQL]
4. PDFBox, POI, Tika로 문서 텍스트를 추출한다.
   → [EC2 / Spring Boot 내 처리]
5. TextSplitter로 문서를 chunk로 나눈다.
   → [EC2 / Spring Boot 내 처리]
6. EmbeddingModel로 chunk를 벡터로 변환해 저장한다.
   → [RDS pgvector]
7. 같은 파일 hash가 이미 분석된 경우 캐시 결과를 사용한다.
   → [Redis 분석 캐시 — NFR-11]
8. agent_jobs를 생성하고 작업 상태를 PENDING/RUNNING으로 관리한다.
   → [RDS PostgreSQL]
9. ChatClient와 Structured Output으로 요약·후보를 JSON으로 생성한다.
   → [AWS Bedrock 또는 직접 모델 연결, 사용자별 모델 요청 횟수는 Redis로 제한 — NFR-10]
10. 결과 JSON은 schema_version 기준으로 검증하고 prompt_version, model_name, job_id를 함께 남긴다.
   → [RDS PostgreSQL]
11. 결과를 resource_analysis, requirement_candidates, task_candidates, agent_suggestions 등에 후보로 저장한다.
   → [RDS PostgreSQL]
12. 사용자가 승인한 후보만 API 서버와 핵심 업무 모듈이 WBS, TODO, 일정 같은 확정 데이터로 반영한다.
   → [RDS PostgreSQL]
13. 화면에는 자료 상세 패널, 프로젝트룸 WBS/작업판, 대시보드, 위젯 버블로 보여준다.
    → [Next.js 회원 웹 앱 / Tauri 위젯]
```

### 12.4 LiveKit 보이스챗 흐름

LiveKit API key와 secret은 서버에서만 관리하며 클라이언트(웹·Tauri)에는 노출하지 않는다 (NFR-14).

```
사용자가 프로젝트룸 소통 화면에서 보이스챗 시작
-> Spring Boot가 로그인 사용자와 채팅방, 프로젝트룸 권한 확인
-> voice_rooms에 통화방 메타데이터 저장 [RDS PostgreSQL]
-> Spring Boot가 LiveKit roomName과 participant identity로 접속 토큰 발급
   (key/secret은 GitHub Secrets + 서버 환경변수로만 관리)
-> Next.js 또는 Tauri가 serverUrl과 token으로 LiveKit Cloud 접속
-> 사용자가 나가면 참가 기록과 종료 시간을 저장
```

프로젝트룸 보이스챗은 음성 연결, 마이크 켜기와 끄기, 나가기, 참여자 표시를 기본 기능으로 둔다. 1:1 보이스챗은 같은 구조를 재사용할 수 있지만 프로젝트룸 보이스가 안정화된 뒤 확장한다. 녹음, 자동 회의록, 음성 요약은 향후 확장으로 둔다. LiveKit Cloud를 우선 사용하며, self-host는 인프라 확장 단계에서 검토한다.
