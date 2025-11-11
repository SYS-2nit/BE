# 알림 기능 설계 문서

## 0. 용어 및 기술 정리

### 0.1 용어 정리

- **SSE (Server-Sent Events)**: 서버에서 클라이언트로 실시간 데이터를 전송하는 웹 기술. 웹소켓과 달리 단방향 통신(서버→클라이언트)이며, HTTP 연결을 통해 이벤트 스트림을 전송

- **하트비트 (Heartbeat)**: 연결이 살아있는지 확인하기 위해 주기적으로 보내는 신호. SSE 연결 유지를 위해 30초마다 빈 메시지를 전송

- **비트마스크 (Bitmask)**: 여러 옵션을 하나의 숫자로 표현하는 방법. 예: 요일 선택 시 1=일, 2=월, 4=화, 8=수, 16=목, 32=금, 64=토 → 127(1+2+4+8+16+32+64) = 모든 요일

- **웹훅 (Webhook)**: 외부 서비스(슬랙 등)에 HTTP POST 요청을 보내 알림을 전달하는 방식

- **임계값 (Threshold)**: 알림을 발생시키는 기준값. 예: CPU 사용률 80% 이상이면 위험 알림

- **누적 시간 옵션 (Delay Time)**: 연속으로 임계값을 초과한 횟수가 지정된 시간 동안 지속되어야 알림이 발생하도록 하는 옵션. 메트릭 수집 주기가 1분이므로 최소 누적 시간은 1분입니다.
  - '1M' = 1분 (1회 연속 초과)
  - '5M' = 5분 (5회 연속 초과)
  - '10M' = 10분 (10회 연속 초과)
  - '1H' = 1시간 (60회 연속 초과)

- **델타 (Delta) 계산**: 이전 값과 현재 값의 차이를 계산하는 방식. 메트릭 수집 시 누적값의 증가량을 계산하기 위해 사용

### 0.2 사용 기술 설명

- **Spring Boot @Scheduled**: 주기적인 작업을 자동 실행하기 위한 어노테이션. `@Scheduled(fixedRate = 60000)` 형태로 1분마다 메트릭 수집 작업 실행

- **Spring SseEmitter**: Spring Framework에서 제공하는 SSE 구현 클래스. 서버에서 클라이언트로 실시간 이벤트를 전송하기 위해 사용. `SseEmitter emitter = new SseEmitter(timeout)` 형태로 생성

- **JavaMailSender (Spring Mail)**: 이메일 전송을 위한 Spring의 인터페이스. SMTP 서버를 통해 이메일을 발송. `MimeMessage`를 생성하여 `send()` 메서드로 전송

- **RestTemplate / WebClient**: HTTP 클라이언트 라이브러리. 슬랙 웹훅 URL로 POST 요청을 보내 알림을 전달하기 위해 사용. Spring 5 이상에서는 `WebClient` 사용 권장

- **비트마스크 연산**: 요일 체크를 위해 비트 연산 사용. 예: `(days & (1 << dayOfWeek)) != 0` 형태로 특정 요일이 포함되어 있는지 확인

- **ConcurrentHashMap**: 멀티스레드 환경에서 안전하게 사용할 수 있는 Map 구현체. AlertStateStore에서 상태 저장 시 사용

- **Optional**: null 처리를 안전하게 하기 위한 Java 8의 클래스. 값이 있을 수도 없을 수도 있는 경우를 표현

---

## 1. 개요

### 1.1 목적

Oracle DB 모니터링 시스템에서 메트릭이 임계값을 초과할 때 사용자에게 실시간으로 알림을 전송하는 기능을 제공합니다.

### 1.2 주요 특징

- **실시간 알림**: SSE를 통한 실시간 알림 전송
- **다중 채널**: SSE, 이메일, Slack을 통한 알림 전송
- **사용자 맞춤 설정**: 사용자별 알림 정책 생성 및 관리 (정책 생성자만 알림 수신)
- **심각도 관리**: WARNING(1), DANGER(2), CRITICAL(3) 세 단계 심각도
- **누적 시간 옵션**: 연속 임계값 초과 시에만 알림 발생 (1M, 5M, 10M, 1H)
- **히스토리 관리**: 알림 발생 이력 및 처리 이력 관리

### 1.3 범위

- CPU, Memory, Session, I/O, Storage 카테고리별 메트릭 모니터링
- 각 카테고리당 3~5개의 알림 메트릭 지원
- 0~100% 범위의 메트릭만 지원 (정규화/델타 계산 불필요)

---

## 2. 기능 요구사항

### 2.1 알림 정책 관리

- 사용자는 인스턴스별로 알림 정책을 생성/수정/삭제할 수 있음
- 하나의 정책에 여러 알림 규칙을 포함할 수 있음
- 정책 활성화/비활성화 기능

### 2.2 알림 규칙 설정

- **카테고리 선택**: CPU, Memory, Session, I/O, Storage 중 선택
- **그래프 연결**: 각 알림은 하나의 그래프와 1:1 매핑 (카테고리별 범위 내)
- **메트릭 선택**: 해당 그래프에서 사용하는 메트릭 선택
- **임계값 설정**: WARNING, DANGER, CRITICAL 각각 0~100% 범위로 설정
- **누적 시간**: 1M (1분), 5M (5분), 10M (10분), 1H (1시간)
- **요일/시간대 설정**: 알림 수신 요일(비트마스크) 및 시간대 설정

### 2.3 알림 체크

- 메트릭 수집 후 자동으로 알림 조건 체크
- 임계값 초과 시 알림 이벤트 생성
- 누적 시간 옵션에 따른 연속 초과 확인
- 중복 알림 방지 (동일 조건 재발생 시 무시)

### 2.4 알림 전송

- **SSE**: 실시간 웹 알림 (하트비트 포함)
- **이메일**: JavaMailSender를 통한 이메일 전송
- **Slack**: Webhook을 통한 Slack 메시지 전송
- 정책 생성자에게 전송 (정책 생성자의 이메일/슬랙 설정 사용)

### 2.5 알림 이력 관리

- 알림 발생 이력 조회
- 알림 확인(Acknowledge) 기능
- 알림 해결(Resolve) 기능
- 알림 처리 이력(Progress History) 관리

### 2.6 알림 수신

- 정책 생성자(ALERT_POLICY.member_id)만 해당 정책의 알림을 수신
- 정책 활성화/비활성화(ALERT_POLICY.is_active)로 알림 수신 제어
- 알림 수신 채널: SSE, 이메일, Slack (정책 생성자의 설정 사용)

---

## 3. 데이터베이스 설계

### 3.1 기존 테이블 수정

### MEMBER 테이블 (기존 - 수정 불필요)

**역할**: 사용자 정보를 저장하는 테이블. 알림 수신을 위한 이메일 및 슬랙 주소를 포함합니다.

- `slackAddress`, `warningChannel`, `criticalChannel` 필드가 이미 존재

### 3.2 기존 테이블 수정 사항

### 3.2.1 ALERT_EVENT 테이블 수정

**역할**: 알림 규칙을 정의하는 테이블. 각 알림 규칙은 하나의 그래프와 메트릭에 연결되며, 임계값(WARNING/DANGER/CRITICAL), 누적 시간, 요일/시간대 등의 조건을 포함합니다. 하나의 알림 정책(ALERT_POLICY)에 여러 알림 규칙이 포함될 수 있습니다.

**기존 구조를 최대한 활용하면서 다음 필드 추가/수정:**

```sql
-- 기존 ALERT_EVENT 테이블 수정
ALTER TABLE ALERT_EVENT 
ADD (
    POLICY_ID           NUMBER,
    GRAPH_ID            NUMBER            NOT NULL,
    METRIC_KEY          VARCHAR2(100)     NOT NULL,
    METRIC_NAME         VARCHAR2(200)     NOT NULL,
    IS_REVERSE          NUMBER(1)         DEFAULT 0 NOT NULL
);

-- 기존 필드 수정
ALTER TABLE ALERT_EVENT 
MODIFY (
    CATEGORY            VARCHAR2(20)      NOT NULL,  -- ENUM 대신 VARCHAR2 사용
    DELAY_TIME          VARCHAR2(20)      DEFAULT '1M' NOT NULL,  -- ENUM 대신 VARCHAR2
    DAYS                NUMBER(3)         DEFAULT 127 NOT NULL,  -- NUMBER(1) → NUMBER(3)
    START_TIME          VARCHAR2(5),  -- TIMESTAMP → VARCHAR2(5) (HH:mm 형식)
    END_TIME            VARCHAR2(5)   -- TIMESTAMP → VARCHAR2(5) (HH:mm 형식)
);

-- 기존 필드 제거 (INSTANCE_ID는 POLICY를 통해 접근)
ALTER TABLE ALERT_EVENT DROP COLUMN INSTANCE_ID;

-- 제약조건 추가
ALTER TABLE ALERT_EVENT 
ADD CONSTRAINT FK_ALERT_EVENT_POLICY FOREIGN KEY (POLICY_ID) REFERENCES ALERT_POLICY(ID) ON DELETE CASCADE;

ALTER TABLE ALERT_EVENT 
ADD CONSTRAINT FK_ALERT_EVENT_GRAPH FOREIGN KEY (GRAPH_ID) REFERENCES GRAPH(ID);

ALTER TABLE ALERT_EVENT 
ADD CONSTRAINT CK_ALERT_EVENT_CATEGORY CHECK (CATEGORY IN ('CPU', 'MEMORY', 'SESSION', 'IO', 'STORAGE'));

ALTER TABLE ALERT_EVENT 
ADD CONSTRAINT CK_ALERT_EVENT_DELAY_TIME CHECK (DELAY_TIME IN ('1M', '5M', '10M', '1H'));

ALTER TABLE ALERT_EVENT 
ADD CONSTRAINT CK_ALERT_EVENT_DAYS CHECK (DAYS BETWEEN 0 AND 127);

ALTER TABLE ALERT_EVENT 
ADD CONSTRAINT CK_ALERT_EVENT_IS_REVERSE CHECK (IS_REVERSE IN (0, 1));

-- 인덱스 추가
CREATE INDEX IDX_ALERT_EVENT_POLICY ON ALERT_EVENT(POLICY_ID, IS_DELETED);
CREATE INDEX IDX_ALERT_EVENT_GRAPH ON ALERT_EVENT(GRAPH_ID);
```

**최종 ALERT_EVENT 테이블 구조:**

