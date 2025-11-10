-- Graph 테이블 데이터 삽입
-- 시퀀스 SEQ_GRAPH_ID를 사용하여 ID 생성

-- Graph 데이터 삽입
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'PGA / SGA 압박률', 'CUSTOM', null, 7);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'AAS', 'CUSTOM', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Wait Class 분포', 'CUSTOM', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'CPU 사용(호스트 vs DB CPU)', 'CUSTOM', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'I/O 지연량', 'CUSTOM', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'I/O 처리량', 'CUSTOM', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'SGA 압박(FreeMB/Reloads)', 'CUSTOM', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, '세션 한도/급증', 'CUSTOM', null, 3);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, '아카이브 로그 적체/목적지 FULL', 'CUSTOM', null, 3);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, '핵심 테이블스페이스 여유율', 'CUSTOM', null, 5);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, '백그라운드 프로세스 상태', 'CUSTOM', null, 7);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, '제한 근접 파라미터 감시', 'CUSTOM', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'CPU Activity Overview Tiles', 'CPU', null, 7);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'DB CPU Saturation (AAS vs Core)', 'CPU', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Host CPU Utilization (%) – Trend', 'CPU', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'DB CPU Share of Host (%) – Trend', 'CPU', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Run Queue per Core (Scheduler Load)', 'CPU', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'CPU Cost per Commit/Execution (ms)', 'CPU', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Foreground vs Background CPU — AAS Trend', 'CPU', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Top SQL by CPU_1m', 'CPU', null, 5);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'PGA Execution Memory & Processes', 'MEMORY', null, 7);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'SGA Efficiency & Memory Pools', 'MEMORY', null, 7);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'PGA Utilization (%) – Trend', 'MEMORY', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'SGA Utilization (%) — Trend', 'MEMORY', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Workarea Spill Rate (%) – Trend', 'MEMORY', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Library Cache Reloads per Second – Trend', 'MEMORY', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Buffer Cache Miss Rate (%) – Proxy – Trend', 'MEMORY', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Top SQL by Shared Pool Memory — Bar', 'MEMORY', null, 5);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Active vs Inactive Sessions — Trend', 'SESSION', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'On-CPU vs Wait (AAS 분해) — Trend', 'SESSION', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Lock Wait Sessions — TX vs TM vs Total', 'SESSION', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'TPS — Trend', 'SESSION', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Exec/s — Trend', 'SESSION', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Logons/sec & Disconnects/sec — Trend', 'SESSION', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Session Activity & Resource Summary', 'SESSION', null, 7);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Top Blocker Sessions — Snapshot Top 5', 'SESSION', null, 5);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'I/O Performance Dashboard', 'IO', null, 7);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Direct Path I/O (개/초)', 'IO', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'SQL Parsing & Execution (개/초)', 'IO', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Physical Reads vs Logical Reads (개/초)', 'IO', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Average I/O Wait Time (ms)', 'IO', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Redo Generation Rate (MB/초)', 'IO', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'DBWR Checkpoint Activity', 'IO', null, 8);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, '데이터파일별 I/O 통계 (Top 5)', 'IO', null, 5);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Storage Health Dashboard', 'STORAGE', null, 7);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Temp Tablespace Active Usage (GB)', 'STORAGE', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, '테이블스페이스 사용률 추세 (%)', 'STORAGE', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, '테이블스페이스 증가 추세 (GB/일)', 'STORAGE', null, 2);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'FRA 사용률 추세 (%)', 'STORAGE', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Undo 사용률 추세 (%)', 'STORAGE', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, 'Total Database Usage Trend (%)', 'STORAGE', null, 1);
INSERT INTO graph (id, name, category, info, type) VALUES (SEQ_GRAPH_ID.NEXTVAL, '대용량 세그먼트 (Top 5)', 'STORAGE', null, 5);

COMMIT;

-- 삽입 확인
SELECT * FROM graph ORDER BY id;
