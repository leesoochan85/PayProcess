# PayProcess

실시간 송금 및 결제 시스템을 목표로 개발 중인 **Java / Spring Boot 기반 사이드 프로젝트**입니다.

단순 CRUD 구현에 그치지 않고, 송금·결제 시스템에서 발생할 수 있는 트랜잭션, 동시성, 장애 복구, 비동기 처리 등의 문제를 단계적으로 학습하고 해결하는 것을 목표로 합니다.

현재는 Spring Boot와 JPA를 기반으로 사용자, 계좌, 입금, 계좌 간 송금, 송금 이력 저장 및 거래내역 조회를 구현했으며, 예외 처리 구조화, Bean Validation, Controller/Service 자동 테스트, 실제 H2 기반 Transaction Rollback 통합 테스트와 **동시 송금 상황의 Lost Update 재현 및 비관적 락(Pessimistic Lock)을 이용한 동시성 제어**까지 진행했습니다.

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
* Docker
* 비동기 이벤트 처리
* 멱등성 처리 및 중복 요청 방지

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
* 멱등성 및 중복 요청 방지
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
출금 Account 비관적 락 조회
  ↓
입금 Account 조회
  ↓
출금 계좌 withdraw()
  ↓
입금 계좌 deposit()
  ↓
Transfer Entity 생성
  ↓
TransferRepository.save()
  ↓
Dirty Checking
  ↓
COMMIT / ROLLBACK
  ↓
출금 Account Lock 해제
```

출금 계좌 조회 시 `PESSIMISTIC_WRITE` 락을 사용해 같은 계좌에서 동시에 여러 송금 요청이 들어왔을 때 잔액 정합성이 깨지는 문제를 방지합니다.

---

# 패키지 구조

현재 프로젝트는 기능 단위로 패키지를 구분하고 있습니다.

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
│   ├── AccountTransferResponse.java
│   └── TransferType.java
│
└── exception
    ├── BusinessException.java
    ├── ErrorCode.java
    ├── ErrorResponse.java
    └── GlobalExceptionHandler.java
```

현재 `transfer` 패키지에서는 DB 저장용 Entity와 API 응답용 DTO를 분리하고 있습니다.

```text
Transfer Entity
  ↓
TransferResponse
  ↓
전체 / 보낸 / 받은 거래내역 응답

Transfer Entity
  ↓
AccountTransferResponse
  ↓
특정 계좌 관점 거래내역 응답
  ├── SENT
  └── RECEIVED
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

```text
User
  │
  │ 1 : N
  ▼
Account
```

---

## 3. 계좌 조회

현재 생성된 계좌들을 조회할 수 있습니다.

```text
GET /api/accounts
```

Entity를 API 응답에 직접 노출하면 Hibernate 내부 정보가 함께 노출될 수 있기 때문에 Response DTO를 사용하는 방향으로 구조를 분리합니다.

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

입금 로직은 `Account` Entity 내부에서 처리합니다.

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

## 6. 송금 이력 저장 및 거래내역 조회

송금이 성공하면 계좌 잔액만 변경하는 것이 아니라 `Transfer` Entity를 생성하여 거래 이력을 DB에 저장합니다.

```text
송금 요청
  ↓
출금 계좌 차감
  ↓
입금 계좌 증가
  ↓
Transfer 이력 저장
  ↓
COMMIT
```

주요 저장 정보:

```text
Transfer
├── id
├── fromAccountId
├── toAccountId
├── amount
└── createdAt
```

송금 이력 저장도 송금과 동일한 `@Transactional` 범위 안에서 수행되므로 거래 이력 저장 중 예외가 발생하면 계좌 잔액 변경까지 함께 Rollback됩니다.

### 전체 송금 이력 조회

```text
GET /api/transfers
```

### 보낸 송금 이력 조회

```text
GET /api/transfers/sent/{accountId}
```

### 받은 송금 이력 조회

```text
GET /api/transfers/received/{accountId}
```

### 특정 계좌의 전체 거래내역 조회

```text
GET /api/transfers/accounts/{accountId}
```

특정 계좌 기준 조회에서는 같은 거래라도 조회 계좌의 관점에 따라 `SENT` 또는 `RECEIVED`로 표현합니다.

예를 들어:

```text
1번 계좌 → 3번 계좌
1,000원 송금
```

1번 계좌 기준:

```json
{
  "transferId": 1,
  "type": "SENT",
  "counterAccountId": 3,
  "amount": 1000
}
```

3번 계좌 기준:

```json
{
  "transferId": 1,
  "type": "RECEIVED",
  "counterAccountId": 1,
  "amount": 1000
}
```

`SENT`, `RECEIVED`는 문자열 대신 `TransferType` enum으로 관리합니다.

```java
public enum TransferType {
    SENT,
    RECEIVED
}
```

이를 통해 거래 유형을 정해진 값으로 제한하고 문자열 오타 가능성을 줄였습니다.

---

# Transaction

송금은 출금과 입금, 거래 이력 저장까지 하나의 논리적인 작업으로 처리해야 합니다.

```text
출금
+
입금
+
거래 이력 저장
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