```sql
CREATE TABLE ALERT_EVENT (
    ID                  NUMBER            NOT NULL,
    POLICY_ID           NUMBER            NOT NULL,
    INSTANCE_ID         NUMBER            NOT NULL,  -- 제거 예정 (POLICY를 통해 접근)
    CATEGORY            VARCHAR2(20)      NOT NULL,
    STATE               NUMBER(1)          DEFAULT 1 NOT NULL,  -- 기존 필드 (IS_ACTIVE와 동일)
    NAME                VARCHAR2(128)     NOT NULL,
    WARNING             NUMBER            DEFAULT 50 NOT NULL,
    DANGER              NUMBER            DEFAULT 70 NOT NULL,
    CRITICAL            NUMBER            DEFAULT 90 NOT NULL,
    CREATED_AT          TIMESTAMP         NOT NULL,
    UPDATED_AT          TIMESTAMP         NOT NULL,
    IS_DELETED          NUMBER(1)         DEFAULT 0 NOT NULL,
    DELAY_TIME          VARCHAR2(20)      DEFAULT '1M' NOT NULL,
    DAYS                NUMBER(3)         DEFAULT 127 NOT NULL,
    START_TIME          VARCHAR2(5),
    END_TIME            VARCHAR2(5),
    -- 추가 필드
    GRAPH_ID            NUMBER            NOT NULL,
    METRIC_KEY          VARCHAR2(100)     NOT NULL,
    METRIC_NAME         VARCHAR2(200)     NOT NULL,
    IS_REVERSE          NUMBER(1)         DEFAULT 0 NOT NULL,
    CONSTRAINT PK_ALERT_EVENT PRIMARY KEY (ID),
    CONSTRAINT FK_ALERT_EVENT_POLICY FOREIGN KEY (POLICY_ID) REFERENCES ALERT_POLICY(ID) ON DELETE CASCADE,
    CONSTRAINT FK_ALERT_EVENT_GRAPH FOREIGN KEY (GRAPH_ID) REFERENCES GRAPH(ID),
    CONSTRAINT CK_ALERT_EVENT_CATEGORY CHECK (CATEGORY IN ('CPU', 'MEMORY', 'SESSION', 'IO', 'STORAGE')),
    CONSTRAINT CK_ALERT_EVENT_DELAY_TIME CHECK (DELAY_TIME IN ('1M', '5M', '10M', '1H')),
    CONSTRAINT CK_ALERT_EVENT_DAYS CHECK (DAYS BETWEEN 0 AND 127),
    CONSTRAINT CK_ALERT_EVENT_IS_REVERSE CHECK (IS_REVERSE IN (0, 1))
);
```

**컬럼 설명:**
- **ID**: 알림 규칙의 고유 식별자 (시퀀스: SEQ_ALERT_EVENT_ID)
- **POLICY_ID**: 이 알림 규칙이 속한 알림 정책의 ID (ALERT_POLICY.ID 참조)
- **INSTANCE_ID**: 모니터링 대상 인스턴스 ID (제거 예정, POLICY를 통해 접근)
- **CATEGORY**: 알림 카테고리 ('CPU', 'MEMORY', 'SESSION', 'IO', 'STORAGE' 중 하나)
- **STATE**: 알림 규칙 활성화 상태 (1=활성화, 0=비활성화)
- **NAME**: 알림 규칙 이름 (예: "CPU 사용률 알림")
- **WARNING**: 경고(WARNING) 임계값 (0~100 범위, 기본값: 50)
- **DANGER**: 위험(DANGER) 임계값 (0~100 범위, 기본값: 70)
- **CRITICAL**: 치명(CRITICAL) 임계값 (0~100 범위, 기본값: 90)
- **CREATED_AT**: 알림 규칙 생성 시간
- **UPDATED_AT**: 알림 규칙 수정 시간
- **IS_DELETED**: 소프트 삭제 여부 (0=정상, 1=삭제됨)
- **DELAY_TIME**: 누적 시간 옵션 ('1M'=1분, '5M'=5분, '10M'=10분, '1H'=1시간, 기본값: '1M')
  - 메트릭 수집 주기가 1분이므로 최소 누적 시간은 1분
  - 연속으로 임계값을 초과한 횟수가 지정된 시간 동안 지속되어야 알림 발생
  - 예: '5M'이면 5분 동안 연속 초과 시 알림 발생
- **DAYS**: 알림 수신 요일 (비트마스크, 0~127, 기본값: 127=모든 요일)
  - 1=일요일, 2=월요일, 4=화요일, 8=수요일, 16=목요일, 32=금요일, 64=토요일
  - 예: 127 = 모든 요일, 62 = 월~토 (일요일 제외)
- **START_TIME**: 알림 수신 시작 시간 (HH:mm 형식, 예: "09:00", NULL이면 제한 없음)
- **END_TIME**: 알림 수신 종료 시간 (HH:mm 형식, 예: "18:00", NULL이면 제한 없음)
- **GRAPH_ID**: 연결된 그래프 ID (GRAPH.ID 참조, 카테고리별 범위 내)
- **METRIC_KEY**: 메트릭 키 (GraphRegistry의 컬럼명, 예: "HOST_CPU_UTIL_PCT")
- **METRIC_NAME**: 메트릭 이름 (표시용, 예: "Host CPU 사용률")
- **IS_REVERSE**: 역방향 메트릭 여부 (0=정상, 1=역방향, 높을수록 문제가 아닌 경우)

### 3.2.2 EVENT 테이블 수정

**역할**: 실제로 발생한 알림 이벤트를 저장하는 테이블. 메트릭이 임계값을 초과했을 때 생성되며, 심각도(SEVERITY), 현재 값(CURRENT_VALUE), 임계값(THRESHOLD_VALUE) 등을 기록합니다. 알림 확인(Acknowledge) 및 해결(Resolve) 상태도 관리합니다.

**기존 구조를 최대한 활용하면서 다음 필드 추가:**

```sql
-- EVENT 테이블 수정
ALTER TABLE EVENT 
ADD (
    INSTANCE_ID         NUMBER            NOT NULL,
    MEMBER_ID           NUMBER            NOT NULL,
    CURRENT_VALUE       NUMBER(10,2)      NOT NULL,
    THRESHOLD_VALUE     NUMBER(5,2)       NOT NULL,
    ACKNOWLEDGED_AT     TIMESTAMP,
    ACKNOWLEDGED_BY     NUMBER,
    RESOLVED_AT         TIMESTAMP,
    RESOLVED_BY         NUMBER
);

-- 기존 필드 수정
ALTER TABLE EVENT 
MODIFY (
    SEVERITY            NUMBER            NOT NULL,  -- 기존 필드 유지 (1=WARNING, 2=DANGER, 3=CRITICAL)
    STATUS              VARCHAR2(20)      DEFAULT 'PENDING' NOT NULL  -- 추가 필요
);

-- 제약조건 추가
ALTER TABLE EVENT 
ADD CONSTRAINT FK_EVENT_INSTANCE FOREIGN KEY (INSTANCE_ID) REFERENCES INSTANCE(ID);

ALTER TABLE EVENT 
ADD CONSTRAINT FK_EVENT_MEMBER FOREIGN KEY (MEMBER_ID) REFERENCES MEMBER(ID);

ALTER TABLE EVENT 
ADD CONSTRAINT FK_EVENT_ACKNOWLEDGED_BY FOREIGN KEY (ACKNOWLEDGED_BY) REFERENCES MEMBER(ID);

ALTER TABLE EVENT 
ADD CONSTRAINT FK_EVENT_RESOLVED_BY FOREIGN KEY (RESOLVED_BY) REFERENCES MEMBER(ID);

ALTER TABLE EVENT 
ADD CONSTRAINT CK_EVENT_STATUS CHECK (STATUS IN ('PENDING', 'CLOSED'));

ALTER TABLE EVENT 
ADD CONSTRAINT CK_EVENT_SEVERITY CHECK (SEVERITY IN (1, 2, 3));

-- 인덱스 추가
CREATE INDEX IDX_EVENT_INSTANCE ON EVENT(INSTANCE_ID, IS_DELETED);
CREATE INDEX IDX_EVENT_MEMBER ON EVENT(MEMBER_ID, IS_DELETED);
CREATE INDEX IDX_EVENT_STATUS ON EVENT(STATUS, IS_DELETED);
CREATE INDEX IDX_EVENT_TRIGGERED_AT ON EVENT(CREATED_AT DESC);
```

**최종 EVENT 테이블 구조:**

```sql
CREATE TABLE EVENT (
    ID                  NUMBER            NOT NULL,
    ALERT_EVENT_ID      NUMBER            NOT NULL,
    INSTANCE_ID         NUMBER            NOT NULL,
    MEMBER_ID           NUMBER            NOT NULL,
    STATUS              VARCHAR2(20)      DEFAULT 'PENDING' NOT NULL,
    SEVERITY            NUMBER            NOT NULL,  -- 1=WARNING, 2=DANGER, 3=CRITICAL
    CURRENT_VALUE       NUMBER(10,2)      NOT NULL,
    THRESHOLD_VALUE     NUMBER(5,2)      NOT NULL,
    MESSAGE             VARCHAR2(100)     NOT NULL,
    CREATED_AT          TIMESTAMP         NOT NULL,
    UPDATED_AT          TIMESTAMP         NOT NULL,
    IS_DELETED          NUMBER(1)         DEFAULT 0 NOT NULL,
    ACKNOWLEDGED_AT     TIMESTAMP,
    ACKNOWLEDGED_BY     NUMBER,
    RESOLVED_AT         TIMESTAMP,
    RESOLVED_BY         NUMBER,
    CONSTRAINT PK_EVENT PRIMARY KEY (ID),
    CONSTRAINT FK_EVENT_ALERT_EVENT FOREIGN KEY (ALERT_EVENT_ID) REFERENCES ALERT_EVENT(ID),
    CONSTRAINT FK_EVENT_INSTANCE FOREIGN KEY (INSTANCE_ID) REFERENCES INSTANCE(ID),
    CONSTRAINT FK_EVENT_MEMBER FOREIGN KEY (MEMBER_ID) REFERENCES MEMBER(ID),
    CONSTRAINT FK_EVENT_ACKNOWLEDGED_BY FOREIGN KEY (ACKNOWLEDGED_BY) REFERENCES MEMBER(ID),
    CONSTRAINT FK_EVENT_RESOLVED_BY FOREIGN KEY (RESOLVED_BY) REFERENCES MEMBER(ID),
    CONSTRAINT CK_EVENT_STATUS CHECK (STATUS IN ('PENDING', 'CLOSED')),
    CONSTRAINT CK_EVENT_SEVERITY CHECK (SEVERITY IN (1, 2, 3))
);
```

