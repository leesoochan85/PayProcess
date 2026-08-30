# PayProcess

실시간 송금 및 결제 시스템을 목표로 개발 중인 **Java / Spring Boot 기반 사이드 프로젝트**입니다.

단순 CRUD 구현에 그치지 않고, 송금·결제 시스템에서 발생할 수 있는 트랜잭션, 동시성, 장애 복구, 비동기 처리 등의 문제를 단계적으로 학습하고 해결하는 것을 목표로 합니다.

현재는 Spring Boot와 JPA를 기반으로 사용자, 계좌, 입금, 계좌 간 송금 기능을 구현하며 JPA의 영속성 컨텍스트와 트랜잭션 동작을 학습하고 있습니다.

---

## 기술 스택

### Backend

* Java 21
* Spring Boot 4.1.1
* Spring MVC
* Spring Data JPA
* Hibernate
* Gradle

### Database

* H2 Database

현재는 학습과 빠른 테스트를 위해 H2를 사용하고 있습니다.

### 향후 추가 예정

* Kafka
* MySQL 또는 PostgreSQL
* JUnit 기반 테스트 코드
* 동시성 제어
* Docker
* 거래 이력 관리
* 비동기 이벤트 처리

---

# 프로젝트 목표

PayProcess는 단순한 계좌 CRUD 프로젝트가 아니라 실제 송금/결제 시스템에서 고려해야 하는 문제를 단계적으로 구현하는 것을 목표로 합니다.

주요 학습 및 구현 목표는 다음과 같습니다.

* REST API 설계
* Spring 계층 구조 이해
* JPA / ORM 이해
* Entity 연관관계 설계
* 영속성 컨텍스트 이해
* Dirty Checking
* Transaction 처리
* Rollback
* 송금 처리
* API 요청/응답 DTO 분리
* 예외 처리 구조화
* 거래 이력 관리
* 동시성 문제 해결
* Kafka 기반 비동기 이벤트 처리

---

# 현재 구현 구조

현재 기본적인 요청 처리 구조는 다음과 같습니다.

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
```

송금 처리에서는 다음과 같은 흐름으로 동작합니다.

```text
Client
  ↓
TransferController
  ↓
TransferService
  ↓
@Transactional
  ↓
Account 조회
  ↓
출금 계좌 withdraw()
  ↓
입금 계좌 deposit()
  ↓
Dirty Checking
  ↓
COMMIT / ROLLBACK
```

---

# 패키지 구조

현재 프로젝트는 기능 단위로 패키지를 구분하고 있습니다.

```text
com.payflow.payflow
├── user
│   ├── User.java
│   ├── UserController.java
│   ├── UserService.java
│   └── UserRepository.java
│
├── account
│   ├── Account.java
│   ├── AccountController.java
│   ├── AccountService.java
│   └── AccountRepository.java
│
└── transfer
    ├── TransferController.java
    └── TransferService.java
```

DTO 학습 이후에는 다음과 같은 구조로 확장할 예정입니다.

```text
transfer
├── dto
│   ├── TransferRequest.java
│   └── TransferResponse.java
├── TransferController.java
└── TransferService.java

account
├── dto
│   └── AccountResponse.java
├── Account.java
├── AccountController.java
├── AccountService.java
└── AccountRepository.java
```

---

# 현재 구현 기능

## 1. 사용자 생성

사용자를 생성하고 JPA를 통해 H2 Database에 저장합니다.

```text
POST /api/users
```

사용자 데이터는 `User` Entity로 관리합니다.

---

## 2. 계좌 생성

사용자에게 계좌를 생성할 수 있습니다.

`User`와 `Account` 사이의 연관관계를 JPA로 구성하여 Entity 관계를 학습했습니다.

개념적으로 다음 관계를 가집니다.

```text
User
  │
  │ 1 : N
  ▼
