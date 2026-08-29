# PayProcess

실시간 송금 및 결제 시스템을 목표로 개발 중인 Java/Spring Boot 기반 사이드 프로젝트입니다.

현재는 Spring Boot와 JPA를 학습하면서
사용자 생성, 계좌 생성, 입금 처리까지 단계적으로 구현하고 있습니다.

---

## 기술 스택

- Java 21
- Spring Boot 4.1.1
- Spring MVC
- Spring Data JPA
- Hibernate
- H2 Database
- Gradle

향후 추가 예정

- Kafka
- MySQL 또는 PostgreSQL
- 테스트 코드
- 예외 처리
- 동시성 제어
- Docker

---

## 프로젝트 목표

단순 CRUD 프로젝트가 아니라,
송금/결제 과정에서 발생하는 실제 문제를 단계적으로 해결하는 것을 목표로 합니다.

주요 학습 및 구현 목표는 다음과 같습니다.

- REST API 설계
- Spring 계층 구조 이해
- JPA와 ORM 이해
- Entity 연관관계 설계
- Transaction 처리
- 송금 처리
- 거래 이력 관리
- 동시성 문제 해결
- Kafka 기반 비동기 이벤트 처리

---

# 현재 구현 구조

현재 요청 처리 구조는 다음과 같습니다.

```text
Client
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
Spring Data JPA
  ↓
Hibernate
  ↓
H2 Database