**컬럼 설명:**
- **ID**: 알림 이벤트의 고유 식별자 (시퀀스: SEQ_EVENT_ID)
- **ALERT_EVENT_ID**: 이 이벤트를 발생시킨 알림 규칙의 ID (ALERT_EVENT.ID 참조)
- **INSTANCE_ID**: 알림이 발생한 인스턴스 ID (INSTANCE.ID 참조)
- **MEMBER_ID**: 알림 정책 생성자 ID (ALERT_POLICY.MEMBER_ID, 알림 수신자)
- **STATUS**: 알림 상태 ('PENDING'=미처리, 'CLOSED'=해결됨)
- **SEVERITY**: 심각도 (1=WARNING, 2=DANGER, 3=CRITICAL)
- **CURRENT_VALUE**: 알림 발생 시점의 메트릭 현재 값 (예: 85.5)
- **THRESHOLD_VALUE**: 초과한 임계값 (예: 85.0, WARNING/DANGER/CRITICAL 중 하나)
- **MESSAGE**: 알림 메시지 (예: "CPU 사용률이 85%를 초과했습니다")
- **CREATED_AT**: 알림 발생 시간
- **UPDATED_AT**: 알림 수정 시간
- **IS_DELETED**: 소프트 삭제 여부 (0=정상, 1=삭제됨)
- **ACKNOWLEDGED_AT**: 알림 확인 시간 (NULL=미확인)
- **ACKNOWLEDGED_BY**: 알림을 확인한 사용자 ID (MEMBER.ID 참조, NULL=미확인)
- **RESOLVED_AT**: 알림 해결 시간 (NULL=미해결)
- **RESOLVED_BY**: 알림을 해결한 사용자 ID (MEMBER.ID 참조, NULL=미해결)

### 3.2.3 PROGRESS_HISTORY 테이블 수정

**역할**: 알림 이벤트의 처리 이력을 저장하는 테이블. 사용자가 알림을 확인하거나 해결할 때, 또는 추가 조치 사항을 기록할 때 사용됩니다. 각 EVENT에 대해 여러 개의 처리 이력이 기록될 수 있습니다.

**기존 구조를 최대한 활용하면서 다음 필드 수정:**

```sql
-- PROGRESS_HISTORY 테이블 수정
ALTER TABLE PROGRESS_HISTORY 
RENAME COLUMN EVNET_ID TO EVENT_ID;  -- 오타 수정

ALTER TABLE PROGRESS_HISTORY 
ADD CREATED_BY NUMBER NOT NULL;

ALTER TABLE PROGRESS_HISTORY 
DROP COLUMN USER_NAME;  -- CREATED_BY로 대체

-- 제약조건 추가
ALTER TABLE PROGRESS_HISTORY 
ADD CONSTRAINT FK_PROGRESS_HISTORY_CREATED_BY FOREIGN KEY (CREATED_BY) REFERENCES MEMBER(ID);

-- 인덱스 추가
CREATE INDEX IDX_PROGRESS_HISTORY_CREATED_BY ON PROGRESS_HISTORY(CREATED_BY);
```

**최종 PROGRESS_HISTORY 테이블 구조:**

```sql
CREATE TABLE PROGRESS_HISTORY (
    ID                  NUMBER            NOT NULL,
    EVENT_ID            NUMBER            NOT NULL,
    CONTENT             VARCHAR2(256),
    CREATED_BY           NUMBER            NOT NULL,
    CREATED_AT          TIMESTAMP         NOT NULL,
    UPDATED_AT          TIMESTAMP         NOT NULL,
    IS_DELETED          NUMBER(1)         DEFAULT 0 NOT NULL,
    CONSTRAINT PK_PROGRESS_HISTORY PRIMARY KEY (ID),
    CONSTRAINT FK_PROGRESS_HISTORY_EVENT FOREIGN KEY (EVENT_ID) REFERENCES EVENT(ID) ON DELETE CASCADE,
    CONSTRAINT FK_PROGRESS_HISTORY_CREATED_BY FOREIGN KEY (CREATED_BY) REFERENCES MEMBER(ID)
);
```

**컬럼 설명:**
- **ID**: 처리 이력의 고유 식별자 (시퀀스: SEQ_PROGRESS_HISTORY_ID)
- **EVENT_ID**: 처리 이력이 속한 알림 이벤트 ID (EVENT.ID 참조)
- **CONTENT**: 처리 이력 내용 (예: "알림 확인됨", "문제 해결 완료", "추가 조치 사항" 등)
- **CREATED_BY**: 이력을 작성한 사용자 ID (MEMBER.ID 참조)
- **CREATED_AT**: 이력 작성 시간
- **UPDATED_AT**: 이력 수정 시간
- **IS_DELETED**: 소프트 삭제 여부 (0=정상, 1=삭제됨)

### 3.3 신규 테이블

### 3.3.1 ALERT_POLICY (알림 정책) - 신규 생성

**역할**: 알림 정책을 관리하는 테이블. 하나의 인스턴스(INSTANCE)에 대해 하나 이상의 알림 정책을 생성할 수 있으며, 각 정책은 여러 개의 알림 규칙(ALERT_EVENT)을 포함할 수 있습니다. 정책 단위로 활성화/비활성화가 가능하며, **정책 생성자(MEMBER_ID)만 해당 정책의 알림을 수신**합니다.

```sql
CREATE TABLE ALERT_POLICY (
    ID                  NUMBER            NOT NULL,
    MEMBER_ID           NUMBER            NOT NULL,
    INSTANCE_ID         NUMBER            NOT NULL,
    NAME                VARCHAR2(200)     NOT NULL,
    DESCRIPTION         VARCHAR2(1000),
    IS_ACTIVE           NUMBER(1)         DEFAULT 1 NOT NULL,
    CREATED_AT          TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT          TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
    IS_DELETED          NUMBER(1)         DEFAULT 0 NOT NULL,
    CONSTRAINT PK_ALERT_POLICY PRIMARY KEY (ID),
    CONSTRAINT FK_ALERT_POLICY_MEMBER FOREIGN KEY (MEMBER_ID) REFERENCES MEMBER(ID),
    CONSTRAINT FK_ALERT_POLICY_INSTANCE FOREIGN KEY (INSTANCE_ID) REFERENCES INSTANCE(ID),
    CONSTRAINT CK_ALERT_POLICY_IS_ACTIVE CHECK (IS_ACTIVE IN (0, 1)),
    CONSTRAINT CK_ALERT_POLICY_IS_DELETED CHECK (IS_DELETED IN (0, 1))
);

CREATE SEQUENCE SEQ_ALERT_POLICY_ID START WITH 1 INCREMENT BY 1;
CREATE INDEX IDX_ALERT_POLICY_MEMBER ON ALERT_POLICY(MEMBER_ID, IS_DELETED);
CREATE INDEX IDX_ALERT_POLICY_INSTANCE ON ALERT_POLICY(INSTANCE_ID, IS_DELETED);
```

**컬럼 설명:**
- **ID**: 알림 정책의 고유 식별자 (시퀀스: SEQ_ALERT_POLICY_ID)
- **MEMBER_ID**: 정책을 생성한 사용자 ID (MEMBER.ID 참조, 정책 생성자만 알림 수신)
- **INSTANCE_ID**: 모니터링 대상 인스턴스 ID (INSTANCE.ID 참조)
- **NAME**: 알림 정책 이름 (예: "프로덕션 DB 알림 정책")
- **DESCRIPTION**: 알림 정책 설명 (선택 사항, 예: "프로덕션 환경 알림 설정")
- **IS_ACTIVE**: 정책 활성화 여부 (1=활성화, 0=비활성화, 비활성화 시 알림 체크 안 함)
- **CREATED_AT**: 정책 생성 시간 (기본값: SYSTIMESTAMP)
- **UPDATED_AT**: 정책 수정 시간 (기본값: SYSTIMESTAMP)
- **IS_DELETED**: 소프트 삭제 여부 (0=정상, 1=삭제됨)

### 3.3.2 ALERT_METRIC_TEMPLATE (알림 메트릭 템플릿) - 신규 생성

**역할**: 카테고리별로 선정된 알림 메트릭 템플릿을 저장하는 테이블. 사용자가 알림 규칙을 생성할 때 선택할 수 있는 메트릭 목록을 제공합니다. 각 템플릿은 Graph ID, Metric Key, 기본 임계값 등을 포함합니다.

```sql
CREATE TABLE ALERT_METRIC_TEMPLATE (
    ID                  NUMBER            NOT NULL,
    CATEGORY            VARCHAR2(20)      NOT NULL,
    GRAPH_ID            NUMBER            NOT NULL,
    METRIC_KEY          VARCHAR2(100)     NOT NULL,
    METRIC_NAME         VARCHAR2(200)     NOT NULL,
    DEFAULT_WARNING     NUMBER(5,2),
    DEFAULT_DANGER      NUMBER(5,2),
    DEFAULT_CRITICAL     NUMBER(5,2),
    DESCRIPTION         VARCHAR2(500),
    IS_ACTIVE           NUMBER(1)         DEFAULT 1 NOT NULL,
    CREATED_AT          TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_AT          TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
    IS_DELETED          NUMBER(1)         DEFAULT 0 NOT NULL,
    CONSTRAINT PK_ALERT_METRIC_TEMPLATE PRIMARY KEY (ID),
    CONSTRAINT FK_ALERT_METRIC_TEMPLATE_GRAPH FOREIGN KEY (GRAPH_ID) REFERENCES GRAPH(ID),
    CONSTRAINT CK_ALERT_METRIC_TEMPLATE_CATEGORY CHECK (CATEGORY IN ('CPU', 'MEMORY', 'SESSION', 'IO', 'STORAGE')),
    CONSTRAINT CK_ALERT_METRIC_TEMPLATE_IS_ACTIVE CHECK (IS_ACTIVE IN (0, 1)),
    CONSTRAINT CK_ALERT_METRIC_TEMPLATE_IS_DELETED CHECK (IS_DELETED IN (0, 1))
);

CREATE SEQUENCE SEQ_ALERT_METRIC_TEMPLATE_ID START WITH 1 INCREMENT BY 1;
CREATE INDEX IDX_ALERT_METRIC_TEMPLATE_CATEGORY ON ALERT_METRIC_TEMPLATE(CATEGORY, IS_ACTIVE, IS_DELETED);
CREATE INDEX IDX_ALERT_METRIC_TEMPLATE_GRAPH ON ALERT_METRIC_TEMPLATE(GRAPH_ID);
```