정상 완료 시 `COMMIT`, 중간에 예외가 발생하면 `ROLLBACK`되어 일부 작업만 DB에 반영되는 것을 방지합니다.

`@Transactional`은 하나의 송금 작업의 원자성을 보장하지만, 서로 다른 트랜잭션이 같은 계좌를 동시에 조회하고 수정하는 문제까지 자동으로 해결하지는 않습니다. 해당 문제는 별도의 동시성 테스트를 통해 재현하고 비관적 락으로 해결했습니다.

---

# Dirty Checking

송금 과정에서는 Account에 별도의 `save()`를 호출하지 않고 Entity의 상태만 변경합니다.

```java
fromAccount.withdraw(amount);
toAccount.deposit(amount);
```

JPA가 관리하고 있는 Entity의 값이 변경되면 트랜잭션 종료 시점에 변경 내용을 감지하고 필요한 `UPDATE` SQL을 실행합니다.

이를 JPA의 **Dirty Checking(변경 감지)** 이라고 합니다.

---

# Rollback 테스트

`@Transactional`의 실제 동작을 확인하기 위해 **Spring Boot + JPA + H2 기반 통합 테스트**를 작성했습니다.

송금 흐름 중 후반부인 거래 이력 저장 단계에서 의도적으로 예외를 발생시키고, 이미 실행된 출금/입금 변경까지 모두 Rollback되는지 검증했습니다.

```text
출금 계좌: 10,000원
입금 계좌:  5,000원

3,000원 송금 시작
   ↓
출금 계좌: 7,000원
입금 계좌: 8,000원
   ↓
Transfer 이력 저장 중 예외 발생
   ↓
ROLLBACK
   ↓
출금 계좌: 10,000원
입금 계좌:  5,000원
```

테스트에서는 변경된 Entity 객체만 확인하지 않고 DB에서 계좌를 다시 조회하여 실제 저장 상태가 원래 잔액으로 복구되었는지 검증했습니다.

이를 통해 송금의 다음 세 작업이 하나의 원자적인 Transaction으로 처리되는 것을 확인했습니다.

```text
출금
+
입금
+
송금 이력 저장
```

---

# 동시성 제어

같은 출금 계좌에서 동시에 여러 송금 요청이 발생할 때 잔액 정합성이 유지되는지 검증하기 위해 `TransferConcurrencyTest`를 작성했습니다.

## 동시성 문제 재현

초기 상태:

```text
출금 계좌: 10,000원
입금 계좌 1: 0원
입금 계좌 2: 0원
```

두 Thread가 같은 출금 계좌에서 동시에 각각 8,000원씩 송금하도록 구성했습니다.

```text
Thread 1: 출금 계좌 → 입금 계좌 1, 8,000원
Thread 2: 출금 계좌 → 입금 계좌 2, 8,000원
```

락이 없는 상태에서는 두 트랜잭션이 모두 출금 계좌의 잔액을 10,000원으로 읽고 각각 송금 가능하다고 판단했습니다.

실제 테스트 결과:

```text
출금 계좌 잔액: 2,000원
입금 계좌 1 잔액: 8,000원
입금 계좌 2 잔액: 8,000원

초기 총액: 10,000원
최종 총액: 18,000원
```

출금 계좌는 두 트랜잭션이 각각 `10,000 - 8,000 = 2,000`으로 계산한 값을 저장하면서 한쪽 UPDATE가 다른 UPDATE를 덮어쓰고, 서로 다른 입금 계좌에는 각각 8,000원이 반영되는 **Lost Update 계열의 데이터 정합성 문제**가 발생했습니다.

이를 통해 `@Transactional`만으로는 여러 트랜잭션이 동시에 같은 데이터를 수정하는 문제를 방지할 수 없음을 확인했습니다.

## 동시성 테스트 구성

Java의 동시성 도구를 이용해 실제로 여러 Thread에서 송금을 수행했습니다.

