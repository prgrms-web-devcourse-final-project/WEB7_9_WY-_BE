# 📅 Kalendar

> K-POP 팬을 위한 통합 일정 관리 및 팬 활동 플랫폼

## 📑 목차
- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [시작하기](#-시작하기)
- [API 문서](#-api-문서)
- [개발 일정](#-개발-일정)
- [데모 영상](#-데모-영상)
- [팀원 소개](#-팀원-소개)
- [협업 방식](#-협업-방식)

<br>

## 🎯 프로젝트 소개

Kalendar는 K-POP 팬들이 **아티스트 일정 확인부터 공연 예매, 현장 이동까지** 한 곳에서 해결할 수 있는 통합 플랫폼입니다.

### 😥 이런 불편함을 겪고 계신가요?

- 좋아하는 아티스트 일정을 확인하려면 여러 SNS, 팬카페, 공식 사이트를 일일이 방문해야 함
- 공연 예매 사이트와 일정 정보가 분리되어 있어 불편함
- 공연장까지 가는 교통편을 구하기 위해 카톡 오픈채팅방을 뒤져야 함
- 팬들끼리 자발적으로 만든 택시팟에 참여하고 싶지만 신뢰도가 걱정됨

### ✨ Kalendar가 해결합니다

- Kalendar는 **일정 확인 → 예매 → 이동**까지 팬 활동의 전 과정을 하나의 플랫폼에서 제공합니다.

<br>

## 🚀 주요 기능

### 1. 📅 통합 일정 관리
- 팔로우한 아티스트들의 모든 일정을 하나의 캘린더에서 확인
- 콘서트, 팬미팅, 방송, 컴백 등 일정 카테고리별 분류
- 중요한 일정에 대한 실시간 알림 (SSE 기반)

### 2. 🎫 공연 예매 시스템
- 플랫폼 내에서 직접 예매 진행
- 대기열 시스템으로 공정한 예매 기회 제공
- Redis 분산락 기반의 안정적인 좌석 선점 처리
- 실시간 좌석 상태 확인

### 3. 🚗 이동팟 매칭
- 공연장 왕복을 위한 합승 파티 생성 및 참여
- 출발지, 도착지, 시간, 성별, 인원 등 상세 조건 설정
- 파티장의 신청자 승인/거절 관리 기능

### 4. 💬 실시간 채팅
- WebSocket 기반 파티별 실시간 채팅
- 파티 참여 시 자동 채팅방 입장
- 파티장의 멤버 관리 기능 (강퇴 등)

<br>

## 🛠 기술 스택

### Frontend
![React](https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Next.js](https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=next.js&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![MUI](https://img.shields.io/badge/MUI-007FFF?style=for-the-badge&logo=mui&logoColor=white)
![Zustand](https://img.shields.io/badge/Zustand-443E38?style=for-the-badge&logo=react&logoColor=white)
![React Query](https://img.shields.io/badge/React_Query-FF4154?style=for-the-badge&logo=react-query&logoColor=white)

### Backend
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)
![Java 21](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-0078D4?style=for-the-badge&logo=database&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=json-web-tokens&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socket.io&logoColor=white)
![SSE](https://img.shields.io/badge/SSE-FF6B6B?style=for-the-badge&logo=server&logoColor=white)

### Database & Cache
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Redisson](https://img.shields.io/badge/Redisson-DC382D?style=for-the-badge&logo=redis&logoColor=white)

### Payment
![Toss Payments](https://img.shields.io/badge/Toss_Payments-0064FF?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEyIDI0QzE4LjYyNzQgMjQgMjQgMTguNjI3NCAyNCAxMkMyNCA1LjM3MjU4IDE4LjYyNzQgMCAxMiAwQzUuMzcyNTggMCAwIDUuMzcyNTggMCAxMkMwIDE4LjYyNzQgNS4zNzI1OCAyNCAxMiAyNFoiIGZpbGw9IndoaXRlIi8+Cjwvc3ZnPgo=&logoColor=white)

### Infrastructure
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazon-s3&logoColor=white)
![Terraform](https://img.shields.io/badge/Terraform-7B42BC?style=for-the-badge&logo=terraform&logoColor=white)

### DevOps & Tools
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=github-actions&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![JUnit](https://img.shields.io/badge/JUnit-25A162?style=for-the-badge&logo=junit5&logoColor=white)
![JMeter](https://img.shields.io/badge/JMeter-D22128?style=for-the-badge&logo=apache-jmeter&logoColor=white)

<br>

## 🚀 시작하기

### 필수 요구사항

- Node.js 18.0 이상
- Java 21 이상
- PostgreSQL 14.0 이상
- Redis 7.0 이상
- Docker & Docker Compose

### 설치 및 실행

#### 1. 저장소 클론
```bash
git clone https://github.com/prgrms-web-devcourse-final-project/WEB7_9_WY-_BE.git
cd WEB7_9_WY-_BE
```

#### 2. 환경 변수 설정
```bash
# Backend 환경 변수 설정
cp backend/.env.example backend/.env
# 필요한 환경 변수 값을 .env 파일에 입력하세요

# Frontend 환경 변수 설정
cp frontend/.env.example frontend/.env
# 필요한 환경 변수 값을 .env 파일에 입력하세요
```

#### 3. Docker Compose로 실행
```bash
docker-compose up -d
```

#### 4. 개별 실행 (개발 모드)

**Backend**
```bash
cd backend
./gradlew bootRun
```

**Frontend**
```bash
cd frontend
pnpm install
pnpm run dev
```

<br>

## 📚 API 문서

Swagger UI를 통해 API 문서를 확인할 수 있습니다.

- 개발 환경: `http://localhost:8080/swagger-ui/index.html`
- 프로덕션: `https://idol-kalendar.shop/swagger-ui/index.html`

<br>

## 📅 개발 일정

### 1차 스프린트 (1주) - MVP 개발
- ✅ 프로젝트 초기 세팅
- ✅ 인증/인가 시스템
- ✅ 아티스트 관리 및 팔로우 기능
- ✅ 캘린더 기본 기능
- ✅ 파티 매칭 기본 기능
- ✅ 마이페이지 기본 기능

### 2차 스프린트 (2주) - 핵심 기능 고도화
- 🔄 예매 시스템 구축 (대기열, 좌석 선점)
- 🔄 실시간 채팅 기능
- 🔄 알림 시스템 (SSE)
- 🔄 AWS 배포 및 안정화

### 3차 스프린트 (1주) - 안정화 및 마무리
- ⏳ 코드 리팩터링 및 최적화
- ⏳ 버그 수정 및 안정화
- ⏳ 성능 테스트 및 개선
- ⏳ 문서화 완성

<br>

## 🎬 데모 영상

> 데모 영상은 프로젝트 완성 후 추가 예정입니다.

<br>

## 👥 팀원 소개

| 이름 | 역할 | 담당 기능 |
|------|------|-----------|
| 안병선 | PO | 인증/인가, 예매 시스템 |
| 백승범 | 백엔드 팀장 | 파티 매칭, 채팅 시스템 |
| 조영재 | 프론트 팀장 | 프론트 개발 |
| 박준석 | 아키텍트 설계 | 메인페이지, 아티스트 관리, 예매 시스템 |
| 김예진 | 고급 기능 / 성능 개선 | 캘린더, 알림 시스템 |
| 정혜연 | 고급 기능 / 성능 개선 | 마이페이지, 예매 시스템 |

<br>

## 🤝 협업 방식

### 커뮤니케이션
- 정기 회의: 주 1회 (매주 월요일 전체 회의)
- 긴급 상황: Slack 즉시 공유

### 코드 관리
- Git Flow 전략 사용
- PR 필수 리뷰
- Swagger 기반 API 문서 실시간 업데이트

### 품질 관리
- JUnit 기반 단위 테스트
- JMeter 부하 테스트
- 코드 리뷰 필수

<br>
