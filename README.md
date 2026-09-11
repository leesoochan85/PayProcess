# PayProcess

Java / Spring Boot 기반의 **송금 시스템 사이드 프로젝트**입니다.

단순 CRUD 구현에 그치지 않고 실제 송금 시스템에서 발생할 수 있는 트랜잭션, 데이터 정합성, 동시성, 중복 요청 등의 문제를 직접 재현하고 해결하는 것을 목표로 개발하고 있습니다.

현재는 Spring Boot와 JPA를 기반으로 다음 기능을 구현했습니다.

* 사용자 및 계좌 관리
* 입금
* 계좌 간 송금
* 송금 이력 저장 및 조회
* DTO 기반 API 응답
* 공통 예외 처리
* Transaction Rollback
* 동시 송금 제어
* 비관적 락(Pessimistic Lock)
* 낙관적 락(Optimistic Lock) 비교 실험
* Idempotency-Key 기반 중복 송금 방지
* 동일 멱등성 요청 재시도 시 최초 transferId 반환
* 송금 성공 응답 DTO
* MockMvc 기반 송금 응답 검증

---

# 기술 스택

## Backend

* Java 21
* Spring Boot 4.1.1
* Spring MVC
* Spring Data JPA
* Hibernate
* Jakarta Validation
* Gradle

## Database

* H2 Database

현재는 기능 개발 및 테스트를 위해 H2 In-Memory Database를 사용하고 있습니다.

## Test

* JUnit 5
* Spring Boot Test
* MockMvc
* Mockito
* AssertJ
* ExecutorService
* CountDownLatch

## 향후 추가 예정

* MySQL 또는 PostgreSQL
* Kafka
* Docker
* Pagination
* 거래 상태 관리
* 비동기 이벤트 처리

---

# 프로젝트 목표

PayProcess는 단순한 계좌 CRUD 애플리케이션이 아니라 실제 송금 시스템에서 고려해야 하는 문제를 단계적으로 구현하는 프로젝트입니다.

주요 학습 및 구현 목표는 다음과 같습니다.

* REST API 설계
* Spring 계층 구조 이해
* JPA / ORM 이해
* Entity 연관관계
* 영속성 컨텍스트
* Dirty Checking
* Transaction
* Rollback
* 예외 처리
* DTO와 Entity 분리
* 거래 이력 관리
* 동시성 문제 재현 및 해결
* Optimistic / Pessimistic Lock 비교
* API 멱등성
* 중복 요청 방지
* Kafka 기반 비동기 이벤트 처리

---

# 현재 구조

기본적인 요청 처리 구조는 다음과 같습니다.

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
Database
```

송금 요청에서는 데이터 정합성을 위해 Transaction, Pessimistic Lock, Idempotency-Key를 함께 사용합니다.

```text
Client
  ↓
POST /api/transfers
Idempotency-Key
  ↓
TransferController
  ↓
TransferService
  ↓
@Transactional
  ↓
출금 계좌 PESSIMISTIC_WRITE Lock
  ↓
Idempotency-Key 확인
  ↓
계좌 조회
  ↓
출금 / 입금
  ↓
Transfer 저장
  ↓
IdempotencyKey 저장
  ↓
COMMIT
```

---

# 패키지 구조

프로젝트는 기능 단위로 패키지를 분리하고 있습니다.

```text
com.payflow
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
├── transfer
│   ├── Transfer.java
│   ├── TransferController.java
│   ├── TransferService.java
│   ├── TransferRepository.java
│   ├── TransferResponse.java
│   └── dto
│       ├── TransferRequest.java
│       ├── TransferCreateResponse.java
│       └── AccountTransferResponse.java
│
├── idempotency
│   ├── IdempotencyKey.java
│   └── IdempotencyKeyRepository.java
│
└── exception
    ├── BusinessException.java
    ├── ErrorCode.java
    ├── ErrorResponse.java
    └── GlobalExceptionHandler.java