**컬럼 설명:**
- **ID**: 메트릭 템플릿의 고유 식별자 (시퀀스: SEQ_ALERT_METRIC_TEMPLATE_ID)
- **CATEGORY**: 메트릭 카테고리 ('CPU', 'MEMORY', 'SESSION', 'IO', 'STORAGE' 중 하나)
- **GRAPH_ID**: 연결된 그래프 ID (GRAPH.ID 참조, 카테고리별 범위 내)
- **METRIC_KEY**: 메트릭 키 (GraphRegistry의 컬럼명, 예: "HOST_CPU_UTIL_PCT")
- **METRIC_NAME**: 메트릭 이름 (표시용, 예: "Host CPU 사용률")
- **DEFAULT_WARNING**: 기본 경고(WARNING) 임계값 (0~100 범위, 선택 사항)
- **DEFAULT_DANGER**: 기본 위험(DANGER) 임계값 (0~100 범위, 선택 사항)
- **DEFAULT_CRITICAL**: 기본 치명(CRITICAL) 임계값 (0~100 범위, 선택 사항)
- **DESCRIPTION**: 메트릭 설명 (선택 사항)
- **IS_ACTIVE**: 템플릿 활성화 여부 (1=활성화, 0=비활성화)
- **CREATED_AT**: 템플릿 생성 시간 (기본값: SYSTIMESTAMP)
- **UPDATED_AT**: 템플릿 수정 시간 (기본값: SYSTIMESTAMP)
- **IS_DELETED**: 소프트 삭제 여부 (0=정상, 1=삭제됨)

**초기 데이터 삽입:**
- plan.md의 "6. 카테고리별 메트릭 선정" 섹션에 정의된 모든 메트릭을 INSERT
- 각 메트릭의 Graph ID, Metric Key, Metric Name, 기본 임계값 등을 저장


### 3.4 테이블 관계도

**테이블 역할 요약:**
- **MEMBER**: 사용자 정보 (이메일, 슬랙 주소 등)
- **ALERT_POLICY**: 알림 정책 (인스턴스별 정책 관리, 정책 생성자만 알림 수신)
- **ALERT_EVENT**: 알림 규칙 (정책에 속한 개별 알림 규칙)
- **ALERT_METRIC_TEMPLATE**: 알림 메트릭 템플릿 (카테고리별 선정된 메트릭 목록)
- **EVENT**: 알림 발생 이벤트 (실제 발생한 알림 기록)
- **PROGRESS_HISTORY**: 알림 처리 이력 (이벤트 처리 과정 기록)
- **INSTANCE**: DB 인스턴스 (모니터링 대상)
- **GRAPH**: 그래프 정보 (대시보드 그래프 정의)

**관계도:**

```
MEMBER (사용자)
  ├── ALERT_POLICY (1:N) - 사용자가 생성한 알림 정책
  │     ├── ALERT_EVENT (1:N) - 정책에 속한 알림 규칙들
  │     └── EVENT (1:N) - 정책 생성자에게 발생한 알림 이벤트
  │           └── PROGRESS_HISTORY (1:N) - 이벤트 처리 이력

INSTANCE (DB 인스턴스)
  ├── ALERT_POLICY (1:N) - 인스턴스에 대한 알림 정책들
  └── EVENT (1:N) - 인스턴스에서 발생한 알림 이벤트들

GRAPH (그래프)
  ├── ALERT_METRIC_TEMPLATE (1:N) - 그래프와 연결된 메트릭 템플릿들
  └── ALERT_EVENT (1:N) - 그래프와 연결된 알림 규칙들

ALERT_METRIC_TEMPLATE (메트릭 템플릿)
  └── ALERT_EVENT (1:N) - 템플릿을 기반으로 생성된 알림 규칙들 (참조 관계)
```

**주요 관계 설명:**
- **MEMBER → ALERT_POLICY**: 한 사용자가 여러 알림 정책 생성 가능, 정책 생성자만 해당 정책의 알림 수신
- **ALERT_POLICY → ALERT_EVENT**: 한 정책에 여러 알림 규칙 포함 가능
- **ALERT_EVENT → EVENT**: 알림 규칙이 임계값 초과 시 이벤트 생성
- **EVENT → PROGRESS_HISTORY**: 한 이벤트에 여러 처리 이력 기록 가능
- **INSTANCE → ALERT_POLICY**: 한 인스턴스에 여러 정책 설정 가능
- **GRAPH → ALERT_EVENT**: 한 그래프에 여러 알림 규칙 연결 가능

### 3.5 EVENT_CATEGORY_ENUM 테이블

- 기존 테이블이지만 코드에서 Enum으로 관리하므로 사용하지 않음
- 삭제하거나 유지해도 무방 (사용 안 함)

---

## 4. 주요 플로우

### 4.1 메트릭 수집 및 알림 체크 플로우

```
1. 스케줄러 실행 (매 분, app.batch.enabled=true일 때만)
   ↓
2. CollectorService.runOnce(instanceId) - 메트릭 수집 및 계산
   ↓
3. finals Map<String, Object> 생성 (255개 지표)
   ↓
4. AlertCheckService.checkAlerts(finals, instanceId) - 알림 조건 체크
   ↓
5. 활성화된 ALERT_EVENT 조회
   - SELECT * FROM ALERT_EVENT
     WHERE policy_id IN (
       SELECT id FROM ALERT_POLICY
       WHERE instance_id=? AND is_active=true AND is_deleted=false
     ) AND state=1 AND is_deleted=false
   - 해당 알림이 속한 정책이 활성화되어 있는지 확인
   ↓
6. 각 알림 규칙에 대해 체크:
   a. 시간 범위 체크
      - 현재 시간이 start_time ~ end_time 사이인지 확인
   b. 요일 체크 (비트마스크)
      - 현재 요일이 days 비트마스크에 포함되는지 확인
   c. 메트릭 값 추출
      - finals.get(alertEvent.metricKey)
      - 값이 null이면 스킵
   d. 임계값 비교 및 심각도 결정
      - value >= critical → severity = 3 (치명)
      - value >= danger → severity = 2 (위험)
      - value >= warning → severity = 1 (주의)
      - value < warning → 스킵
   e. 누적 시간 조건 확인
      - AlertStateStore에서 상태 조회
      - Key: instanceId:alertEventId
      - DELAY_TIME에 따른 최소 연속 초과 횟수 계산:
        * '1M' = 1회 (1분)
        * '5M' = 5회 (5분)
        * '10M' = 10회 (10분)
        * '1H' = 60회 (1시간)
      - 현재 연속 초과 횟수 < 최소 연속 초과 횟수이면:
        * consecutiveCount++
        * 상태 저장
        * 스킵 (아직 알림 발생 안 함)
      - 현재 연속 초과 횟수 >= 최소 연속 초과 횟수이면:
        * 알림 발생
   f. 임계값 미만이면:
      - consecutiveCount = 0 (리셋)
      - 상태 저장
   ↓
7. 알림 발생 시
   a. EVENT 테이블에 저장
      - alert_event_id (ALERT_EVENT.id)
      - instance_id (ALERT_POLICY.instance_id)
      - member_id (ALERT_POLICY.member_id - 알림 정책 생성자)
      - severity (1=WARNING, 2=DANGER, 3=CRITICAL)
      - message (알림 메시지)
      - current_value (현재 메트릭 값)
      - threshold_value (초과한 임계값)
      - status = 'PENDING' (미처리)
      - created_at = 현재 시간
   b. 알림 수신자 조회
      - ALERT_POLICY.member_id 조회 (정책 생성자)
      - 정책 생성자만 알림 수신
   c. AlertNotificationService.sendAlerts(event, memberId) 호출
      - SSE 전송: 정책 생성자에게만 전송
      - 이메일 전송: severity가 1 또는 2인 경우, 정책 생성자의 이메일로 전송
      - 슬랙 전송: severity가 3인 경우, 정책 생성자의 슬랙 웹훅 URL로 전송
   ↓
8. MetricData 생성 및 DB 저장
   - GraphRegistry.mapRow()로 MetricData 생성
   - MetricData 테이블에 저장
   ↓
9. 원래 플로우 계속
   - MetricCollectionTasklet.execute() 계속 실행
```

### 4.2 알림 정책 생성 플로우

```
1. 사용자가 알림 정책 생성 요청
   ↓
2. AlertPolicyCommandService.createPolicy()
   ↓
3. ALERT_POLICY 테이블에 저장
   ↓
4. 알림 규칙 추가 (ALERT_EVENT)
   │   ├─ 카테고리 선택
   │   ├─ 그래프 선택 (카테고리 범위 내)
   │   ├─ 메트릭 선택
   │   ├─ 임계값 설정
   │   └─ 누적 시간/요일/시간대 설정
   ↓
5. 정책 활성화/비활성화 (ALERT_POLICY.IS_ACTIVE)
```

### 4.3 실시간 알림 수신 플로우 (SSE)

```
1. 클라이언트가 SSE 연결 요청
   │   GET /api/alerts/sse/stream
   ↓
2. SseAlertService.createConnection()
   │   ├─ 인증 확인
   │   └─ SseEmitter 생성
   ↓
3. SseEmitter를 Map에 저장 (userId → SseEmitter)
   ↓
4. 하트비트 전송 (30초마다)
   │   :heartbeat
   ↓
5. 알림 발생 시
   │   ├─ 정책 생성자 확인 (ALERT_POLICY.member_id)
   │   ├─ SSE 연결 확인
   │   └─ 실시간 전송
   │       data: { eventId, severity, message, ... }
   ↓
6. 연결 종료 시 정리
   │   └─ SseEmitter 제거
```

### 4.4 알림 확인 및 해결 플로우

```
1. 사용자가 알림 확인 요청
   │   POST /api/alerts/events/{eventId}/acknowledge
   ↓
2. EventCommandService.acknowledgeEvent()
   ↓
3. EVENT 테이블 업데이트
   │   ├─ STATUS는 유지 (PENDING)
   │   ├─ ACKNOWLEDGED_AT = 현재 시간
   │   └─ ACKNOWLEDGED_BY = 현재 사용자 ID
   ↓
4. PROGRESS_HISTORY에 기록
   │   └─ "알림 확인됨" 내용 추가
   ↓
5. 사용자가 알림 해결 요청
   │   POST /api/alerts/events/{eventId}/resolve
   ↓
6. EventCommandService.resolveEvent()
   ↓
7. EVENT 테이블 업데이트
   │   ├─ STATUS = 'CLOSED'
   │   ├─ RESOLVED_AT = 현재 시간
   │   └─ RESOLVED_BY = 현재 사용자 ID
```

---

## 5. 주요 API 엔드포인트

### 5.1 알림 정책 관리

#### POST /api/alerts/policies
- 알림 정책 생성
- Request Body: 
```json
{
  "name": "프로덕션 DB 알림 정책",
  "description": "프로덕션 환경 알림 설정",
  "instanceId": 1
}
```

#### GET /api/alerts/policies
- 알림 정책 목록 조회 (현재 사용자가 생성한 정책)
- Query Parameters: `instanceId` (선택)

#### GET /api/alerts/policies/{policyId}
- 알림 정책 상세 조회