* `ExecutorService`
* `CountDownLatch`
* `AtomicInteger`

테스트에서는 다음 조건을 자동 검증합니다.

```text
성공한 송금 요청 = 1
실패한 송금 요청 = 1
출금 계좌 최종 잔액 = 2,000원
두 입금 계좌의 잔액 합계 = 8,000원
전체 계좌 잔액 합계 = 10,000원
```

## 비관적 락 적용

출금 계좌 조회 시 `PESSIMISTIC_WRITE`를 적용한 별도의 Repository 메서드를 사용합니다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select a from Account a where a.id = :id")
Optional<Account> findByIdWithLock(@Param("id") Long id);
```

`TransferService`에서는 송금 시 출금 계좌를 다음 방식으로 조회합니다.

```java
Account fromAccount = accountRepository.findByIdWithLock(fromAccountId)
        .orElseThrow(() ->
                new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND)
        );
```

실제 Hibernate SQL에서도 다음과 같이 `FOR UPDATE`가 실행되는 것을 확인했습니다.

```sql
select ...
from accounts
where id = ?
for update
```

동작 흐름:

```text
Thread 1
  ↓
출금 Account 조회 + Row Lock 획득
  ↓
잔액 10,000원 확인
  ↓
8,000원 송금
  ↓
COMMIT
  ↓
Lock 해제

Thread 2
  ↓
같은 출금 Account Lock 대기
  ↓
Thread 1 COMMIT 이후 조회
  ↓
최신 잔액 2,000원 확인
  ↓
8,000원 송금 불가
  ↓
INSUFFICIENT_BALANCE
```

비관적 락 적용 후 실제 결과:

```text
출금 계좌 잔액: 2,000원
입금 계좌 1 잔액: 8,000원
입금 계좌 2 잔액: 0원
```

또는 두 Thread의 실행 순서에 따라 입금 계좌 1과 입금 계좌 2의 결과가 반대로 나타날 수 있습니다.

어느 Thread가 먼저 실행되더라도 다음 불변조건은 유지됩니다.

```text
성공 요청 = 1
실패 요청 = 1
전체 잔액 = 10,000원
```

## 낙관적 락 비교 실험

비관적 락과의 차이를 확인하기 위해 `Account`에 임시로 `@Version`을 추가해 낙관적 락도 실험했습니다.

```java
@Version
private Long version;
```

낙관적 락에서는 두 Thread가 동시에 같은 잔액과 version을 조회하는 것은 허용됩니다.

대신 UPDATE 시 version이 조건에 포함됩니다.

```sql
update accounts
set ..., version = ?
where id = ?
  and version = ?
```

한 트랜잭션이 먼저 UPDATE하여 version을 증가시키면, 다른 트랜잭션이 이전 version으로 UPDATE할 때 수정되는 row가 0건이 되어 충돌이 감지됩니다.

실제 테스트에서는 다음 예외를 확인했습니다.

```text
org.springframework.orm.ObjectOptimisticLockingFailureException
```

### 비관적 락과 낙관적 락 비교

| 구분 | 비관적 락 | 낙관적 락 |
| --- | --- | --- |
| 핵심 방식 | 조회 시 DB Row Lock 획득 | 수정 시 version 비교 |
| 대표 SQL | `SELECT ... FOR UPDATE` | `UPDATE ... WHERE version = ?` |
| 충돌 시 | 다른 트랜잭션이 대기 | 충돌 예외 발생 |
| 장점 | 충돌이 많은 상황에서 처리 흐름이 명확함 | 충돌이 적을 때 Lock 대기 비용을 줄일 수 있음 |
| 단점 | Lock 대기 및 성능 저하 가능 | 충돌 시 재시도 정책 필요 |
| 이번 프로젝트 | **최종 채택** | 비교 실험 |

PayProcess의 송금에서는 같은 출금 계좌에 대한 동시 수정이 발생했을 때 잔액 정합성을 우선 보장하고, 대기 후 최신 잔액을 기준으로 잔액 부족 여부를 다시 판단할 수 있도록 **비관적 락을 최종 방식으로 선택했습니다.**

낙관적 락 실험을 위해 추가했던 `@Version` 필드는 최종 구현에서는 제거합니다.

---

# 송금 Validation

송금 요청 검증은 **API 입력값 검증**과 **비즈니스 규칙 검증**으로 역할을 분리했습니다.

## API 입력값 검증

`TransferRequest`에 Bean Validation을 적용하고 Controller에서 `@Valid`로 검증합니다.

```java
public record TransferRequest(
        @NotNull(message = "출금계좌 ID는 필수입니다.")
        Long fromAccountId,

        @NotNull(message = "입금계좌 ID는 필수입니다.")
        Long toAccountId,

        @NotNull(message = "송금금액은 필수입니다.")
        @Positive(message = "송금금액은 0보다 커야합니다.")
        Long amount
) {
}
```

검증 예시:

```text
amount = -1000
→ 400 INVALID_REQUEST