```

테스트에서는 송금 비즈니스 로직뿐 아니라 Transaction, Controller, 동시성, 멱등성까지 각각 검증합니다.

```text
src/test/java/com/payflow/transfer
├── TransferServiceTest
├── TransferTransactionTest
├── TransferConcurrencyTest
├── TransferControllerTest
└── TransferIdempotencyTest
```

---

# 구현 기능

## 1. 사용자 생성

사용자를 생성하고 JPA를 통해 DB에 저장합니다.

```text
POST /api/users
```

사용자 데이터는 `User` Entity로 관리합니다.

---

# 2. 계좌 생성

사용자에게 계좌를 생성할 수 있습니다.

```text
User
  │
  │ 1 : N
  ▼
Account
```

JPA 연관관계를 통해 User와 Account 관계를 관리합니다.

---

# 3. 계좌 조회

생성된 계좌 목록을 조회할 수 있습니다.

```text
GET /api/accounts
```

API 계층과 DB Entity를 분리하기 위해 응답 DTO를 사용하는 방향으로 구성하고 있습니다.

---

# 4. 입금

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

잔액 변경은 Service가 직접 필드를 수정하지 않고 Account의 도메인 메서드를 통해 수행합니다.

```java
account.deposit(amount);
```

---

# 5. 계좌 간 송금

두 계좌 사이에서 금액을 송금할 수 있습니다.

```text
POST /api/transfers
```

현재 송금 API에서는 중복 요청을 구분하기 위해 `Idempotency-Key` HTTP Header를 사용합니다.

```http
POST /api/transfers
Content-Type: application/json
Idempotency-Key: transfer-001

{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 3000
}
```

예를 들어:

```text
1번 계좌: 10,000원
2번 계좌: 10,000원

1번 → 2번
3,000원 송금

↓

1번 계좌: 7,000원
2번 계좌: 13,000원
```

송금이 정상 처리되면 생성된 거래 ID와 처리 결과를 JSON으로 반환합니다.

```json
{
  "transferId": 1,
  "status": "SUCCESS"
}
```

기존의 단순 문자열 또는 거래 ID만 반환하는 방식에서 응답 DTO를 사용하도록 개선했습니다.

이를 통해 클라이언트가 생성된 거래를 식별할 수 있고, 향후 거래 상태 관리 기능으로 확장할 수 있는 구조를 마련했습니다.

---

# 6. 송금 이력 저장

송금이 성공하면 계좌 잔액 변경뿐 아니라 `Transfer` Entity를 생성하여 거래 이력을 저장합니다.

```text
송금 요청
  ↓
출금
  ↓
입금
  ↓
Transfer 저장
  ↓
COMMIT
```

Transfer에는 다음 정보를 저장합니다.

```text
Transfer
├── id
├── fromAccountId
├── toAccountId
├── amount
└── createdAt
```

잔액 변경과 Transfer 저장은 하나의 Transaction 안에서 처리됩니다.

따라서 Transfer 저장 과정에서 예외가 발생하면 계좌 잔액 변경 역시 Rollback됩니다.

---

# 7. 거래내역 조회

## 전체 송금 이력

```text
GET /api/transfers
```

## 보낸 송금 이력

```text
GET /api/transfers/sent/{accountId}
```

## 받은 송금 이력

```text
GET /api/transfers/received/{accountId}
```

## 특정 계좌 전체 거래내역

```text
GET /api/transfers/accounts/{accountId}
```

특정 계좌 관점에서는 같은 Transfer도 `SENT` 또는 `RECEIVED`로 구분합니다.

```text
1번 → 3번
1,000원
```

1번 계좌 관점:

```json
{
  "transferId": 1,
  "type": "SENT",
  "counterAccountId": 3,
  "amount": 1000
}
```

3번 계좌 관점:

```json
{
  "transferId": 1,
  "type": "RECEIVED",
  "counterAccountId": 1,
  "amount": 1000
}
```

거래 유형은 문자열 대신 Enum으로 제한합니다.

```java
public enum TransferType {
    SENT,
    RECEIVED
}
```

---

# Transaction

송금은 다음 작업이 모두 성공해야 하나의 거래가 완료됩니다.

```text
출금
+
입금
+
Transfer 이력 저장
+
IdempotencyKey 저장
```

이를 위해 송금 Service에 `@Transactional`을 적용했습니다.

```java
@Transactional
public void transfer(...) {
    ...
}
```

모든 처리가 정상 완료되면 `COMMIT`되고, 처리 중 예외가 발생하면 `ROLLBACK`됩니다.

이를 통해 일부 작업만 DB에 반영되는 것을 방지합니다.

---

# Dirty Checking

송금 과정에서는 조회한 Account Entity에 별도의 `save()`를 호출하지 않고 상태를 변경합니다.

```java
fromAccount.withdraw(amount);
toAccount.deposit(amount);
```

JPA가 관리하는 Entity의 상태가 변경되면 Transaction 종료 시점에 Hibernate가 변경을 감지하여 필요한 `UPDATE` SQL을 수행합니다.

이 과정을 **Dirty Checking(변경 감지)** 이라고 합니다.

---

# 예외 처리

비즈니스 예외를 일관된 형태로 처리하기 위해 다음 구조를 사용합니다.

```text
BusinessException
        ↓