Account
```

하나의 사용자가 여러 계좌를 가질 수 있도록 구성합니다.

---

## 3. 계좌 조회

현재 생성된 계좌들을 조회할 수 있습니다.

```text
GET /api/accounts
```

현재 Entity를 직접 응답하는 구조에서는 Hibernate 내부 정보가 JSON에 포함될 수 있습니다.

예:

```json
{
  "accountNumber": "1bc1f377",
  "user": {
    "name": "홍길동",
    "hibernateLazyInitializer": {},
    "id": 1
  },
  "balance": 10000,
  "id": 1
}
```

이 문제를 해결하기 위해 Entity를 API 응답에 직접 노출하지 않고 `AccountResponse` DTO로 변환하는 구조를 학습하고 있습니다.

목표 응답 형태:

```json
{
  "id": 1,
  "accountNumber": "1bc1f377",
  "balance": 10000,
  "userId": 1,
  "userName": "홍길동"
}
```

---

## 4. 입금

계좌에 금액을 입금할 수 있습니다.

```text
POST /api/accounts/{accountId}/deposit
```

예:

```json
{
  "amount": 10000
}
```

입금 로직은 Account Entity 내부에서 처리합니다.

```java
account.deposit(amount);
```

Service에서 직접 balance 값을 수정하지 않고 Entity가 자신의 상태 변경 규칙을 담당하도록 구성했습니다.

---

## 5. 계좌 간 송금

두 계좌 사이에서 금액을 송금할 수 있습니다.

```text
POST /api/transfers
```

예:

```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 3000
}
```

송금 과정은 다음과 같습니다.

```text
1번 계좌: 10,000원
2번 계좌: 10,000원

1번 → 2번
3,000원 송금

↓

1번 계좌: 7,000원
2번 계좌: 13,000원
```

---

# Transaction

송금은 단순히 한 계좌에서 금액을 빼고 다른 계좌에 더하는 작업이 아닙니다.

다음 두 작업이 반드시 함께 성공하거나 함께 실패해야 합니다.

```text
출금 계좌
- 3,000원

입금 계좌
+ 3,000원
```

이를 위해 송금 Service에 `@Transactional`을 적용했습니다.

```java
@Transactional
public void transfer(
        Long fromAccountId,
        Long toAccountId,
        Long amount
) {
    ...
}
```

정상적으로 모든 작업이 완료되면:

```text
COMMIT
```

중간에 예외가 발생하면:

```text
ROLLBACK
```

되어 일부 작업만 DB에 반영되는 것을 방지합니다.

---

# Dirty Checking

송금 과정에서는 별도의 `save()` 호출 없이 Entity의 상태만 변경합니다.

```java
fromAccount.withdraw(amount);
toAccount.deposit(amount);
```

JPA가 관리하고 있는 Entity의 값이 변경되면 트랜잭션 종료 시점에 변경된 내용을 감지하고 필요한 `UPDATE` SQL을 실행합니다.

이를 JPA의 **Dirty Checking(변경 감지)** 이라고 합니다.

```text
Account 조회
    ↓
영속성 컨텍스트에서 관리
    ↓
withdraw() / deposit()
    ↓
Entity 상태 변경
    ↓
Dirty Checking
    ↓
UPDATE SQL
    ↓
COMMIT
```

---

# Rollback 테스트

Transaction의 동작을 확인하기 위해 출금 이후 의도적으로 예외를 발생시키는 테스트도 진행했습니다.

예:

```java
fromAccount.withdraw(amount);

throw new RuntimeException("송금 중 오류 발생");

toAccount.deposit(amount);
```

출금 코드가 먼저 실행되었더라도 트랜잭션이 정상적으로 완료되지 않으면 전체 작업이 Rollback되어 DB의 잔액은 변경되지 않는 것을 확인하는 방식입니다.

이를 통해 송금 시스템에서 `@Transactional`이 필요한 이유를 학습했습니다.

---

# 송금 Validation

현재 송금 과정에서 잘못된 요청을 방지하기 위한 기본 검증을 추가했습니다.

### 동일 계좌 송금 방지

```java
if (fromAccountId.equals(toAccountId)) {
    throw new IllegalArgumentException(
            "같은 계좌로는 송금할 수 없습니다."
    );
}
```

### 존재하지 않는 출금 계좌

```java
accountRepository.findById(fromAccountId)
        .orElseThrow(() ->
                new IllegalArgumentException(
                        "출금 계좌가 존재하지 않습니다."
                )
        );