amount 누락
→ 400 INVALID_REQUEST

fromAccountId 누락
→ 400 INVALID_REQUEST
```

## 비즈니스 규칙 검증

요청 형식 자체는 올바르지만 실제 송금 규칙을 위반한 경우 Service / Domain 계층에서 검증합니다.

* 동일 계좌 송금 방지
* 존재하지 않는 출금 계좌
* 존재하지 않는 입금 계좌
* 잔액 부족
* 0 이하 금액에 대한 Domain 자체 방어

```text
Controller / Request DTO
├── null 여부
├── 양수 여부
└── 기본 요청 형식

Service / Domain
├── 계좌 존재 여부
├── 동일 계좌 송금 여부
├── 잔액 부족 여부
└── 도메인 상태 변경 규칙
```

---

# DTO

API 계층과 Entity를 분리하기 위해 DTO 구조를 적용하고 있습니다.

DTO는 **Data Transfer Object**로 API 요청 및 응답 데이터를 전달하기 위한 객체입니다.

```text
JSON
 ↓
Request DTO
 ↓
Controller
 ↓
Service
```

응답은 다음과 같이 변환합니다.

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

* `TransferResponse` — 거래 자체의 정보를 표현
* `AccountTransferResponse` — 특정 계좌 관점의 거래정보를 표현

이를 통해 DB Entity 구조와 외부 API 명세를 분리할 수 있습니다.

---

# 예외 처리 구조

초기에는 `IllegalArgumentException`으로 여러 오류를 처리했지만, 비즈니스 예외가 늘어날수록 오류 종류를 구분하기 어려워지는 문제가 있었습니다.

이를 다음 구조로 리팩토링했습니다.

```text
BusinessException
        │
        ▼
ErrorCode
├── ACCOUNT_NOT_FOUND
├── INSUFFICIENT_BALANCE
├── SAME_ACCOUNT_TRANSFER
└── INVALID_AMOUNT
        │
        ▼
GlobalExceptionHandler
        │
        ▼
ErrorResponse
```

`ErrorCode` enum에 HTTP Status, 오류 코드, 메시지를 함께 정의하여 예외 정보를 한곳에서 관리합니다.

예:

```text
ACCOUNT_NOT_FOUND
→ 404
→ "ACCOUNT_NOT_FOUND"
→ "계좌가 존재하지 않습니다."

