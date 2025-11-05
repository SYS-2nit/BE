CREATE TABLE graph (
                       id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       name VARCHAR2(64) NOT NULL,
                       category VARCHAR2(32) NOT NULL,
                       info VARCHAR2(255),
                       type NUMBER NOT NULL,
                       created_at TIMESTAMP DEFAULT SYSDATE NOT NULL,
                       updated_at TIMESTAMP DEFAULT SYSDATE NOT NULL,
                       is_deleted CHAR(1) DEFAULT 'N' NOT NULL
);