ErrorCode
        ↓
GlobalExceptionHandler
        ↓
ErrorResponse
```

응답 예:

```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "잔액이 부족합니다."
}
```

현재 처리하는 주요 예외는 다음과 같습니다.

| ErrorCode                  | HTTP Status | 설명                  |
| -------------------------- | ----------: | ------------------- |
| `INSUFFICIENT_BALANCE`     |         400 | 잔액 부족               |
| `SAME_ACCOUNT_TRANSFER`    |         400 | 동일 계좌 송금            |
| `ACCOUNT_NOT_FOUND`        |         404 | 존재하지 않는 계좌          |
| `INVALID_REQUEST`          |         400 | 요청 Validation 실패    |
| `IDEMPOTENCY_KEY_CONFLICT` |         400 | 동일 멱등성 키를 다른 요청에 사용 |

Validation 실패 역시 공통 ErrorResponse 형식으로 응답합니다.

---

# Transaction Rollback 테스트

송금 과정의 데이터 정합성을 확인하기 위해 거래 이력 저장 과정에서 의도적으로 실패가 발생하는 상황을 테스트했습니다.

```text
출금
  ↓
입금
  ↓
Transfer 저장 실패
  ↓
ROLLBACK
```

Transfer 저장에 실패하더라도 출금/입금 결과가 DB에 남지 않는 것을 테스트로 확인했습니다.

이를 통해 `@Transactional`이 송금 전체 작업을 하나의 단위로 처리하는 것을 검증했습니다.

---

# 동시성 문제

동일한 출금 계좌에 여러 송금 요청이 동시에 들어오면 Lost Update와 같은 데이터 정합성 문제가 발생할 수 있습니다.

예:

```text
잔액: 10,000원

Thread A: 3,000원 송금
Thread B: 3,000원 송금
```

동시에 같은 잔액을 읽으면 두 요청이 서로의 변경을 인식하지 못하는 문제가 발생할 수 있습니다.

동시성 문제를 직접 확인하기 위해 다음 도구를 사용해 테스트했습니다.

```text
ExecutorService
CountDownLatch
AtomicInteger
```

---

# 비관적 락

송금에서 출금 계좌의 데이터 정합성이 중요하기 때문에 최종 송금 처리에는 **Pessimistic Lock**을 사용합니다.

AccountRepository에서 출금 계좌를 조회할 때 `PESSIMISTIC_WRITE`를 적용합니다.

개념적인 동작은 다음과 같습니다.

```text
Thread A
  ↓
Account Lock 획득
  ↓
송금
  ↓
COMMIT
  ↓
Lock 해제

Thread B
  ↓
Lock 대기
  ↓
A 완료 후 최신 Account 조회
  ↓