INSUFFICIENT_BALANCE
→ 400
→ "INSUFFICIENT_BALANCE"
→ "잔액이 부족합니다."
```

비즈니스 로직에서는 다음과 같이 공통 예외를 사용합니다.

```java
throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
```

`GlobalExceptionHandler`에서는 `BusinessException`을 받아 `ErrorCode`에 정의된 상태 코드와 메시지를 API 응답으로 변환합니다.

Bean Validation 실패는 `MethodArgumentNotValidException`을 처리하여 다음 형태로 응답합니다.

```json
{
  "code": "INVALID_REQUEST",
  "message": "송금금액은 0보다 커야합니다."
}
```

이를 통해 API 입력 오류와 비즈니스 규칙 위반을 분리하고 일관된 ErrorResponse를 제공하도록 구성했습니다.

---

# Enum

정해진 상태값을 문자열 대신 타입으로 관리합니다.

```text
TransferType
├── SENT
└── RECEIVED
```

`"SNET"`이나 `"RECIEVED"` 같은 문자열 오타를 방지하고 허용되는 상태를 코드 수준에서 명확하게 제한할 수 있습니다.

---

# API

현재까지 구현한 주요 API입니다.

| Method | Endpoint | 기능 |
| ------ | -------- | ---- |
| POST | `/api/users` | 사용자 생성 |
| POST | `/api/accounts` | 계좌 생성 |
| GET | `/api/accounts` | 계좌 목록 조회 |
| POST | `/api/accounts/{accountId}/deposit` | 계좌 입금 |
| POST | `/api/transfers` | 계좌 간 송금 |
| GET | `/api/transfers` | 전체 송금 이력 조회 |
| GET | `/api/transfers/sent/{accountId}` | 특정 계좌가 보낸 송금 조회 |
| GET | `/api/transfers/received/{accountId}` | 특정 계좌가 받은 송금 조회 |
| GET | `/api/transfers/accounts/{accountId}` | 특정 계좌의 전체 거래내역 조회 |

API는 IntelliJ의 `.http` 파일을 이용하여 테스트하고 있습니다.

---

# 학습 진행 상황

## Day 1 — Spring MVC 기본 구조

* Controller
* Service
* REST API
* JSON 요청/응답
* IntelliJ `.http` 파일을 통한 API 테스트

---

## Day 2 — JPA + H2

* Spring Data JPA
* Entity
* Repository
* H2 Database
* Hibernate

---

## Day 3 — Entity 연관관계와 입금

* User / Account Entity
* Entity 연관관계
* `@ManyToOne`
* 입금 도메인 로직
* `@Transactional`
* Dirty Checking

---

## Day 4 — 계좌 간 송금

* 계좌 간 송금
* Transaction
* Dirty Checking
* Rollback
* 도메인 메서드
* 동일 계좌 송금 검증

---

## Day 5 — 예외 처리

* 잘못된 송금 요청 처리
* 잔액 부족
* 존재하지 않는 계좌
* 동일 계좌 송금
* HTTP 오류 응답 구조
* `@RestControllerAdvice`
* `GlobalExceptionHandler`
* `BusinessException`
* `ErrorCode` enum
* `ErrorResponse`
* 비즈니스 예외 공통 구조 리팩토링

---

## Day 6 — DTO와 Entity 분리

* Request DTO
* Response DTO
* Java `record`
* Entity 직접 노출의 문제점
* Entity → DTO 변환

---

## Day 7 — 송금 이력 및 거래내역 조회

* `Transfer` Entity 설계
* `TransferRepository`
* 송금 성공 시 거래 이력 저장
* 송금과 이력 저장을 하나의 Transaction으로 처리
* Spring Data JPA Query Method
* `Or` 조건 조회
* `OrderBy...Desc` 최신순 정렬
* `TransferResponse`
* `AccountTransferResponse`
* Stream `map()`을 이용한 Entity → DTO 변환
* 특정 계좌 관점의 `SENT` / `RECEIVED` 구분
* `TransferType` enum 리팩토링

거래 저장:

```text
TransferService
    ↓
@Transactional
    ├── Account 잔액 변경
    └── Transfer 이력 저장
```

거래 조회:

```text
TransferRepository
    ↓
Transfer Entity
    ↓
Service
    ↓
Response DTO
    ↓
Controller
    ↓
JSON
```

특정 계좌 거래내역은 다음 조건으로 조회합니다.

```text
fromAccountId = accountId
OR
toAccountId = accountId
    ↓
createdAt DESC
```

---

## Day 8 — Validation과 자동 테스트

### Bean Validation

* `@Valid`
* `@NotNull`
* `@Positive`
* `MethodArgumentNotValidException`
* API 입력 검증과 비즈니스 검증 역할 분리

### Controller 테스트

`MockMvc`를 이용해 HTTP 요청과 오류 응답을 자동 검증했습니다.

검증 항목:

* 음수 송금 금액 → `400 INVALID_REQUEST`
* 송금 금액 누락 → `400 INVALID_REQUEST`
* 출금 계좌 ID 누락 → `400 INVALID_REQUEST`
* 존재하지 않는 계좌 → `404 ACCOUNT_NOT_FOUND`
* 동일 계좌 송금 → `400 SAME_ACCOUNT_TRANSFER`
* 잔액 부족 → `400 INSUFFICIENT_BALANCE`

### Service 단위 테스트

Mockito를 사용해 실제 `TransferService` 비즈니스 로직을 검증했습니다.

* 정상 송금 시 출금/입금 잔액 변경
* 송금 이력 저장 호출
* 존재하지 않는 계좌 예외
* 동일 계좌 송금 예외
* 잔액 부족 예외
* 실패한 송금에서 거래 이력이 저장되지 않는지 검증

### Transaction 통합 테스트

`@SpringBootTest`와 실제 H2/JPA 환경에서 송금 후반부에 예외를 발생시켜 Transaction Rollback을 검증했습니다.

```text
TransferService.transfer()
        ↓
Account 조회
        ↓
withdraw()
        ↓
deposit()
        ↓
TransferRepository.save()
        ↓
예외 발생
        ↓
ROLLBACK
        ↓
