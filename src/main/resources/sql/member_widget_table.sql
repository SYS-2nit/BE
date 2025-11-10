-- ============================================
-- member_widget 테이블 생성
-- ============================================

-- 시퀀스 생성
CREATE SEQUENCE SEQ_MEMBER_WIDGET_ID
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- 테이블 생성
CREATE TABLE member_widget (
    id NUMBER PRIMARY KEY,
    member_id NUMBER NOT NULL,
    graph_id NUMBER NOT NULL,
    position NUMBER NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_member_widget_graph FOREIGN KEY (graph_id) REFERENCES graph(id),
    CONSTRAINT uk_member_widget_position UNIQUE (member_id, position, is_deleted)
);

-- 인덱스 생성
CREATE INDEX idx_member_widget_member_id ON member_widget(member_id, is_deleted);
CREATE INDEX idx_member_widget_graph_id ON member_widget(graph_id);

COMMIT;