송금 여부 판단
```

DB 수준에서 동일 Account에 대한 동시 쓰기를 직렬화하여 잔액 정합성을 보호합니다.

---

# 낙관적 락 비교

동시성 제어 학습을 위해 `@Version` 기반 Optimistic Lock 방식도 비교했습니다.

Optimistic Lock은 데이터를 먼저 잠그지 않고 변경 시점에 Version을 비교합니다.

```text
Thread A → version = 0 조회
Thread B → version = 0 조회

Thread A → UPDATE version 0 → 1 성공

Thread B → UPDATE WHERE version = 0
           ↓
           이미 version = 1
           ↓
           충돌
```

비교 결과 송금처럼 동일 계좌를 대상으로 충돌 가능성이 존재하고 데이터 정합성이 중요한 작업에서는 비관적 락을 최종 구현 방식으로 선택했습니다.

---

# API 멱등성

네트워크 재시도나 버튼 중복 클릭으로 동일 송금 요청이 여러 번 서버에 전달될 수 있습니다.

예:

```text
사용자: 3,000원 송금

첫 요청 → 송금 성공

응답 전달 실패
  ↓
클라이언트 재시도
  ↓
동일 송금이 다시 실행될 위험
```

이를 방지하기 위해 송금 API에 **Idempotency-Key**를 도입했습니다.

---

## Idempotency-Key

클라이언트는 송금 요청마다 고유한 키를 Header로 전달합니다.

```http
Idempotency-Key: transfer-test-001
```

서버에서는 처리한 키를 `IdempotencyKey` Entity로 저장합니다.

```text
IdempotencyKey
├── id
├── idempotencyKey
├── transferId
├── fromAccountId
├── toAccountId
├── amount
└── createAt
```

`idempotencyKey`에는 DB UNIQUE 제약을 적용하여 동일한 키가 중복 저장되지 않도록 했습니다.

---

# 동일 요청 중복 처리

동일한 멱등성 키와 동일한 송금 요청이 다시 들어오면 실제 송금을 다시 수행하지 않습니다.

첫 번째 요청:

```text
Idempotency-Key: abc-001

2 → 3
3,000원

↓

송금 실행
↓

Transfer 저장
transferId = 1

↓

IdempotencyKey 저장
transferId = 1
```

응답:

```json
{
  "transferId": 1,
  "status": "SUCCESS"
}
```

같은 요청을 다시 전송하면:

```text
Idempotency-Key: abc-001

2 → 3
3,000원

↓

기존 IdempotencyKey 발견

↓

송금 재실행하지 않음

↓

기존 transferId 반환
```

응답:

```json
{
  "transferId": 1,
  "status": "SUCCESS"
}
```

따라서 동일한 요청을 여러 번 보내더라도 실제 송금은 한 번만 수행되며, 재요청에서도 최초 송금에서 생성된 동일한 `transferId`를 반환합니다.

이를 통해 중복 상태 변경을 방지할 뿐 아니라 재시도 요청에도 일관된 처리 결과를 제공합니다.

---

# 멱등성 + 동시 요청 문제

초기 구현에서는 다음 순서로 멱등성 키를 검사했습니다.

```text
Idempotency-Key 확인
  ↓
출금 계좌 Lock
  ↓
송금
```

하지만 동일한 키의 두 요청이 정확히 동시에 들어오면 두 Thread가 모두 키가 존재하지 않는다고 판단할 수 있었습니다.

```text
Thread A → Key 없음
Thread B → Key 없음

Thread A → 송금
Thread B → 송금
```

이후 두 요청 모두 IdempotencyKey 저장을 시도하면서 DB UNIQUE 제약 충돌이 발생했습니다.

데이터 자체는 Transaction Rollback으로 보호되었지만 두 번째 요청에서 예외가 발생한다는 문제가 있었습니다.

이를 해결하기 위해 처리 순서를 다음과 같이 변경했습니다.

```text
출금 계좌 Pessimistic Lock
        ↓
Idempotency-Key 조회
        ↓
송금 처리
        ↓
IdempotencyKey 저장
```

동일한 출금 계좌에 대한 요청은 Lock을 통해 순차 처리됩니다.

```text
Thread A
  ↓
Account Lock
  ↓