DB 계좌 잔액 원복 확인
```

이를 통해 단순 Java 로직뿐 아니라 실제 Spring Transaction 경계에서도 송금 원자성이 유지되는지 확인했습니다.

---

## Day 9 — 동시성 문제 재현과 비관적 락

### 동시성 문제 재현

* `ExecutorService`
* `CountDownLatch`
* `AtomicInteger`
* 같은 출금 계좌에 대한 동시 송금 테스트
* Lost Update 계열의 잔액 정합성 문제 재현
* 락이 없는 상태에서 초기 총액 10,000원이 18,000원으로 증가하는 비정상 상태 확인

### 비관적 락

* `@Lock(LockModeType.PESSIMISTIC_WRITE)`
* `SELECT ... FOR UPDATE`
* 같은 출금 계좌에 대한 동시 수정 직렬화
* 두 요청 중 1건 성공 / 1건 잔액 부족 실패 검증
* 전체 잔액 10,000원 보존 확인

### 낙관적 락 비교 실험

* `@Version`
* version 조건이 포함된 UPDATE SQL 확인
* `ObjectOptimisticLockingFailureException` 발생 확인
* 비관적 락과 낙관적 락의 동작 방식 비교

최종적으로 PayProcess의 송금 처리에서는 **비관적 락을 채택**했습니다.

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

### Enum

정해진 상태값을 타입으로 제한하여 잘못된 문자열이나 오타를 방지합니다.

### Validation

Bean Validation을 이용해 Controller 진입 단계에서 잘못된 요청값을 차단하고, 비즈니스 규칙은 Service / Domain 계층에서 별도로 검증합니다.

### Exception Handling

`BusinessException + ErrorCode + GlobalExceptionHandler` 구조를 통해 API 오류 응답을 일관되게 관리합니다.

### Test

Controller, Service, Transaction, Concurrency 계층을 나누어 자동 테스트를 작성했습니다.

```text
Controller Test
→ HTTP / Validation / ErrorResponse

Service Test
→ 송금 비즈니스 로직

Transaction Integration Test
→ JPA / H2 / Rollback

Concurrency Test
→ 동시 송금 / Lost Update / Pessimistic Lock
```

### Concurrency Control

`@Transactional`만으로 해결되지 않는 여러 트랜잭션 간 동시 수정 문제를 테스트로 재현했습니다.

출금 계좌 조회 시 `PESSIMISTIC_WRITE`를 적용하여 같은 계좌를 동시에 수정하려는 트랜잭션을 순차적으로 처리하고 잔액 정합성을 유지합니다.

---

# 앞으로 구현할 기능

## 1. 멱등성 및 중복 송금 방지

네트워크 재시도나 사용자의 중복 클릭으로 동일한 송금 요청이 여러 번 전달되어도 실제 송금은 한 번만 처리되도록 멱등성 구조를 학습하고 구현할 예정입니다.

검토 항목:

* Idempotency Key
* 중복 요청 식별
* 중복 송금 방지
* 요청 처리 결과 재사용
* 동시 중복 요청 처리

## 2. 테스트 코드 확장

현재 Controller / Service / Transaction Rollback / Concurrency 테스트를 작성했습니다.

향후 다음 테스트를 추가할 예정입니다.

* Repository 테스트
* 성공/실패 Transaction 통합 테스트 확대
* 동시성 테스트 케이스 확대
* 예외 응답 케이스 확대
* 멱등성 테스트

## 3. DTO 구조 확장

현재 송금 영역에는 Request/Response DTO와 ErrorResponse 구조를 적용했습니다.

향후 다음 영역까지 일관되게 확장할 예정입니다.

* AccountResponse
* UserResponse

## 4. 송금 이력 고도화

기본 거래 이력 저장 및 조회는 구현했습니다.

향후 다음 기능을 추가할 예정입니다.

* 거래 상태 관리
* Pagination
* 기간별 거래 조회
* 거래 상세 조회
* 필요 시 Entity 연관관계 적용 여부 검토

## 5. Kafka

송금 핵심 Transaction과 직접 관련 없는 후속 작업을 이벤트 기반으로 분리할 예정입니다.

```text
송금 성공
   ↓
Transaction Commit
   ↓
TransferCompleted Event
   ↓
Kafka
   ├── 알림
   ├── 거래 이력 후속 처리
   └── 기타 비동기 이벤트
```

## 6. 운영 환경 확장

* MySQL 또는 PostgreSQL 적용
* Docker 기반 실행 환경 구성
* 운영 환경 설정 분리
* 장애 복구 전략 검토

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