#### GET /api/alerts/metrics/templates
- 알림 메트릭 템플릿 목록 조회
- Query Parameters: `category` (선택, 'CPU', 'MEMORY', 'SESSION', 'IO', 'STORAGE')
- Response: 카테고리별로 선정된 메트릭 템플릿 목록
- Response Body 예시:
```json
[
  {
    "id": 1,
    "category": "CPU",
    "graphId": 15,
    "metricKey": "HOST_CPU_UTIL_PCT",
    "metricName": "Host CPU 사용률",
    "defaultWarning": 70,
    "defaultDanger": 85,
    "defaultCritical": 95,
    "description": "호스트 전체 CPU 사용률"
  },
  {
    "id": 2,
    "category": "CPU",
    "graphId": 14,
    "metricKey": "CPU_SATURATION_PCT",
    "metricName": "DB CPU 포화도",
    "defaultWarning": 80,
    "defaultDanger": 90,
    "defaultCritical": 95,
    "description": "DB CPU 포화도"
  }
]
```

#### PUT /api/alerts/policies/{policyId}
- 알림 정책 수정

#### DELETE /api/alerts/policies/{policyId}
- 알림 정책 삭제 (소프트 삭제)

#### PATCH /api/alerts/policies/{policyId}/toggle
- 알림 정책 활성화/비활성화

### 5.2 알림 규칙 관리

#### POST /api/alerts/policies/{policyId}/events
- 알림 규칙 추가
- Request Body:
```json
{
  "category": "CPU",
  "graphId": 15,
  "metricKey": "HOST_CPU_UTIL_PCT",
  "metricName": "Host CPU 사용률",
  "name": "CPU 사용률 알림",
  "warning": 70,
  "danger": 85,
  "critical": 90,
  "delayTime": "1M",
  "days": 127,
  "startTime": "09:00",
  "endTime": "18:00",
  "isReverse": 0
}
```

#### GET /api/alerts/policies/{policyId}/events
- 알림 규칙 목록 조회

#### PUT /api/alerts/events/{eventId}
- 알림 규칙 수정

#### DELETE /api/alerts/events/{eventId}
- 알림 규칙 삭제 (소프트 삭제)

#### PATCH /api/alerts/events/{eventId}/toggle
- 알림 규칙 활성화/비활성화 (STATE 변경)

### 5.3 알림 이벤트 관리

#### GET /api/alerts/events
- 알림 이벤트 목록 조회
- Query Parameters: 
  - `status` (PENDING, CLOSED)
  - `severity` (1, 2, 3)
  - `instanceId`
  - `memberId`
  - `page`, `size`

#### GET /api/alerts/events/{eventId}
- 알림 이벤트 상세 조회

#### POST /api/alerts/events/{eventId}/acknowledge
- 알림 확인
- Request Body:
```json
{
  "message": "확인했습니다"
}
```

#### POST /api/alerts/events/{eventId}/resolve
- 알림 해결
- Request Body:
```json
{
  "message": "문제 해결 완료"
}
```

#### GET /api/alerts/events/{eventId}/history
- 알림 처리 이력 조회

#### POST /api/alerts/events/{eventId}/history
- 알림 처리 이력 추가
- Request Body:
```json
{
  "content": "추가 조치 사항"
}
```

### 5.4 실시간 알림 (SSE)

#### GET /api/alerts/sse/stream
- SSE 연결 생성
- Headers: `Authorization: Bearer {token}`
- Response: `text/event-stream`
- 하트비트: 30초마다 `:heartbeat` 전송
- 알림 데이터 형식:
```
data: {"eventId": 1, "severity": 2, "message": "CPU 사용률이 85%를 초과했습니다", "instanceId": 1, "graphId": 15}
```

---

## 6. 카테고리별 메트릭 선정

**저장 위치**: 선정된 메트릭은 `ALERT_METRIC_TEMPLATE` 테이블에 저장됩니다. 사용자가 알림 규칙을 생성할 때 이 템플릿 목록에서 메트릭을 선택할 수 있습니다.

### 6.1 선정 기준
- MetricData 엔티티에 컬럼 존재
- GraphRegistry의 그래프에서 사용 중
- 0~100% 범위로 표현 가능 (정규화 불필요)
- 높을수록 문제인 메트릭만 (역방향 알림 제외)
- 각 알림은 하나의 Graph ID와 1:1 매핑
- 델타 계산 불필요 (현재 값만으로 판단 가능)
- 그래프 ID는 카테고리별 범위 내에서만 선택:
  - CPU: 13 ~ 20
  - Memory: 21 ~ 28
  - Session: 29 ~ 36
  - I/O: 37 ~ 44
  - Storage: 45 ~ 52

## CPU (카테고리 2) - 5개 메트릭

### 1. HOST_CPU_UTIL_PCT - 호스트 CPU 사용률
- **Graph ID**: 15
- **Graph 이름**: "Host CPU Utilization (%) – Trend"
- **Metric Key**: `HOST_CPU_UTIL_PCT`
- **MetricData 필드**: `hostCpuUtilPct`
- **범위**: 0~100%
- **알림 기준 제안**: 70% (Warning), 85% (Danger), 95% (Critical)

### 2. CPU_SATURATION_PCT - DB CPU 포화도
- **Graph ID**: 14
- **Graph 이름**: "DB CPU Saturation (AAS vs Core)"
- **Metric Key**: `CPU_SATURATION_PCT`
- **MetricData 필드**: `cpuSaturationPct`
- **범위**: 0~100%
- **알림 기준 제안**: 80% (Warning), 90% (Danger), 95% (Critical)

### 3. DB_OF_HOST_SHARE_PCT - DB CPU 비율
- **Graph ID**: 16
- **Graph 이름**: "DB CPU Share of Host (%) – Trend"
- **Metric Key**: `DB_OF_HOST_SHARE_PCT`
- **MetricData 필드**: `dbOfHostSharePct`
- **범위**: 0~100%
- **알림 기준 제안**: 60% (Warning), 75% (Danger), 90% (Critical)

### 4. OTHER_PROCESSES_PCT - 기타 프로세스 CPU 비율
- **Graph ID**: 16
- **Graph 이름**: "DB CPU Share of Host (%) – Trend"
- **Metric Key**: `OTHER_PROCESSES_PCT`
- **MetricData 필드**: `otherProcessesPct`
- **범위**: 0~100%
- **알림 기준 제안**: 30% (Warning), 50% (Danger), 70% (Critical)
- **참고**: Graph 16과 공유하지만 OTHER_PROCESSES_PCT를 메인 메트릭으로 사용

### 5. AAS_ONCPU_SESSIONS - On-CPU 세션 수 (비율 계산)
- **Graph ID**: 14
- **Graph 이름**: "DB CPU Saturation (AAS vs Core)"
- **Metric Key**: `AAS_ONCPU_SESSIONS` (CORE_BASELINE_SESSIONS 대비 비율)
- **MetricData 필드**: `aasOncpuSessions`, `coreBaselineSessions`
- **범위**: 0~100% (AAS_ONCPU_SESSIONS / CORE_BASELINE_SESSIONS * 100)
- **알림 기준 제안**: 70% (Warning), 85% (Danger), 95% (Critical)
- **참고**: Graph 14와 공유하지만 AAS_ONCPU_SESSIONS를 메인 메트릭으로 사용, 비율 계산은 단순 나눗셈

## Memory (카테고리 3) - 5개 메트릭

### 1. PGA_UTIL_PCT - PGA 사용률
- **Graph ID**: 23
- **Graph 이름**: "PGA Utilization (%) – Trend"
- **Metric Key**: `PGA_UTIL_PCT`
- **MetricData 필드**: `pgaUtilPct`
- **범위**: 0~100%
- **알림 기준 제안**: 70% (Warning), 85% (Danger), 95% (Critical)

### 2. SGA_UTIL_PCT - SGA 사용률
- **Graph ID**: 24
- **Graph 이름**: "SGA Utilization (%) — Trend"
- **Metric Key**: `SGA_UTIL_PCT`
- **MetricData 필드**: `sgaUtilPct`
- **범위**: 0~100%
- **알림 기준 제안**: 70% (Warning), 85% (Danger), 95% (Critical)

### 3. WORKAREA_SPILL_RATE_PCT - Workarea Spill 비율
- **Graph ID**: 25
- **Graph 이름**: "Workarea Spill Rate (%) – Trend"
- **Metric Key**: `WORKAREA_SPILL_RATE_PCT`
- **MetricData 필드**: `workareaSpillRatePct`
- **범위**: 0~100%
- **알림 기준 제안**: 10% (Warning), 20% (Danger), 30% (Critical)

### 4. BUFFER_MISS_PCT - Buffer Cache Miss 비율
- **Graph ID**: 27
- **Graph 이름**: "Buffer Cache Miss Rate (%) – Proxy – Trend"
- **Metric Key**: `BUFFER_MISS_PCT`
- **MetricData 필드**: `bufferMissPct`
- **범위**: 0~100%
- **알림 기준 제안**: 10% (Warning), 20% (Danger), 30% (Critical)

### 5. HARD_PARSE_RATIO_PCT - Hard Parse 비율
- **Graph ID**: 7
- **Graph 이름**: "SGA 압박(FreeMB/Reloads)"
- **Metric Key**: `HARD_PARSE_RATIO_PCT`
- **MetricData 필드**: `hardParseRatioPct`
- **범위**: 0~100%
- **알림 기준 제안**: 5% (Warning), 10% (Danger), 20% (Critical)

## Session (카테고리 4) - 5개 메트릭

### 1. ACTIVE_USER_RATIO_PCT - 활성 사용자 비율
- **Graph ID**: 35
- **Graph 이름**: "Session Activity & Resource Summary"
- **Metric Key**: `ACTIVE_USER_RATIO_PCT`
- **MetricData 필드**: `activeUserRatioPct`
- **범위**: 0~100%
- **알림 기준 제안**: 80% (Warning), 90% (Danger), 95% (Critical)

### 2. SESSIONS_LIMIT_UTIL_PCT - 세션 한도 사용률
- **Graph ID**: 35
- **Graph 이름**: "Session Activity & Resource Summary"
- **Metric Key**: `SESSIONS_LIMIT_UTIL_PCT`
- **MetricData 필드**: `sessionsLimitUtilPct`
- **범위**: 0~100%
- **알림 기준 제안**: 70% (Warning), 85% (Danger), 95% (Critical)
- **참고**: Graph 35과 공유하지만 SESSIONS_LIMIT_UTIL_PCT를 메인 메트릭으로 사용

### 3. PROCESSES_LIMIT_UTIL_PCT - 프로세스 한도 사용률
- **Graph ID**: 35
- **Graph 이름**: "Session Activity & Resource Summary"
- **Metric Key**: `PROCESSES_LIMIT_UTIL_PCT`
- **MetricData 필드**: `processesLimitUtilPct`
- **범위**: 0~100%
- **알림 기준 제안**: 70% (Warning), 85% (Danger), 95% (Critical)
- **참고**: Graph 35과 공유하지만 PROCESSES_LIMIT_UTIL_PCT를 메인 메트릭으로 사용