Key 없음
  ↓
송금
  ↓
Key 저장
  ↓
COMMIT


Thread B
  ↓
Account Lock 대기
  ↓
A COMMIT
  ↓
Lock 획득
  ↓
Key 존재
  ↓
송금 생략
```

이를 통해 동시 요청에서도 두 요청 자체는 정상 처리되면서 실제 송금은 한 번만 발생하도록 구현했습니다.

---

# 동일 Idempotency-Key 재사용 방지

Idempotency-Key의 존재 여부만 검사하면 다음 문제가 발생할 수 있습니다.

첫 번째 요청:

```text
Idempotency-Key: abc-001

2 → 3
3,000원
```

두 번째 요청:

```text
Idempotency-Key: abc-001

2 → 3
5,000원
```

두 요청의 내용은 다른데 키만 같기 때문에 단순 존재 여부 검사에서는 두 번째 요청을 첫 요청의 재시도로 잘못 판단할 수 있습니다.

이를 방지하기 위해 IdempotencyKey에 요청 정보를 함께 저장합니다.

```text
fromAccountId
toAccountId
amount
```

기존 요청과 재요청의 내용을 비교합니다.

```text
같은 Key + 같은 요청
→ 기존 요청의 재시도
→ 송금 재실행하지 않음

같은 Key + 다른 요청
→ 잘못된 Key 재사용
→ 400 Bad Request
```

실제 오류 응답:

```json
{
  "code": "IDEMPOTENCY_KEY_CONFLICT",
  "message": "동일한 멱등성 키로 다른 송금 요청을 보낼 수 없습니다."
}
```

---

# 멱등성 테스트

`TransferIdempotencyTest`에서는 다음 상황을 검증합니다.

## 멱등성 적용 전 중복 요청

```text
10,000원

3,000원 송금
3,000원 송금

↓

4,000원
Transfer 2건
```

동일한 API 요청을 구분하지 않으면 실제로 두 번 처리되는 문제를 재현했습니다.

## 동일 Idempotency-Key 순차 요청

```text
요청 1
→ 송금 실행
→ transferId = 1

요청 2
→ 동일 Key + 동일 요청
→ 송금 실행 생략
→ 기존 transferId = 1 반환

↓

잔액 7,000원
Transfer 1건
IdempotencyKey 1건

firstTransferId == secondTransferId
```

동일 요청이 한 번만 처리되는 것뿐 아니라 최초 요청과 재요청에서 반환되는 `transferId`가 동일한지도 검증합니다.

## 같은 Key + 다른 요청

```text
key = same-key-test
3,000원 송금

↓

동일 key
5,000원 송금

↓

IDEMPOTENCY_KEY_CONFLICT
```

첫 번째 송금 결과만 유지되는 것을 검증합니다.

## 동일 Key 동시 요청

두 개의 Thread에서 같은 Idempotency-Key로 동시에 송금을 요청합니다.

```text
Thread A ─┐
          ├─ 같은 Key
Thread B ─┘
```

최종 검증:

```text
두 요청 정상 처리
실제 송금 1회
Transfer 1건
IdempotencyKey 1건
```

---

# DTO

API 계층과 Entity를 분리하기 위해 DTO를 사용합니다.

```text
JSON
 ↓
Request DTO
 ↓
Controller
 ↓
Service
```

응답:

```text
Entity
 ↓
Response DTO
 ↓
Controller
 ↓
