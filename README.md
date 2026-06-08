# 📊 K-Stock Compass

> **한국투자증권(KIS) Open API와 DART API를 활용한 AI 기반 주식 정보 조회 및 목표가 알림 서비스**

복잡한 재무 정보를 직관적인 **5단계 재무 신호등**으로 제공하고,
목표가 도달 시 실시간 알림을 받을 수 있도록 개발한 개인 프로젝트입니다.

🔗 https://k-stockcompass.kr

---

## 📌 Overview

K-Stock Compass는 개인 투자자가 기업의 재무 건전성과 주가 정보를 쉽게 확인할 수 있도록 만든 웹 서비스입니다.

* 실시간 주가 조회
* AI 기반 재무 분석
* 관심종목 및 목표가 알림
* JWT + Redis 인증 시스템

**Development Period**

* 2026.01 ~ 2026.06

---

## ⚙️ Tech Stack

| Category       | Technology                                     |
| -------------- | ---------------------------------------------- |
| Backend        | Java 17, Spring Boot 3.x, Spring Security, JWT |
| ORM            | JPA, Hibernate                                 |
| Database       | MariaDB (AWS RDS), Redis                       |
| Infrastructure | AWS EC2, AWS RDS, Nginx, Let's Encrypt         |
| External API   | KIS Open API, DART API, OpenAI API             |
| Frontend       | Thymeleaf, HTML/CSS, JavaScript                |

---

## 🚀 Main Features

### 🔐 Authentication

* 회원가입 / 로그인
* JWT Access Token & Refresh Token
* Redis 기반 Refresh Token 관리
* 이메일 인증 (Redis TTL)
* AES-128 CBC 개인정보 암호화
* BCrypt 비밀번호 암호화

### 📈 Stock Information

* 실시간 주가 조회
* 종목 검색
* KOSPI / KOSDAQ 지수 조회

### 🚦 Financial Signal

* DART API 재무제표 분석
* OpenAI API 기반 AI 재무 해설
* 5단계 재무 신호등 제공
* AI Agent 챗봇

### ⭐ Alert Service

* 관심종목 관리
* 목표가 알림 등록
* 장중 5분 단위 알림 스케줄러
* 브라우저 알림 및 탭 강조

### 💼 Portfolio

* 보유 종목 관리
* 실시간 평가 손익
* 수익 시뮬레이션
* 자산 변화 그래프

---

## 🏗 Architecture

```text
Browser
   │
 HTTPS
   │
 Nginx
   │
Spring Boot
   ├── Security (JWT)
   ├── Controller
   ├── Service
   ├── Repository
   │
   ├── MariaDB (AWS RDS)
   ├── Redis
   │     ├── Refresh Token
   │     ├── Email Verification
   │     └── KIS Access Token
   │
   └── External APIs
         ├── KIS Open API
         ├── DART API
         └── OpenAI API
```

---

## 🔑 Key Implementations

### JWT + Redis Authentication

* Access Token 만료 시 401 반환
* authFetch()를 통한 자동 재발급
* Redis Refresh Token 검증
* 재발급 실패 시 자동 로그아웃

### KIS API Token Cache

* JVM Memory Cache
* Redis Cache
* KIS API 직접 호출

3단계 캐시 구조를 적용하여 API 호출량을 최소화했습니다.

### Data Encryption

* 이메일 / 전화번호 : AES-128 CBC
* 비밀번호 : BCrypt

---

## 📂 Project Structure

```text
src/main/java/kopo/kstockcompass
├── config
├── controller
├── dto
├── entity
├── repository
├── scheduler
├── security
├── service
└── util
```

---

## 🛠 Trouble Shooting

| Issue                | Solution                        |
| -------------------- | ------------------------------- |
| JWT 만료 시 서버 오류       | ExpiredJwtException 처리 후 401 반환 |
| EC2 시간대 문제           | Asia/Seoul TimeZone 설정          |
| Refresh Token 재발급 실패 | 예외 전달 구조 개선                     |
| 인증 헤더 누락             | authFetch() 공통 모듈 적용            |

---

## 📚 What I Learned

* JWT + Spring Security 인증 구조
* Redis Cache 및 TTL 활용
* AWS EC2 / AWS RDS 서비스 운영
* Nginx Reverse Proxy 및 HTTPS 구성
* Open API 연동 및 캐싱 전략
* 문제 해결 과정 문서화와 리팩토링

---

## 👨‍💻 Developer

**Yu Young Sang**

Backend Developer

GitHub : https://github.com/isyouyoung

Email : [isyouyoung@gmail.com](mailto:isyouyoung@gmail.com)