```

### 존재하지 않는 입금 계좌

```java
accountRepository.findById(toAccountId)
        .orElseThrow(() ->
                new IllegalArgumentException(
                        "입금 계좌가 존재하지 않습니다."
                )
        );
```

### 잔액 부족

출금 가능한 금액보다 많은 금액을 요청할 경우 출금을 거부하도록 Account에서 검증합니다.

```text
balance < amount
    ↓
출금 거부
```

---

# DTO

API 계층과 Entity를 분리하기 위해 DTO 구조를 학습하고 있습니다.

DTO는 **Data Transfer Object**로 API 요청 및 응답 데이터를 전달하기 위한 객체입니다.

예:

```java
public record TransferRequest(
        Long fromAccountId,
        Long toAccountId,
        Long amount
) {
}
```

클라이언트의 JSON 요청은 다음 흐름으로 처리됩니다.

```text
JSON
 ↓
TransferRequest DTO
 ↓
Controller
 ↓
Service
```

응답도 Entity를 직접 반환하기보다 Response DTO로 변환하는 구조를 적용할 예정입니다.

```text
Entity
 ↓
Response DTO
 ↓
Controller
 ↓
JSON
```

이를 통해 다음 문제를 방지할 수 있습니다.

* Entity 내부 구조가 API에 직접 노출되는 문제
* Hibernate 내부 데이터 노출
* Entity 변경이 API 명세 변경으로 이어지는 문제
* 필요한 데이터만 선택적으로 응답하기 어려운 문제

---

# API

현재까지 구현하거나 학습한 주요 API입니다.

| Method | Endpoint                            | 기능       |
| ------ | ----------------------------------- | -------- |
| POST   | `/api/users`                        | 사용자 생성   |
| POST   | `/api/accounts`                     | 계좌 생성    |
| GET    | `/api/accounts`                     | 계좌 목록 조회 |
| POST   | `/api/accounts/{accountId}/deposit` | 계좌 입금    |
| POST   | `/api/transfers`                    | 계좌 간 송금  |

API는 IntelliJ의 `.http` 파일을 이용하여 테스트하고 있습니다.

예:

```http
POST http://localhost:8080/api/transfers
Content-Type: application/json

{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 3000
}
```

---

# 학습 진행 상황

## Day 1 — Spring MVC 기본 구조

학습 내용:

* Controller
* Service
* REST API
* JSON 요청/응답
* IntelliJ `.http` 파일을 통한 API 테스트

기본 요청 흐름을 학습했습니다.

```text
Client
 ↓
Controller
 ↓
Service
```

---

## Day 2 — JPA + H2

학습 내용:

* Spring Data JPA
* Entity
* Repository
* H2 Database
* Hibernate

구조가 다음과 같이 확장되었습니다.

```text
Controller
 ↓
Service
 ↓
Repository
 ↓
JPA
 ↓
H2
```

---

## Day 3 — Entity 연관관계와 입금

학습 내용:

* User / Account Entity
* Entity 연관관계
* `@ManyToOne`
* 입금 도메인 로직
* `@Transactional`
* Dirty Checking

```text
User
  ↓
Account
```

사용자와 계좌 사이의 관계를 JPA로 표현하고 계좌 Entity가 잔액 변경을 담당하도록 구현했습니다.

---

## Day 4 — 계좌 간 송금

학습 내용:

* 계좌 간 송금
* Transaction
* Dirty Checking
* Rollback
* 도메인 메서드
* 동일 계좌 송금 검증

```text
Account A
   ↓
withdraw()
   ↓
Transaction
   ↓
deposit()
   ↓
