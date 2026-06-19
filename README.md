# 🫧 Bubli Backend

> 프리랜서를 위한 업무 관리 어시스턴트 — Backend API Server

[![Java](https://img.shields.io/badge/Java-21-007396?style=flat&logo=openjdk&logoColor=white)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.x-6DB33F?style=flat&logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![AWS EC2](https://img.shields.io/badge/AWS%20EC2-FF9900?style=flat&logo=amazonec2&logoColor=white)](https://aws.amazon.com/ec2/)
[![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=flat&logo=githubactions&logoColor=white)](https://github.com/features/actions)

---

## 📌 소개

**Bubli**는 프리랜서의 업무 흐름을 정리해주는 AI 기반 업무 관리 어시스턴트입니다.
이 레포는 Bubli의 **백엔드 API 서버**로, 사용자 인증부터 AI 에이전트 연동, 실시간 음성/영상 채팅 시그널링까지 핵심 비즈니스 로직을 담당합니다.

## ✨ 주요 기능

- **AI 문서 분석 에이전트** — Spring AI 기반 `@Tool` 에이전트가 업무 문서를 분석하고 작업(Task)을 자동으로 추출
- **벡터 검색 기반 대화 메모리** — pgvector + OpenAI Embedding(`text-embedding-3-small`)으로 컨텍스트를 기억하는 AI 채팅
- **실시간 음성/영상 채팅** — LiveKit Cloud 연동으로 1:1 및 멀티 유저 음성·영상 통화, 화면 공유 지원
- **작업 관리 API** — 프리랜서의 프로젝트/태스크를 등록, 조회, 관리하는 RESTful API

## 🛠 기술 스택

| 구분 | 스택 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.x, Spring AI |
| Database | pgvector (PostgreSQL) |
| AI | OpenAI Embedding, AWS Bedrock |
| Realtime | LiveKit Cloud |
| Infra | Docker, AWS EC2 |
| CI/CD | GitHub Actions |

## 🚀 시작하기

```bash
# 클론
git clone https://github.com/kosta-dev-sjh/bubli-backend.git
cd bubli-backend

# 환경 변수 설정
cp src/main/resources/application-secret.yml.example src/main/resources/application-secret.yml
# application-secret.yml에 본인 API 키 등 입력

# 실행
./gradlew bootRun
```

## 📁 프로젝트 구조

```
src/main/java/com/bubli/
├── auth/          # 인증/인가
├── agent/         # AI 에이전트 (Spring AI Tool)
├── voicechat/      # LiveKit 시그널링
├── task/           # 작업 관리
└── common/         # 공통 유틸
```

## 🌿 협업 규칙

브랜치 전략, 커밋 컨벤션, PR 규칙은 [GIT_CONVENTION.md](./GIT_CONVENTION.md) 참고

## 👥 팀

KOSTA Final Project Team — **Hae Maeil**
