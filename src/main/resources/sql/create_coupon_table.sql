-- 1. 기존 테이블 삭제
DROP TABLE COUPON CASCADE CONSTRAINTS;
DROP SEQUENCE SEQ_COUPON_ID;

-- 2. 시퀀스 생성
CREATE SEQUENCE SEQ_COUPON_ID
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- 3. 쿠폰 테이블 생성 (간소화)
CREATE TABLE COUPON (
                        ID              NUMBER          DEFAULT SEQ_COUPON_ID.NEXTVAL NOT NULL,
                        USER_ID         NUMBER(19)      NOT NULL,
                        COUPON_CODE     VARCHAR2(50)    NOT NULL,
                        STATUS          VARCHAR2(20)    DEFAULT 'AVAILABLE' NOT NULL,
                        ORDER_ID        NUMBER(19),
                        USED_AT         TIMESTAMP,
                        CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
                        UPDATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,

    -- 제약조건
                        CONSTRAINT PK_COUPON PRIMARY KEY (ID),
                        CONSTRAINT UK_COUPON_CODE UNIQUE (COUPON_CODE),
                        CONSTRAINT CHK_COUPON_STATUS CHECK (STATUS IN ('AVAILABLE', 'USED', 'EXPIRED', 'CANCELLED'))
);

-- 4. 인덱스 생성
CREATE INDEX IDX_COUPON_USER_ID ON COUPON(USER_ID);
CREATE INDEX IDX_COUPON_STATUS ON COUPON(STATUS);
CREATE INDEX IDX_COUPON_CREATED_AT ON COUPON(CREATED_AT DESC);
CREATE INDEX IDX_COUPON_ORDER_ID ON COUPON(ORDER_ID);


-- 6. 수정일시 자동 업데이트 트리거
CREATE OR REPLACE TRIGGER TRG_COUPON_UPDATE
    BEFORE UPDATE ON COUPON
                      FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;
/

COMMIT;


-- =====================================================
-- 쿠폰 발급 카운트 증가 프로시저 (COUPON_CONFIG 사용) 무한 대기
-- =====================================================

CREATE OR REPLACE PROCEDURE SP_INCREMENT_COUPON_COUNT(
    p_updated_rows OUT NUMBER
)
    IS
    v_config_id NUMBER;
    v_total_quantity NUMBER;
    v_issued_count NUMBER;
BEGIN
    -- FOR UPDATE (대기 모드)
SELECT CONFIG_ID, TOTAL_QUANTITY, ISSUED_COUNT
INTO v_config_id, v_total_quantity, v_issued_count
FROM COUPON_CONFIG
WHERE IS_ACTIVE = 'Y'
  AND CONFIG_NAME = 'DEFAULT'
    FOR UPDATE;  -- NOWAIT 제거 → 순서대로 대기

-- 수량 체크
IF v_issued_count >= v_total_quantity THEN
        p_updated_rows := 0;
        RETURN;
END IF;

    -- 카운트 증가
UPDATE COUPON_CONFIG
SET ISSUED_COUNT = ISSUED_COUNT + 1
WHERE CONFIG_ID = v_config_id;

p_updated_rows := SQL%ROWCOUNT;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_updated_rows := 0;
WHEN OTHERS THEN
        p_updated_rows := 0;
        RAISE;
END SP_INCREMENT_COUPON_COUNT;
/










-- =====================================================
-- 쿠폰 기본 정책 테이블 (단일 행)
-- =====================================================

-- 1. 기존 테이블 삭제 (재생성 시)
-- DROP TABLE COUPON_CONFIG CASCADE CONSTRAINTS;
-- DROP SEQUENCE SEQ_COUPON_CONFIG_ID;

-- 2. 시퀀스 생성
CREATE SEQUENCE SEQ_COUPON_CONFIG_ID
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- 3. 쿠폰 설정 테이블 생성
CREATE TABLE COUPON_CONFIG (
                               CONFIG_ID       NUMBER(19)      NOT NULL,           -- 설정 ID (PK)
                               CONFIG_NAME     VARCHAR2(100)   DEFAULT 'DEFAULT' NOT NULL, -- 설정 이름
                               TOTAL_QUANTITY  NUMBER(10)      NOT NULL,           -- 총 발행 가능 수량
                               ISSUED_COUNT    NUMBER(10)      DEFAULT 0 NOT NULL, -- 현재 발행된 수량
                               IS_ACTIVE       CHAR(1)         DEFAULT 'Y' NOT NULL, -- 활성화 여부 (Y/N)
                               CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
                               UPDATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,

    -- 제약조건
                               CONSTRAINT PK_COUPON_CONFIG PRIMARY KEY (CONFIG_ID),
                               CONSTRAINT UK_CONFIG_NAME UNIQUE (CONFIG_NAME),
                               CONSTRAINT CHK_IS_ACTIVE CHECK (IS_ACTIVE IN ('Y', 'N')),
                               CONSTRAINT CHK_CONFIG_TOTAL_QUANTITY CHECK (TOTAL_QUANTITY > 0),
                               CONSTRAINT CHK_CONFIG_ISSUED_COUNT CHECK (ISSUED_COUNT >= 0),
                               CONSTRAINT CHK_CONFIG_ISSUED_LE_TOTAL CHECK (ISSUED_COUNT <= TOTAL_QUANTITY)
);

-- 4. 인덱스 생성
CREATE INDEX IDX_CONFIG_ACTIVE ON COUPON_CONFIG(IS_ACTIVE);


-- 6. 수정일시 자동 업데이트 트리거
CREATE OR REPLACE TRIGGER TRG_COUPON_CONFIG_UPDATE
    BEFORE UPDATE ON COUPON_CONFIG
                      FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;
/

-- 7. 기본 설정 데이터 삽입
INSERT INTO COUPON_CONFIG (CONFIG_ID, CONFIG_NAME, TOTAL_QUANTITY, ISSUED_COUNT, IS_ACTIVE)
VALUES (SEQ_COUPON_CONFIG_ID.NEXTVAL, 'DEFAULT', 10000, 0, 'Y');

COMMIT;

-- 8. 생성 확인
SELECT * FROM COUPON_CONFIG;

