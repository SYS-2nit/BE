-- ============================================
-- member_widget 테이블 더미 데이터 삽입
-- 유저 1에 대한 위젯 설정 9개 생성
-- ============================================

-- 기존 유저 1의 위젯 설정이 있다면 삭제 (선택사항)
-- DELETE FROM member_widget WHERE member_id = 1;
-- COMMIT;

-- CUSTOM 카테고리의 그래프 중 처음 9개를 선택하여 유저 1의 위젯 설정으로 저장
-- position은 1부터 9까지 순서대로 할당

INSERT INTO member_widget (id, member_id, graph_id, position, created_at, updated_at, is_deleted)
SELECT
    SEQ_MEMBER_WIDGET_ID.NEXTVAL,
    1 AS member_id,
    graph_id,
    ROWNUM AS position,
    SYSTIMESTAMP AS created_at,
    SYSTIMESTAMP AS updated_at,
    0 AS is_deleted
FROM (
    SELECT id AS graph_id
    FROM graph
    WHERE category = 'CUSTOM'
    ORDER BY id
    FETCH FIRST 9 ROWS ONLY
);

COMMIT;

-- 삽입 확인
SELECT 
    mw.id,
    mw.member_id,
    mw.graph_id,
    mw.position,
    g.name AS graph_name,
    g.category,
    mw.created_at,
    mw.updated_at,
    mw.is_deleted
FROM member_widget mw
JOIN graph g ON mw.graph_id = g.id
WHERE mw.member_id = 1
  AND mw.is_deleted = 0
ORDER BY mw.position;