### 4. session_usage_pct - 세션 사용률
- **Graph ID**: 8
- **Graph 이름**: "세션 한도/급증"
- **Metric Key**: `session_usage_pct`
- **MetricData 필드**: `sessionUsagePct`
- **범위**: 0~100%
- **알림 기준 제안**: 70% (Warning), 85% (Danger), 95% (Critical)

### 5. processes_usage_pct - 프로세스 사용률
- **Graph ID**: 12
- **Graph 이름**: "제한 근접 파라미터 감시"
- **Metric Key**: `processes_usage_pct`
- **MetricData 필드**: `processesUsagePct`
- **범위**: 0~100%
- **알림 기준 제안**: 70% (Warning), 85% (Danger), 95% (Critical)

## I/O (카테고리 5) - 3개 메트릭

### 1. direct_io_ratio_pct - Direct I/O 비율
- **Graph ID**: 38
- **Graph 이름**: "Direct Path I/O (개/초)"
- **Metric Key**: `direct_io_ratio_pct`
- **MetricData 필드**: `directIoRatioPct`
- **범위**: 0~100%
- **알림 기준 제안**: 20% (Warning), 30% (Danger), 40% (Critical)

### 2. parse_execute_ratio - Parse/Execute 비율
- **Graph ID**: 37
- **Graph 이름**: "I/O Performance Dashboard"
- **Metric Key**: `parse_execute_ratio`
- **MetricData 필드**: `parseExecuteRatio`
- **범위**: 0~100% (비율 값, 높을수록 문제)
- **알림 기준 제안**: 0.5 (Warning), 0.7 (Danger), 0.9 (Critical)
- **참고**: parse_execute_ratio는 0~1 범위이므로 * 100으로 변환하여 0~100%로 처리

### 3. cache_hit_ratio_pct - 캐시 히트 비율 (역방향 변환)
- **Graph ID**: 37
- **Graph 이름**: "I/O Performance Dashboard"
- **Metric Key**: `cache_hit_ratio_pct`
- **MetricData 필드**: `cacheHitRatioPct`
- **범위**: 0~100% (100 - cache_hit_ratio_pct로 변환하여 높을수록 문제로 처리)
- **알림 기준 제안**: 90% 미만 → 10% 이상 (Warning), 85% 미만 → 15% 이상 (Danger), 80% 미만 → 20% 이상 (Critical)
- **참고**: 역방향 메트릭이지만 100 - cache_hit_ratio_pct로 변환하여 높을수록 문제로 처리 (단순 계산)

## Storage (카테고리 6) - 5개 메트릭

### 1. TOTAL_DB_USAGE_PCT - 전체 DB 사용률
- **Graph ID**: 51
- **Graph 이름**: "Total Database Usage Trend (%)"
- **Metric Key**: `total_db_usage_percent`
- **MetricData 필드**: `totalDbUsagePercent`
- **범위**: 0~100%
- **알림 기준 제안**: 70% (Warning), 85% (Danger), 95% (Critical)

### 2. FRA_USAGE_PCT - FRA 사용률
- **Graph ID**: 49
- **Graph 이름**: "FRA 사용률 추세 (%)"
- **Metric Key**: `usage_pct` (Graph 49의 usage_pct)
- **MetricData 필드**: `usagePct` (Graph 49 기준)
- **범위**: 0~100%
- **알림 기준 제안**: 70% (Warning), 85% (Danger), 95% (Critical)
- **참고**: Graph 49의 usage_pct 필드 사용 (FRA 관련)

### 3. UNDO_USAGE_PCT - Undo 사용률
- **Graph ID**: 50
- **Graph 이름**: "Undo 사용률 추세 (%)"
- **Metric Key**: `undo_usage_percent`
- **MetricData 필드**: `undoUsagePercent`
- **범위**: 0~100%
- **알림 기준 제안**: 70% (Warning), 85% (Danger), 95% (Critical)

### 4. TEMP_USAGE_PCT - Temp 사용률
- **Graph ID**: 46
- **Graph 이름**: "Temp Tablespace Active Usage (GB)"
- **Metric Key**: `temp_usage_percent`
- **MetricData 필드**: `tempUsagePercent`
- **범위**: 0~100%
- **알림 기준 제안**: 70% (Warning), 85% (Danger), 95% (Critical)

### 5. MAX_TS_USAGE_PCT - 최대 테이블스페이스 사용률
- **Graph ID**: 45
- **Graph 이름**: "Storage Health Dashboard"
- **Metric Key**: `MAX_TS_USAGE_PCT`
- **MetricData 필드**: `maxTsUsagePct`
- **범위**: 0~100%
- **알림 기준 제안**: 80% (Warning), 90% (Danger), 95% (Critical)

## 메트릭 선정 요약

**총 23개 메트릭 (CPU 5개, Memory 5개, Session 5개, I/O 3개, Storage 5개)**

## 중요한 설계 포인트

1. **1:1 그래프 매핑**: 각 알림은 하나의 Graph ID와 연결됨
2. **히스토리 페이지**: 알림 발생 시 해당 그래프가 속한 카테고리의 모든 그래프를 표시
3. **Metric Key**: GraphRegistry의 컬럼명을 그대로 사용 (대소문자 구분)
4. **단순 계산만 허용**:
   - `AAS_ONCPU_SESSIONS`: CORE_BASELINE_SESSIONS로 나눈 비율 (단순 나눗셈)
   - `parse_execute_ratio`: * 100으로 변환 (단순 곱셈)
   - `cache_hit_ratio_pct`: 100 - 값으로 변환 (단순 뺄셈)
5. **정규화/델타 계산 불필요**: 모든 메트릭은 현재 값만으로 판단 가능

---

## 7. 구현 단계

### 7.0 구현 순서 요약

**패키지 구조:**
- 모든 알림 관련 코드는 `com.sys.dbmonitor.domains.notification` 패키지 하위에 생성
- 기존 도메인 구조와 동일하게 구성:
  ```
  domains/notification/
    ├── domain/          (엔티티 클래스)
    ├── dto/            (Request/Response DTO)
    │   ├── request/
    │   └── response/
    ├── repository/     (JPA Repository)
    ├── service/         (비즈니스 로직)
    │   ├── command/    (Command Service)
    │   └── query/      (Query Service)
    ├── controller/      (REST API Controller)
    │   ├── command/    (Command Controller)
    │   └── query/      (Query Controller)
    └── state/          (인메모리 상태 저장)
  ```

**구현 순서:**