Account B
```

송금 과정 전체를 하나의 Transaction으로 묶어 데이터 정합성을 유지하는 방법을 학습했습니다.

---

## Day 5 — 예외 처리

학습 내용:

* 잘못된 송금 요청 처리
* 잔액 부족
* 존재하지 않는 계좌
* 동일 계좌 송금
* HTTP 오류 응답 구조
* Global Exception Handler 개념
* Custom Exception 설계 방향

향후 다음과 같이 에러 응답을 통일할 예정입니다.

```json
{
  "code": "ACCOUNT_NOT_FOUND",
  "message": "출금 계좌가 존재하지 않습니다."
}
```

---

## Day 6 — DTO와 Entity 분리

현재 학습 중입니다.

학습 내용:

* Request DTO
* Response DTO
* Java `record`
* Entity 직접 노출의 문제점
* Entity → DTO 변환

목표 구조:

```text
Client
 ↓
Request DTO
 ↓
Controller
 ↓
Service
 ↓
Entity
 ↓
Repository
```

응답:

```text
Entity
 ↓
Response DTO
 ↓
Controller
 ↓
Client
```

---

# 현재까지 배운 핵심

### Spring 계층 구조

```text
Controller
 ↓
Service
 ↓
Repository
```

각 계층의 역할을 분리합니다.

### Entity

DB에 저장되는 데이터와 비즈니스 상태를 표현합니다.

### Repository

Spring Data JPA를 이용하여 DB에 접근합니다.

### Transaction

여러 DB 작업을 하나의 논리적인 작업으로 처리합니다.

### Dirty Checking

JPA가 관리 중인 Entity의 변경을 감지하여 자동으로 UPDATE SQL을 실행합니다.

### Rollback

트랜잭션 도중 문제가 발생하면 변경 내용을 취소하여 데이터 정합성을 유지합니다.

### DTO

API에서 사용하는 데이터와 DB Entity를 분리합니다.

---

# 앞으로 구현할 기능

현재 JPA와 송금의 기본 동작을 학습한 이후 실제 결제 시스템에 가까운 방향으로 확장할 예정입니다.

### 1. DTO 구조 완성

* TransferRequest
* TransferResponse
* AccountResponse
* UserResponse

### 2. 예외 처리 구조화

* `@RestControllerAdvice`
* Custom Exception
* ErrorCode
* 일관된 ErrorResponse

### 3. 송금 이력

송금 성공 여부와 거래 내용을 DB에 기록합니다.

예:

```text
Transfer
├── id
├── fromAccount
├── toAccount
├── amount
├── status
└── createdAt
```

### 4. 테스트 코드

* Service 단위 테스트
* Repository 테스트
* Controller 테스트
* Transaction Rollback 테스트

### 5. 동시성 제어

동시에 같은 계좌에서 송금 요청이 들어오는 문제를 다룰 예정입니다.

예:

```text
잔액: 10,000원

요청 A: 8,000원 출금
요청 B: 8,000원 출금
```

동시 요청으로 인해 실제 잔액보다 많은 금액이 출금되는 문제를 해결합니다.

학습 예정:

* Optimistic Lock
* Pessimistic Lock
* `@Version`

### 6. Kafka

송금 핵심 로직과 직접 관련 없는 후속 작업을 이벤트 기반으로 분리할 예정입니다.

예:

```text
송금 성공
   ↓
Transaction Commit
   ↓
TransferCompleted Event
   ↓
Kafka
   ├── 알림
   ├── 거래 이력 처리
   └── 후속 이벤트 처리
```

---

# 최종 목표 구조

```text
Client
  ↓
Spring Boot API
  ↓
Payment / Transfer Service
  ↓
Database
  ↓
Transaction Commit
  ↓
Kafka
  ├── Notification
  ├── Transaction History
  └── Async Processing
```

단순 기능 구현보다는 **왜 이러한 구조가 필요한지 이해하면서 점진적으로 확장하는 것**을 프로젝트의 핵심 목표로 합니다.