JSON
```

현재 송금 영역에서는 다음 DTO를 사용합니다.

* `TransferRequest`
* `TransferCreateResponse`
* `TransferResponse`
* `AccountTransferResponse`

송금 성공 시 `TransferCreateResponse`를 통해 다음과 같은 JSON 응답을 반환합니다.

```json
{
  "transferId": 1,
  "status": "SUCCESS"
}
```

이를 통해 API가 단순 문자열이나 숫자를 반환하지 않고 명확한 응답 구조를 갖도록 개선했습니다.

Entity 구조와 외부 API 명세를 분리하여 DB 구조 변경이 API에 직접 영향을 주는 것을 줄이는 것을 목표로 합니다.

---

# API

현재 구현한 주요 API입니다.

| Method | Endpoint                              | 기능            |
| ------ | ------------------------------------- | ------------- |
| POST   | `/api/users`                          | 사용자 생성        |
| POST   | `/api/accounts`                       | 계좌 생성         |
| GET    | `/api/accounts`                       | 계좌 목록 조회      |
| POST   | `/api/accounts/{accountId}/deposit`   | 계좌 입금         |
| POST   | `/api/transfers`                      | 계좌 간 송금       |
| GET    | `/api/transfers`                      | 전체 송금 이력 조회   |
| GET    | `/api/transfers/sent/{accountId}`     | 보낸 송금 조회      |
| GET    | `/api/transfers/received/{accountId}` | 받은 송금 조회      |
| GET    | `/api/transfers/accounts/{accountId}` | 특정 계좌 전체 거래내역 |

송금 POST 요청에는 `Idempotency-Key` Header가 필요합니다.

API 테스트는 IntelliJ `.http` 파일을 사용합니다.

---

# 테스트

현재 송금 영역은 여러 계층에서 테스트합니다.

## TransferServiceTest

Mockito 기반 Service 단위 테스트입니다.

주요 비즈니스 로직과 Repository 호출을 검증합니다.

## TransferTransactionTest

Transfer 저장에 실패했을 때 계좌 잔액까지 Rollback되는지 검증합니다.

## TransferConcurrencyTest

여러 Thread가 동일한 계좌에 동시에 접근하는 상황을 재현하고 동시성 제어 동작을 검증합니다.

## TransferControllerTest

MockMvc를 사용하여 HTTP 요청과 응답을 검증합니다.

현재 다음 내용을 테스트합니다.

* 요청 Validation
* BusinessException 응답
* 송금 성공 시 HTTP 200 응답
* 응답 JSON의 `transferId`
* 응답 JSON의 `status = SUCCESS`

Service 로직뿐 아니라 실제 Controller가 반환하는 API 응답 형식까지 자동 테스트로 검증합니다.

## TransferIdempotencyTest

Idempotency-Key를 기반으로 다음을 검증합니다.

* 멱등성 미적용 시 중복 송금 발생
* 동일 키 동일 요청 1회 처리
* 동일 키 동일 요청 재시도 시 동일 transferId 반환
* 동일 키 다른 요청 충돌
* 동일 키 동시 요청 1회 처리

전체 테스트 실행:

```bash
./gradlew test
```

Windows:

```powershell
.\gradlew test
```

현재 전체 테스트가 정상 통과하는 것을 확인했습니다.

```text
BUILD SUCCESSFUL
```

---

# 문제 해결 과정

## 1. 송금 도중 저장 실패

### 문제

계좌 잔액은 변경되었지만 Transfer 저장 과정에서 실패할 가능성이 존재합니다.

### 해결

송금 전체를 `@Transactional`로 묶었습니다.

```text
Account 변경
+
Transfer 저장

↓

하나라도 실패

↓

전체 ROLLBACK
```

---

## 2. 동시 송금 Lost Update

### 문제

같은 출금 계좌에 여러 요청이 동시에 접근하면 동일한 잔액을 기준으로 계산할 수 있습니다.

### 해결

동시성 테스트를 통해 문제를 재현한 후 `PESSIMISTIC_WRITE` Lock을 적용했습니다.

```text
동일 출금 계좌
  ↓
한 Transaction만 쓰기 Lock 획득
  ↓
나머지 Transaction 대기
```

---

## 3. 동일 송금 중복 처리

### 문제

같은 요청을 두 번 보내면 두 번 송금되었습니다.

### 해결

HTTP `Idempotency-Key`와 `IdempotencyKey` Entity를 추가했습니다.

```text
동일 Key
  ↓
이미 처리됨
  ↓