```
Phase 1: 데이터베이스 및 도메인 모델
  ↓
  1.1. Enum 클래스 생성 (domain/)
     - AlertLevel (WARNING, DANGER, CRITICAL)
     - AlertStatus (PENDING, CLOSED)
     - DelayTime (1M, 5M, 10M, 1H)
     - AlertCategory (CPU, MEMORY, SESSION, IO, STORAGE)
  
  1.2. 엔티티 클래스 생성 (domain/)
     - AlertPolicy (알림 정책)
     - AlertEvent (알림 규칙)
     - AlertMetricTemplate (알림 메트릭 템플릿)
     - Event (알림 발생 이벤트)
     - ProgressHistory (처리 이력)
     - @Entity, @Table, @Column 어노테이션 설정
     - JPA 관계 매핑 (@ManyToOne, @OneToMany 등)
  
  1.3. Repository 인터페이스 생성 (repository/)
     - AlertPolicyRepository extends JpaRepository
     - AlertEventRepository extends JpaRepository
     - AlertMetricTemplateRepository extends JpaRepository
     - EventRepository extends JpaRepository
     - ProgressHistoryRepository extends JpaRepository
     - 커스텀 쿼리 메서드 정의 (활성화된 정책 조회, 카테고리별 템플릿 조회 등)
  
  1.4. 메트릭 템플릿 초기 데이터 삽입
     - plan.md의 "6. 카테고리별 메트릭 선정" 섹션에 정의된 모든 메트릭을 ALERT_METRIC_TEMPLATE 테이블에 INSERT
     - 각 카테고리별로 선정된 메트릭의 Graph ID, Metric Key, Metric Name, 기본 임계값 등을 저장
  
  1.5. (선택) 기존 테이블 수정 스크립트 실행
     - ALERT_EVENT 테이블 수정 (POLICY_ID, GRAPH_ID 등 추가)
     - EVENT 테이블 수정 (INSTANCE_ID, MEMBER_ID 등 추가)
     - PROGRESS_HISTORY 테이블 수정 (EVENT_ID 오타 수정)

Phase 2: 알림 체크 로직
  ↓
  2.1. AlertStateStore 구현 (state/)
     - ConcurrentHashMap<String, AlertState> 사용
     - Key: "instanceId:alertEventId"
     - 연속 초과 횟수 및 마지막 체크 시간 저장
  
  2.2. AlertCheckService 구현 (service/)
     - checkAlerts(Map<String, Object> finals, Long instanceId) 메서드
     - 활성화된 ALERT_EVENT 조회 (정책 활성화 확인 포함)
     - 각 알림 규칙에 대해:
       * 시간 범위 체크 (START_TIME, END_TIME)
       * 요일 체크 (비트마스크 연산)
       * 메트릭 값 추출 (finals.get(metricKey))
       * 임계값 비교 및 심각도 결정
       * 누적 시간 조건 확인 (AlertStateStore 사용)
     - Event 엔티티 생성 및 저장

Phase 3: 알림 전송 로직
  ↓
  3.1. SseAlertService 구현 (service/)
     - Map<Long, SseEmitter> 관리 (userId → SseEmitter)
     - createConnection(Long userId) - SSE 연결 생성
     - sendAlert(Long userId, Event event) - 실시간 알림 전송
     - 하트비트 스케줄러 (30초마다 :heartbeat 전송)
  
  3.2. EmailAlertService 구현 (service/)
     - JavaMailSender 의존성 주입
     - sendEmail(Member member, Event event) 메서드
     - 이메일 템플릿 작성 (HTML 형식)
     - SMTP 설정 (application.yml)
  
  3.3. SlackAlertService 구현 (service/)
     - WebClient Bean 사용
     - sendSlack(Member member, Event event) 메서드
     - Slack 메시지 포맷 작성 (JSON)
     - Webhook URL 사용 (Member.slackAddress)
  
  3.4. AlertNotificationService 구현 (service/)
     - sendAlerts(Event event, Long memberId) 메서드
     - 정책 생성자 조회 (ALERT_POLICY.member_id)
     - 심각도별 전송 채널 선택:
       * WARNING(1), DANGER(2) → 이메일
       * CRITICAL(3) → Slack
     - SSE는 항상 전송 (연결된 경우)
     - @Async로 비동기 처리

Phase 4: 서비스 통합
  ↓
  4.1. CollectorService 통합
     - MetricCollectionTasklet.execute() 수정
     - CollectorService.runOnce(instanceId) 호출
     - AlertCheckService.checkAlerts(finals, instanceId) 호출 (MetricData 저장 전)
     - MetricData 생성 및 DB 저장
  
  4.2. Command Service 구현 (service/command/)
     - AlertPolicyCommandService
       * createPolicy() - 정책 생성
       * updatePolicy() - 정책 수정
       * deletePolicy() - 정책 삭제 (소프트 삭제)
       * togglePolicy() - 정책 활성화/비활성화
     - AlertEventCommandService
       * createEvent() - 알림 규칙 추가
       * updateEvent() - 알림 규칙 수정
       * deleteEvent() - 알림 규칙 삭제
       * toggleEvent() - 알림 규칙 활성화/비활성화
     - EventCommandService
       * acknowledgeEvent() - 알림 확인
       * resolveEvent() - 알림 해결
       * addHistory() - 처리 이력 추가
  
  4.3. Query Service 구현 (service/query/)
     - AlertPolicyQueryService
       * getPolicies() - 정책 목록 조회
       * getPolicy() - 정책 상세 조회
     - AlertEventQueryService
       * getEvents() - 알림 규칙 목록 조회
       * getEvent() - 알림 규칙 상세 조회
     - AlertMetricTemplateQueryService
       * getTemplates() - 메트릭 템플릿 목록 조회 (카테고리 필터링)
       * getTemplate() - 메트릭 템플릿 상세 조회
     - EventQueryService
       * getEvents() - 알림 이벤트 목록 조회 (필터링, 페이징)
       * getEvent() - 알림 이벤트 상세 조회
       * getHistory() - 처리 이력 조회

Phase 5: API 및 DTO
  ↓
  5.1. DTO 클래스 생성 (dto/request/, dto/response/)
     - Request DTO:
       * AlertPolicyCreateRequest
       * AlertPolicyUpdateRequest
       * AlertEventCreateRequest
       * AlertEventUpdateRequest
       * EventAcknowledgeRequest
       * EventResolveRequest
       * ProgressHistoryCreateRequest
     - Response DTO:
       * AlertPolicyResponse
       * AlertEventResponse
       * AlertMetricTemplateResponse
       * EventResponse
       * ProgressHistoryResponse
  
  5.2. Controller 계층 구현 (controller/command/, controller/query/)
     - AlertPolicyCommandController (controller/command/)
       * POST /api/alerts/policies - 정책 생성
       * PUT /api/alerts/policies/{id} - 정책 수정
       * DELETE /api/alerts/policies/{id} - 정책 삭제
       * PATCH /api/alerts/policies/{id}/toggle - 정책 활성화/비활성화
     - AlertEventCommandController (controller/command/)
       * POST /api/alerts/policies/{policyId}/events - 규칙 추가
       * PUT /api/alerts/events/{id} - 규칙 수정
       * DELETE /api/alerts/events/{id} - 규칙 삭제
       * PATCH /api/alerts/events/{id}/toggle - 규칙 활성화/비활성화
     - EventCommandController (controller/command/)
       * POST /api/alerts/events/{id}/acknowledge - 알림 확인
       * POST /api/alerts/events/{id}/resolve - 알림 해결
       * POST /api/alerts/events/{id}/history - 처리 이력 추가
     - AlertPolicyQueryController (controller/query/)
       * GET /api/alerts/policies - 정책 목록 조회
       * GET /api/alerts/policies/{id} - 정책 상세 조회
     - AlertEventQueryController (controller/query/)
       * GET /api/alerts/policies/{policyId}/events - 규칙 목록 조회
     - AlertMetricTemplateQueryController (controller/query/)
       * GET /api/alerts/metrics/templates - 메트릭 템플릿 목록 조회 (카테고리 필터링 지원)
     - EventQueryController (controller/query/)
       * GET /api/alerts/events - 이벤트 목록 조회 (필터링, 페이징)
       * GET /api/alerts/events/{id} - 이벤트 상세 조회
       * GET /api/alerts/events/{id}/history - 처리 이력 조회
     - SseAlertController (controller/)
       * GET /api/alerts/sse/stream - SSE 연결 생성

Phase 6: 설정 및 테스트
  ↓
  6.1. 설정 추가 (application.yml)
     - 이메일 SMTP 설정 (spring.mail.*)
     - WebClient Bean 설정 (@Configuration)
     - 비동기 처리 설정 (@EnableAsync, ThreadPoolTaskExecutor)
  
  6.2. 테스트
     - 단위 테스트 (각 Service, Controller 테스트)
     - 통합 테스트 (전체 플로우 테스트)
     - SSE 연결 테스트
     - 이메일/Slack 전송 테스트
```

**구현 우선순위:**
1. **Phase 1** (필수): 데이터베이스 구조 및 엔티티 - 모든 기능의 기반
2. **Phase 2** (필수): 알림 체크 로직 - 알림 발생의 핵심
3. **Phase 3** (필수): 알림 전송 로직 - 사용자에게 알림 전달
4. **Phase 4** (필수): 서비스 통합 - 메트릭 수집과 알림 체크 연결
5. **Phase 5** (필수): API 및 DTO - 사용자 인터페이스
6. **Phase 6** (필수): 설정 및 테스트 - 최종 검증

**의존성 관계:**
- Phase 2는 Phase 1 완료 후 진행 가능 (Repository 필요)
- Phase 3는 Phase 1, Phase 2 완료 후 진행 가능 (Event 엔티티 필요)
- Phase 4는 Phase 1, Phase 2, Phase 3 완료 후 진행 가능 (모든 서비스 필요)
- Phase 5는 Phase 1, Phase 4 완료 후 진행 가능 (Service 필요)
- Phase 6는 모든 Phase 완료 후 진행 (전체 기능 검증)

---

### Phase 1: 데이터베이스 및 도메인 모델

1. 도메인 엔티티 클래스 생성
   - AlertPolicy
   - AlertEvent
   - AlertMetricTemplate
   - Event
   - ProgressHistory
   - Enum 클래스들 (AlertLevel, AlertStatus, DelayTime, AlertCategory)
   - **참고**: 엔티티 생성 후 Hibernate가 자동으로 테이블 생성/수정 (`hibernate.hbm2ddl.auto=update`)
   - **ERD 작성 기준**: plan.md의 "최종 XX 테이블 구조" 섹션의 CREATE TABLE 문 참고

2. Repository 인터페이스 생성
   - AlertPolicyRepository
   - AlertEventRepository
   - AlertMetricTemplateRepository
   - EventRepository
   - ProgressHistoryRepository

3. 메트릭 템플릿 초기 데이터 삽입
   - plan.md의 "6. 카테고리별 메트릭 선정" 섹션에 정의된 모든 메트릭을 ALERT_METRIC_TEMPLATE 테이블에 INSERT
   - 각 카테고리별로 선정된 메트릭의 Graph ID, Metric Key, Metric Name, 기본 임계값 등을 저장

4. (선택) 기존 테이블 수정이 필요한 경우 수동 ALTER TABLE 스크립트 실행
   - **ALERT_EVENT 테이블 수정** (기존 테이블이 있는 경우)
     * POLICY_ID, GRAPH_ID, METRIC_KEY, METRIC_NAME, IS_REVERSE 추가
     * CATEGORY, DELAY_TIME, DAYS, START_TIME, END_TIME 수정
     * INSTANCE_ID 제거 (POLICY를 통해 접근)
   - **EVENT 테이블 수정** (기존 테이블이 있는 경우)
     * INSTANCE_ID, MEMBER_ID, STATUS, CURRENT_VALUE, THRESHOLD_VALUE 추가
     * ACKNOWLEDGED_AT, ACKNOWLEDGED_BY, RESOLVED_AT, RESOLVED_BY 추가
   - **PROGRESS_HISTORY 테이블 수정** (기존 테이블이 있는 경우)
     * EVNET_ID → EVENT_ID 오타 수정
     * USER_NAME → CREATED_BY (NUMBER 타입) 변경
   - **참고**: 신규 테이블(ALERT_POLICY)은 엔티티 생성 시 자동 생성됨

### Phase 2: 알림 체크 로직

1. AlertStateStore 구현 (인메모리 상태 저장, ConcurrentHashMap 사용)
2. AlertCheckService 구현
   - 메서드 시그니처: `checkAlerts(Map<String, Object> finals, Long instanceId)`
   - 활성화된 ALERT_EVENT 조회 (POLICY를 통해 INSTANCE_ID 확인)
   - 메트릭 값 추출 (finals Map에서 직접 추출)
   - 시간 범위 체크 (START_TIME, END_TIME)
   - 요일 체크 (비트마스크 연산, DAYS)
   - 임계값 비교 (WARNING/DANGER/CRITICAL)
   - 누적 시간 확인 (DELAY_TIME, AlertStateStore 사용)
   - Event 생성

### Phase 3: 알림 전송 로직

1. SseAlertService 구현
   - SseEmitter 관리 (Map<Long, SseEmitter>)
   - 하트비트 전송 (30초마다)
   - 실시간 알림 전송
2. EmailAlertService 구현
   - JavaMailSender 설정
   - 이메일 템플릿 작성
3. SlackAlertService 구현
   - WebClient 설정
   - Slack 메시지 포맷 작성
4. AlertNotificationService 구현
   - 알림 수신자 조회 (ALERT_POLICY.member_id - 정책 생성자)
   - 전송 채널 선택
   - 전송 통합

### Phase 4: 서비스 통합

1. CollectorService 통합
   - MetricCollectionTasklet.execute() 수정
   - CollectorService.runOnce(instanceId) 호출 후 finals Map 생성
   - AlertCheckService.checkAlerts(finals, instanceId) 호출 (MetricData DB 저장 전)
   - MetricData 생성 및 DB 저장은 AlertCheckService 호출 후 수행
2. Command Service 구현
   - AlertPolicyCommandService
   - AlertEventCommandService
   - EventCommandService
3. Query Service 구현
   - AlertPolicyQueryService
   - AlertEventQueryService
   - AlertMetricTemplateQueryService
   - EventQueryService

### Phase 5: API 및 DTO

1. DTO 클래스 생성
   - Request DTO (AlertPolicyCreateRequest, AlertEventCreateRequest 등)
   - Response DTO (AlertPolicyResponse, EventResponse 등)
2. Controller 계층 구현
   - AlertPolicyCommandController
   - AlertEventCommandController
   - EventCommandController
   - SseAlertController

### Phase 6: 설정 및 테스트

