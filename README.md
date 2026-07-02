# 🫧 Bubli Backend

> 받은 자료를, 오늘의 할 일로. — Backend API Server

[![Java](https://img.shields.io/badge/Java-21-007396?style=flat&logo=openjdk&logoColor=white)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.x-6DB33F?style=flat&logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-4169E1?style=flat&logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![AWS](https://img.shields.io/badge/AWS-EC2%20%7C%20RDS%20%7C%20Bedrock-FF9900?style=flat&logo=amazonaws&logoColor=white)](https://aws.amazon.com/)
[![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=flat&logo=githubactions&logoColor=white)](https://github.com/features/actions)

---

## 📌 소개

**Bubli**는 프리랜서가 클라이언트에게 받은 계약서, 견적서, 요구사항 문서, 회의록을 프로젝트룸에 모으면, 에이전트가 작업 범위·납품물·마감·확인 질문·WBS/TODO 후보로 정리해주는 문서 기반 업무 보조 서비스입니다.

이 레포는 Bubli의 **백엔드 단일 서버**로, 인증, 프로젝트룸/자료 관리, Spring AI 기반 에이전트 처리, 실시간 채팅·보이스챗 시그널링까지 핵심 로직을 전담합니다.

## ✨ 주요 기능

- **문서 기반 에이전트 분석** — 계약서/견적서/요구사항 문서를 PDFBox·POI·Tika로 텍스트 추출 후, Spring AI ChatClient + Structured Output으로 요약·확인 항목·WBS/TODO 후보 생성
- **승인 기반 후보 관리** — 에이전트는 자동 실행자가 아닌 후보 생성자로 동작. 사용자가 승인한 항목만 실제 작업/일정에 반영
- **프로젝트룸 & 권한 관리** — 혼자 시작 가능한 프로젝트 단위 공간, 친구 초대 및 역할(프로젝트 리더/멤버) 기반 접근 제어
- **실시간 채팅 & 보이스챗** — WebSocket/STOMP 기반 채팅, LiveKit Cloud 연동 보이스챗 (API key/secret 서버 전용 관리)
- **벡터 검색 기반 문서 질의** — pgvector 기반 임베딩 검색으로 관련 문서 추천 및 컨텍스트 응답
- **위젯/대시보드 데이터 제공** — 사용자 권한 기준으로 대시보드와 Tauri 위젯 버블에 동일한 데이터 공급

## 🛠 기술 스택

| 영역 | 기술 | 비고 |
|------|------|------|
| Language | Java 21 | |
| Framework | Spring Boot | EC2 t3.small 단일 서버 |
| Security | Spring Security, JWT | Google 로그인, API 보호 |
| AI | Spring AI (ChatClient, Structured Output, Advisor, Tool Calling) | |
| AI Model | AWS Bedrock 우선, 직접 API 폴백 | Chat: Claude Haiku 4.5 |
| Embedding | Amazon Titan Text Embeddings V2 | Vector dimension 1024 |
| 문서 처리 | PDFBox, Apache POI, Tika, TextSplitter | PDF, DOCX, TXT, MD |
| DB | PostgreSQL + pgvector (RDS t3.micro) | 일반 데이터 + 문서 임베딩 |
| Cache | Redis (EC2 컨테이너 직접 운영) | Rate Limit, 분석 캐시, WebSocket 상태 |
| 파일 저장 | LocalStorage(검증) → S3 | StorageService 인터페이스로 구현체 분리 |
| 실시간 | WebSocket, STOMP | 채팅, 알림 |
| 보이스챗 | LiveKit Cloud, WebRTC | key/secret 서버 전용 |
| 외부 연동 | Google Calendar MCP | 읽기 전용 |
| Infra | EC2 t3.small (Docker Compose), Nginx | Public Subnet, 리버스 프록시 |
| CI/CD | GitHub Actions | Rolling Update 배포 |
| 모니터링 | Spring Boot Actuator, Prometheus, Grafana, CloudWatch Logs | |

## 🏗 아키텍처

```
Next.js 회원 웹 앱 / Tauri
  -> HTTPS API / WebSocket

EC2 t3.small (Docker Compose, Public Subnet)
├─ Nginx: 리버스 프록시, HTTPS 종료
├─ Spring Boot 단일 서버
│  ├─ Auth        회원가입, 로그인, 인증
│  ├─ Project      프로젝트, 프로젝트룸, 멤버
│  ├─ Personal     대시보드, 개인 TODO, 메모, 타이머
│  ├─ Resource     개인/프로젝트룸 자료, 댓글, 버전
│  ├─ Storage      Local/S3 업로드, 파일 검증
│  ├─ Agent        문서 분석, 후보 생성, WBS, 하루 정리
│  ├─ Chat         개인/프로젝트룸 채팅
│  ├─ Memory       장기기억, 하루정리 요약
│  ├─ Voice        LiveKit 방 생성, 토큰 발급
│  ├─ Notification 알림 저장/발행
│  ├─ Calendar     구글 캘린더 읽기
│  └─ Dashboard    작업 현황 계산
├─ Redis: Rate Limit, 분석 캐시, WebSocket 상태
├─ Prometheus → Grafana

데이터 저장
├─ RDS PostgreSQL + pgvector (Private Subnet)
├─ S3 (개발 단계 LocalStorage 폴백)
└─ LiveKit Cloud: 보이스챗 미디어
```

## 🤖 에이전트 처리 흐름

```
1. 파일 업로드          → 권한·크기·형식 검사 (Spring Boot)
2. 원본 저장            → S3 (개발 단계 LocalStorage)
3. 메타데이터 저장       → RDS PostgreSQL
4. 텍스트 추출           → PDFBox / POI / Tika
5. Chunk 분리           → TextSplitter
6. 임베딩 변환 및 저장    → RDS pgvector
7. 캐시 확인 (동일 hash) → Redis 분석 캐시
8. 요약/후보 생성 (JSON) → Bedrock ChatClient + Structured Output
9. 후보 저장            → requirement_candidates, task_candidates, wbs_items 등
10. 화면 반영           → 자료 상세, WBS/작업판, 대시보드, 위젯 버블
```

## 🚀 시작하기

```bash
# 클론
git clone https://github.com/kosta-dev-sjh/bubli-backend.git
cd bubli-backend

# 환경 변수 설정
cp src/main/resources/application-secret.yml.example src/main/resources/application-secret.yml
# Google OAuth, JWT, LiveKit, AWS Bedrock 등 서버 전용 키 입력

# 실행
./gradlew bootRun
```

## 📁 프로젝트 구조

```
src/main/java/com/bubli/
├── auth/          # 인증/인가 (Spring Security, JWT)
├── project/        # 프로젝트룸, 멤버, 초대
├── resource/        # 자료, 댓글, 버전 관리
├── agent/         # Spring AI 에이전트 (문서 분석, 후보 생성)
├── chat/          # WebSocket/STOMP 채팅
├── voice/          # LiveKit 시그널링
├── dashboard/      # 대시보드, 위젯 데이터
└── common/         # 공통 유틸
```

## 🌿 협업 규칙

브랜치 전략, 커밋 컨벤션, PR 규칙은 [CONVENTION.md](./CONVENTION.md) 참고

## 👥 팀

KOSTA AI Java DevOps 파이널 프로젝트 — **해 매일**