송금 생략
```

---

## 4. 동시에 동일 Idempotency-Key 요청

### 문제

멱등성 검사를 Lock 이전에 수행하면 두 요청이 모두 `Key 없음`을 확인할 수 있었습니다.

```text
A → Key 없음
B → Key 없음
```

두 요청 모두 송금을 시도한 뒤 DB UNIQUE 제약에서 충돌했습니다.

### 해결

순서를 변경했습니다.

```text
Pessimistic Lock
  ↓
Idempotency-Key 검사
  ↓
송금
```

이를 통해 동일 출금 계좌의 동시 요청에서도 멱등성 검사가 순차적으로 실행되도록 했습니다.

---

## 5. 동일 Key의 다른 요청 재사용

### 문제

Key 존재 여부만으로 중복 요청을 판단하면 요청 내용이 달라도 같은 요청으로 판단합니다.

### 해결

IdempotencyKey에 요청 정보를 저장하여 기존 요청과 비교합니다.

```text
key
fromAccountId
toAccountId
amount
```

내용이 다르면:

```text
400 Bad Request
IDEMPOTENCY_KEY_CONFLICT
```

를 반환하도록 구현했습니다.

---

## 6. 멱등성 재요청의 응답 일관성

### 문제

초기 멱등성 구현에서는 동일한 요청이 다시 들어오면 실제 송금은 막았지만 단순히 처리를 종료했습니다.

```text
첫 요청
→ 송금 실행
→ Transfer 생성

재요청
→ 송금 실행 X
→ return
```

이 방식은 중복 송금은 방지하지만 클라이언트가 최초 요청에서 생성된 거래를 다시 식별하기 어렵다는 문제가 있습니다.

### 해결

`IdempotencyKey`에 저장된 `transferId`를 이용하여 동일 요청 재시도 시 최초 거래 ID를 반환하도록 개선했습니다.

```text
첫 요청
→ Transfer 생성
→ transferId = 1
→ IdempotencyKey에 transferId 저장

재요청
→ 동일 Key 확인
→ 요청 내용 비교
→ 송금 재실행 X
→ 기존 transferId = 1 반환
```

이를 통해 동일한 요청에 대해 실제 상태 변경뿐 아니라 반환 결과도 일관되게 유지하도록 개선했습니다.

---

# 학습 진행 상황

## Day 1 — Spring MVC

* Controller
* Service
* REST API
* JSON
* IntelliJ `.http`

## Day 2 — JPA + H2

* Entity
* Repository
* Spring Data JPA
* Hibernate
* H2

## Day 3 — Entity 관계와 입금

* User / Account
* `@ManyToOne`
* 도메인 메서드
* Transaction
* Dirty Checking

## Day 4 — 송금

* 계좌 간 송금
* Transaction
* Rollback
* 동일 계좌 검증

## Day 5 — 예외 처리

* `BusinessException`
* `ErrorCode`
* `GlobalExceptionHandler`
* 공통 ErrorResponse
* Validation

## Day 6 — DTO

* Request DTO
* Response DTO
* Java `record`
* Entity / API 분리

## Day 7 — 송금 이력

* Transfer Entity
* TransferRepository
* 거래 이력 저장
* 거래내역 조회
* SENT / RECEIVED
* TransferType Enum

## Day 8 — 테스트와 Transaction

* Service Mock Test
* ArgumentCaptor
* Transaction Rollback Test
* Repository 저장 실패 상황 검증

## Day 9 — 동시성

* ExecutorService
* CountDownLatch
* AtomicInteger
* Lost Update 재현
* Pessimistic Lock
* Optimistic Lock 비교

## Day 10 — 멱등성

* Idempotency-Key
* IdempotencyKey Entity
* UNIQUE Constraint
* 중복 요청 방지
* 동시 중복 요청 테스트
* Lock 순서 개선
* 동일 Key 재사용 충돌 검증

## Day 11 — 송금 응답과 멱등성 결과 일관성

* 송금 Service에서 `transferId` 반환
* 동일 멱등성 요청 재시도 시 기존 `transferId` 반환
* 최초 요청과 재요청의 `transferId` 동일성 테스트
* `TransferCreateResponse` DTO 추가
* 송금 성공 응답 JSON 구조화
* `transferId`, `status` 응답
* MockMvc 기반 Controller 응답 테스트
* 실제 `.http` 요청을 통한 JSON 응답 확인

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

### JPA Entity

DB 데이터뿐 아니라 도메인의 상태와 상태 변경 규칙을 표현합니다.

### Transaction

여러 DB 작업을 하나의 논리적인 작업으로 관리합니다.

### Dirty Checking

영속 상태 Entity의 변경을 Hibernate가 감지하여 UPDATE SQL을 실행합니다.

### Rollback

송금 중 하나의 작업이라도 실패하면 Transaction 전체 변경을 취소하여 데이터 정합성을 보호합니다.

### Pessimistic Lock

DB Row에 Lock을 획득해 동시에 동일 데이터를 수정하지 못하도록 제어합니다.

### Optimistic Lock

Version 값을 이용해 변경 시점에 동시 수정 여부를 감지합니다.

### Idempotency

동일한 요청이 반복되더라도 시스템의 실제 상태 변경은 한 번만 발생하도록 합니다.

PayProcess에서는 동일한 `Idempotency-Key`와 동일한 송금 요청이 다시 들어오면 실제 송금을 다시 수행하지 않습니다.

또한 최초 송금에서 생성된 `transferId`를 저장해 두었다가 재요청에서도 동일한 값을 반환합니다.

```text
첫 요청
→ 송금 실행
→ transferId = 1

