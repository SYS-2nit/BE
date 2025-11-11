-- ============================================
-- graph 테이블 더미 데이터 삽입
-- ============================================

-- 기존 테스트 데이터가 있다면 삭제 (선택사항)
-- DELETE FROM graph WHERE is_deleted = 'N';
-- COMMIT;

-- CPU 카테고리 그래프들
INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('CPU Utilization', 'CPU', 'CPU 사용률 그래프', 1, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('CPU Load Average', 'CPU', 'CPU 로드 평균 그래프', 1, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('CPU Wait Time', 'CPU', 'CPU 대기 시간 그래프', 1, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('CPU Saturation', 'CPU', 'CPU 포화도 그래프', 1, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Top SQL by CPU', 'CPU', 'CPU 사용량 상위 SQL 그래프', 1, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- MEMORY 카테고리 그래프들
INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Memory Usage', 'MEMORY', '메모리 사용량 그래프', 2, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('SGA Usage', 'MEMORY', 'SGA 사용량 그래프', 2, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('PGA Usage', 'MEMORY', 'PGA 사용량 그래프', 2, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Buffer Cache Hit Ratio', 'MEMORY', '버퍼 캐시 적중률 그래프', 2, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Shared Pool Usage', 'MEMORY', 'Shared Pool 사용량 그래프', 2, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- SESSION 카테고리 그래프들
INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Active Sessions', 'SESSION', '활성 세션 수 그래프', 3, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Session Usage', 'SESSION', '세션 사용률 그래프', 3, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Lock Wait', 'SESSION', '락 대기 그래프', 3, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Top Blockers', 'SESSION', '상위 블로커 그래프', 3, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- IO 카테고리 그래프들
INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('IO Wait Time', 'IO', 'IO 대기 시간 그래프', 4, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Physical Reads', 'IO', '물리적 읽기 그래프', 4, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Cache Hit Ratio', 'IO', '캐시 적중률 그래프', 4, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Redo Generation', 'IO', 'Redo 생성량 그래프', 4, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Top Datafiles by IO', 'IO', 'IO 사용량 상위 데이터파일 그래프', 4, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

-- STORAGE 카테고리 그래프들
INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Tablespace Usage', 'STORAGE', '테이블스페이스 사용률 그래프', 5, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('FRA Usage', 'STORAGE', 'FRA 사용률 그래프', 5, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Temp Usage', 'STORAGE', '임시 테이블스페이스 사용률 그래프', 5, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Undo Usage', 'STORAGE', 'Undo 사용률 그래프', 5, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

INSERT INTO graph (name, category, info, type, created_at, updated_at, is_deleted) VALUES
('Top Segments by Size', 'STORAGE', '크기 기준 상위 세그먼트 그래프', 5, SYSTIMESTAMP, SYSTIMESTAMP, 'N');

COMMIT;

-- ============================================
-- 삽입된 데이터 확인
-- ============================================

-- 카테고리별 그래프 개수 확인
SELECT 
    category,
    COUNT(*) as graph_count
FROM graph
WHERE is_deleted = 'N'
GROUP BY category
ORDER BY category;

-- 전체 그래프 목록 확인
SELECT 
    id,
    name,
    category,
    info,
    type,
    created_at
FROM graph
WHERE is_deleted = 'N'
ORDER BY category, id;