1. 설정 추가
   - 이메일 SMTP 설정 (application.yml)
   - WebClient 설정 (Bean)
   - 비동기 처리 설정 (@Async)
2. 테스트
   - 단위 테스트
   - 통합 테스트

---

## 8. 아키텍처 구조 및 흐름도

### 8.1 아키텍처 구조

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Layer                          │
│  (Web Browser / Mobile App)                                 │
└──────────────────────┬──────────────────────────────────────┘
                        │
                        │ HTTP / SSE
                        │
┌──────────────────────▼──────────────────────────────────────┐
│                     Controller Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ AlertPolicy  │  │ AlertEvent   │  │ Event        │      │
│  │ Controller   │  │ Controller   │  │ Controller   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐                                          │
│  │ SSE          │                                          │
│  │ Controller   │                                          │
│  └──────────────┘                                          │
└──────────────────────┬──────────────────────────────────────┘
                        │
                        │
┌──────────────────────▼──────────────────────────────────────┐
│                      Service Layer                           │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Command Services                       │    │
│  │  AlertPolicyCommandService                         │    │
│  │  AlertEventCommandService                          │    │
│  │  EventCommandService                               │    │
│  └────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Query Services                         │    │
│  │  AlertPolicyQueryService                           │    │
│  │  AlertEventQueryService                            │    │
│  │  AlertMetricTemplateQueryService                   │    │
│  │  EventQueryService                                 │    │
│  └────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Alert Services                         │    │
│  │  AlertCheckService                                │    │
│  │  AlertNotificationService                         │    │
│  │  ├─ SseAlertService                               │    │
│  │  ├─ EmailAlertService                             │    │
│  │  └─ SlackAlertService                             │    │
│  └────────────────────────────────────────────────────┘    │
└──────────────────────┬──────────────────────────────────────┘
                        │
                        │
┌──────────────────────▼──────────────────────────────────────┐
│                    Repository Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ AlertPolicy  │  │ AlertEvent   │  │ Event        │      │
│  │ Repository   │  │ Repository   │  │ Repository   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │ AlertMetric  │  │ MetricData   │                        │
│  │ Template     │  │ Repository   │                        │
│  │ Repository   │  │              │                        │
│  └──────────────┘  └──────────────┘                        │
└──────────────────────┬──────────────────────────────────────┘
                        │
                        │
┌──────────────────────▼──────────────────────────────────────┐
│                    Database Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ ALERT_POLICY │  │ ALERT_EVENT  │  │ EVENT        │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ ALERT_METRIC │  │ PROGRESS_   │  │ METRIC_DATA  │      │
│  │ _TEMPLATE    │  │ HISTORY      │  │              │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    External Services                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ SMTP Server  │  │ Slack        │  │ Redis        │      │
│  │ (Email)      │  │ Webhook      │  │ (Optional)   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 메트릭 수집 및 알림 체크 흐름도

```
┌─────────────────┐
│   Scheduler     │ (매 분 실행, app.batch.enabled=true)
│  @Scheduled     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ CollectorService│
│   .runOnce()    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ finals Map      │
│ 생성 (255개 지표)│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ AlertCheckService│
│  .checkAlerts() │
│  (finals, instanceId)│
└────────┬────────┘
         │
         ├─ 활성화된 ALERT_EVENT 조회
         │   (POLICY를 통해 INSTANCE_ID 확인)
         │
         ├─ 각 알림 규칙에 대해:
         │   ├─ 시간 범위 체크
         │   ├─ 요일 체크 (비트마스크)
         │   ├─ 메트릭 값 추출 (finals Map에서)
         │   ├─ 임계값 비교
         │   ├─ 누적 시간 확인
         │   └─ 요일/시간대 확인
         │
         ├─ 임계값 초과?
         │
         ├─ YES ──────┐
         │            │
         │            ▼
         │    ┌──────────────┐
         │    │ Event 생성   │
         │    └──────┬───────┘
         │           │
         │           ▼
         │    ┌──────────────────────┐
         │    │ AlertNotification    │
         │    │ Service.sendAlerts() │
         │    └──────┬───────────────┘
         │           │
         │           ├─► 정책 생성자 조회 (ALERT_POLICY.member_id)
         │           ├─► SSE 전송
         │           ├─► 이메일 전송
         │           └─► Slack 전송
         │
         └─ NO ──────► 다음 수집 대기
         │
         ▼
┌─────────────────┐
│ MetricData      │
│ 생성 및 저장     │
└────────┬────────┘
         │
         ▼
   원래 플로우 계속
```

### 8.3 실시간 알림 수신 흐름도 (SSE)

```
┌──────────────┐                    ┌──────────────┐
│   Client     │                    │   Server    │
└──────┬───────┘                    └──────┬──────┘
       │                                    │
       │  GET /api/alerts/sse/stream       │
       │  Authorization: Bearer {token}    │
       ├───────────────────────────────────►
       │                                    │
       │                                    │ 인증 확인
       │                                    │ SseEmitter 생성
       │                                    │ Map<userId, SseEmitter> 저장
       │                                    │
       │  text/event-stream                 │
       │◄──────────────────────────────────┤
       │                                    │
       │  :heartbeat (30초마다)            │
       │◄──────────────────────────────────┤
       │                                    │
       │  알림 발생 시                       │
       │  data: {eventId, severity, ...}   │
       │◄──────────────────────────────────┤
       │                                    │
       │  연결 종료                          │
       ├───────────────────────────────────►
       │                                    │ SseEmitter 정리
       │                                    │ Map에서 제거
       │                                    │
```

### 8.4 알림 정책 생성 및 활성화 흐름도

```
사용자
  │
  ├─► 알림 정책 생성
  │     │
  │     ▼
  │   ALERT_POLICY 저장
  │     │
  │     ├─► 알림 규칙 추가
  │     │     │
  │     │     ▼
  │     │   ALERT_EVENT 저장
  │     │     ├─ CATEGORY 선택
  │     │     ├─ 메트릭 템플릿 선택 (ALERT_METRIC_TEMPLATE에서 조회)
  │     │     │   └─ GRAPH_ID, METRIC_KEY, METRIC_NAME 자동 설정
  │     │     ├─ 임계값 설정 (WARNING/DANGER/CRITICAL, 템플릿 기본값 사용 가능)
  │     │     ├─ DELAY_TIME 설정
  │     │     └─ DAYS/START_TIME/END_TIME 설정
  │     │
  │     └─► 정책 활성화/비활성화
  │           │
  │           ▼
  │         ALERT_POLICY.IS_ACTIVE 설정
  │
  └─► 알림 수신 (정책 생성자만)
        │
        ├─► SSE: 자동 연결 (GET /api/alerts/sse/stream)
        ├─► 이메일: 정책 생성자의 Member.email 사용
        └─► Slack: 정책 생성자의 Member.slackAddress 사용
```

---

## 9. 추가 고려사항

### 9.1 성능 최적화
- 알림 체크는 비동기로 처리 (@Async)
- SSE 연결은 연결 풀 관리 (Map<Long, SseEmitter>)
- 이메일/Slack 전송은 큐를 통한 비동기 처리

### 9.2 에러 처리
- 알림 전송 실패 시 재시도 로직
- 전송 실패 이력 기록
- 사용자에게 전송 실패 알림

### 9.3 보안
- SSE 연결 시 인증 확인 (Spring Security)
- 알림 정책 접근 권한 확인
- 이메일/Slack 주소 검증

### 9.4 모니터링
- 알림 전송 성공률 모니터링
- SSE 연결 수 모니터링
- 알림 발생 빈도 모니터링

### 9.5 환경별 설정
- **개발 환경**: `app.batch.enabled: false` (스케줄러 비활성화)
- **프로덕션 환경**: `app.batch.enabled: true` (스케줄러 활성화)

---

## 10. 테이블 수정/추가 요약

### 10.1 수정 필요한 테이블

1. **ALERT_EVENT** (알림 규칙 테이블)
   - **역할**: 알림 규칙을 정의하는 테이블. 각 알림 규칙은 하나의 그래프와 메트릭에 연결되며, 임계값, 누적 시간, 요일/시간대 등의 조건을 포함합니다.
   - 추가: POLICY_ID, GRAPH_ID, METRIC_KEY, METRIC_NAME, IS_REVERSE
   - 수정: CATEGORY (ENUM → VARCHAR2), DELAY_TIME (ENUM → VARCHAR2), DAYS (NUMBER(1) → NUMBER(3)), START_TIME/END_TIME (TIMESTAMP → VARCHAR2(5))
   - 제거: INSTANCE_ID (POLICY를 통해 접근)

2. **EVENT** (알림 발생 이벤트 테이블)
   - **역할**: 실제로 발생한 알림 이벤트를 저장하는 테이블. 메트릭이 임계값을 초과했을 때 생성되며, 심각도, 현재 값, 임계값 등을 기록합니다.
   - 추가: INSTANCE_ID, MEMBER_ID, STATUS, CURRENT_VALUE, THRESHOLD_VALUE, ACKNOWLEDGED_AT, ACKNOWLEDGED_BY, RESOLVED_AT, RESOLVED_BY
   - 수정: SEVERITY 유지 (1, 2, 3)

3. **PROGRESS_HISTORY** (알림 처리 이력 테이블)
   - **역할**: 알림 이벤트의 처리 이력을 저장하는 테이블. 사용자가 알림을 확인하거나 해결할 때, 또는 추가 조치 사항을 기록할 때 사용됩니다.
   - 수정: EVNET_ID → EVENT_ID (오타 수정)
   - 수정: USER_NAME → CREATED_BY (NUMBER 타입)
   - 추가: CREATED_BY 제약조건

### 10.2 신규 생성 테이블

1. **ALERT_POLICY** (알림 정책 테이블)
   - **역할**: 알림 정책을 관리하는 테이블. 하나의 인스턴스에 대해 하나 이상의 알림 정책을 생성할 수 있으며, 각 정책은 여러 개의 알림 규칙을 포함할 수 있습니다. 정책 생성자(MEMBER_ID)만 해당 정책의 알림을 수신합니다.

2. **ALERT_METRIC_TEMPLATE** (알림 메트릭 템플릿 테이블)
   - **역할**: 카테고리별로 선정된 알림 메트릭 템플릿을 저장하는 테이블. 사용자가 알림 규칙을 생성할 때 선택할 수 있는 메트릭 목록을 제공합니다. 각 템플릿은 Graph ID, Metric Key, 기본 임계값 등을 포함합니다.

### 10.3 사용하지 않는 테이블

- **EVENT_CATEGORY_ENUM** - 코드에서 Enum으로 관리하므로 사용 안 함

---

**문서 버전**: 1.0  
**작성일**: 2024  
**최종 수정일**: 2024