재요청
→ 송금 실행 X
→ transferId = 1 반환
```

따라서 중복 상태 변경 방지뿐 아니라 재시도 요청에 대한 결과 일관성까지 유지합니다.

### Database Constraint

Application 코드뿐 아니라 DB의 UNIQUE 제약을 함께 사용해 데이터 무결성을 보호합니다.

---

# 앞으로 구현할 기능

## 1. 거래 상태 관리

현재 송금 성공 응답에서는 다음과 같이 처리 결과를 반환합니다.

```json
{
  "transferId": 1,
  "status": "SUCCESS"
}
```

현재 `SUCCESS`는 API 응답 단계에서 사용하고 있습니다.

다음 단계에서는 `Transfer` 자체가 거래 상태를 관리하도록 확장할 예정입니다.

```text
PENDING
SUCCESS
FAILED
```

이를 통해 송금의 처리 상태를 Database에 저장하고 거래 상태 변화를 관리할 수 있는 구조로 발전시킬 예정입니다.

---

## 2. 거래 조회 고도화

* Pagination
* 기간별 거래내역 조회
* 거래 상세 조회
* 조건 검색

---

## 3. Database 전환

현재 H2 기반 구조를 MySQL 또는 PostgreSQL 환경으로 이전하여 실제 DB 환경에서 다음 내용을 다시 검증할 예정입니다.

* Transaction
* Lock
* Isolation Level
* 동시성
* Constraint

---

## 4. Kafka 비동기 처리

송금 Transaction과 직접 관련이 없는 후속 처리를 이벤트 기반으로 분리할 예정입니다.

```text
송금 성공
   ↓
Transaction Commit
   ↓
TransferCompleted Event
   ↓
Kafka
   ├── 알림
   ├── 로그
   └── 기타 후속 작업
```

핵심 송금 Transaction과 부가 기능을 분리하여 서비스 확장 구조를 학습하는 것을 목표로 합니다.

---

## 5. Docker

Application과 Database 실행 환경을 Container 기반으로 구성할 예정입니다.

---

# 최종 목표 구조

```text
Client
  ↓
Spring Boot API
  ↓
Transfer Service
  ├── Transaction
  ├── Pessimistic Lock
  └── Idempotency
  ↓
Database
  ↓
Transaction Commit
  ↓
Event
  ↓
Kafka
  ├── Notification
  ├── Logging
  └── Async Processing
```

PayProcess는 기능 개수를 늘리는 것보다 **실제 송금 시스템에서 왜 Transaction, Lock, Idempotency가 필요한지를 문제 재현과 테스트를 통해 이해하고 구현하는 것**을 목표로 합니다.
