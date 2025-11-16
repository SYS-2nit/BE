-- ============================================
-- metric_data 테이블을 metric_data_test로 이름 변경
-- 기존 데이터를 보존하면서 테이블명만 변경
-- ============================================

-- 1. 테이블 이름 변경
ALTER TABLE metric_data RENAME TO metric_data_test;

-- 2. 인덱스 이름 변경 (존재하는 경우)
-- 인덱스가 존재하는지 확인 후 변경d
BEGIN
    BEGIN
        EXECUTE IMMEDIATE 'ALTER INDEX idx_metric_db_graph_time RENAME TO idx_metric_db_graph_time_test';
        DBMS_OUTPUT.PUT_LINE('idx_metric_db_graph_time 변경 완료');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -1418 THEN  -- ORA-01418: specified index does not exist
                DBMS_OUTPUT.PUT_LINE('idx_metric_db_graph_time 인덱스가 존재하지 않습니다.');
            ELSE
                DBMS_OUTPUT.PUT_LINE('idx_metric_db_graph_time 변경 중 오류: ' || SQLERRM);
            END IF;
    END;
    
    BEGIN
        EXECUTE IMMEDIATE 'ALTER INDEX idx_metric_interval_type RENAME TO idx_metric_interval_type_test';
        DBMS_OUTPUT.PUT_LINE('idx_metric_interval_type 변경 완료');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -1418 THEN
                DBMS_OUTPUT.PUT_LINE('idx_metric_interval_type 인덱스가 존재하지 않습니다.');
            ELSE
                DBMS_OUTPUT.PUT_LINE('idx_metric_interval_type 변경 중 오류: ' || SQLERRM);
            END IF;
    END;
END;
/

-- 3. 제약조건 이름 변경 (존재하는 경우)
BEGIN
    BEGIN
        EXECUTE IMMEDIATE 'ALTER TABLE metric_data_test RENAME CONSTRAINT fk_metric_graph TO fk_metric_graph_test';
        DBMS_OUTPUT.PUT_LINE('fk_metric_graph 제약조건 변경 완료');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -2443 THEN  -- ORA-02443: Cannot drop constraint - nonexistent constraint
                DBMS_OUTPUT.PUT_LINE('fk_metric_graph 제약조건이 존재하지 않습니다.');
            ELSE
                DBMS_OUTPUT.PUT_LINE('fk_metric_graph 변경 중 오류: ' || SQLERRM);
            END IF;
    END;
    
    BEGIN
        EXECUTE IMMEDIATE 'ALTER TABLE metric_data_test RENAME CONSTRAINT fk_metric_instance TO fk_metric_instance_test';
        DBMS_OUTPUT.PUT_LINE('fk_metric_instance 제약조건 변경 완료');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -2443 THEN
                DBMS_OUTPUT.PUT_LINE('fk_metric_instance 제약조건이 존재하지 않습니다.');
            ELSE
                DBMS_OUTPUT.PUT_LINE('fk_metric_instance 변경 중 오류: ' || SQLERRM);
            END IF;
    END;
END;
/

-- 4. 파티션 관련 프로시저 업데이트 (존재하는 경우)
-- rename_partition_daily_proc 프로시저를 삭제하고 재생성
BEGIN
    BEGIN
        EXECUTE IMMEDIATE 'DROP PROCEDURE rename_partition_daily_proc';
        DBMS_OUTPUT.PUT_LINE('기존 rename_partition_daily_proc 프로시저 삭제 완료');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -4043 THEN  -- ORA-04043: object does not exist
                DBMS_OUTPUT.PUT_LINE('rename_partition_daily_proc 프로시저가 존재하지 않습니다.');
            ELSE
                RAISE;
            END IF;
    END;
END;
/

-- 5. 프로시저 재생성 (metric_data_test 테이블용)
CREATE OR REPLACE PROCEDURE rename_partition_daily_proc
AS
    v_partition_name VARCHAR2(100);
    v_new_part_name VARCHAR2(100);
    v_new_subpart_name VARCHAR2(100);
    v_interval_type VARCHAR2(10);
    v_high_value DATE;
    v_date_str VARCHAR2(8);
    v_high_value_str VARCHAR2(20);
    v_renamed_count NUMBER := 0;
BEGIN
    -- 모든 자동 생성된 파티션(SYS_P로 시작) 찾아서 이름 변경
    FOR part_rec IN (
        SELECT 
            partition_name,
            high_value
        FROM user_tab_partitions
        WHERE table_name = 'METRIC_DATA_TEST'
          AND partition_name LIKE 'SYS_P%'
          AND partition_name != 'P_START'
        ORDER BY partition_position
    ) LOOP
        BEGIN
            -- high_value를 DATE로 변환
            EXECUTE IMMEDIATE 'SELECT ' || part_rec.high_value || ' FROM DUAL' INTO v_high_value;
            
            -- 파티션 날짜 계산 (high_value는 다음날 00:00:00이므로 -1일)
            v_date_str := TO_CHAR(v_high_value - 1, 'YYYYMMDD');
            v_new_part_name := 'P_' || v_date_str;
            
            -- 파티션이 이미 변경되었는지 확인
            IF part_rec.partition_name != v_new_part_name THEN
                -- 파티션명 변경
                EXECUTE IMMEDIATE 'ALTER TABLE metric_data_test RENAME PARTITION ' || part_rec.partition_name || ' TO ' || v_new_part_name;
                v_renamed_count := v_renamed_count + 1;
                
                -- 해당 파티션의 서브파티션명 변경
                FOR subpart_rec IN (
                    SELECT 
                        subpartition_name,
                        high_value
                    FROM user_tab_subpartitions
                    WHERE table_name = 'METRIC_DATA_TEST'
                      AND partition_name = v_new_part_name
                      AND subpartition_name LIKE 'SYS_SUBP%'
                ) LOOP
                    BEGIN
                        -- interval_type 값 추출
                        v_interval_type := REPLACE(REPLACE(SUBSTR(subpart_rec.high_value, 2, LENGTH(subpart_rec.high_value) - 2), '''', ''), ' ', '');
                        v_new_subpart_name := 'SP_' || UPPER(v_interval_type);
                        
                        -- 서브파티션명 변경
                        EXECUTE IMMEDIATE 'ALTER TABLE metric_data_test RENAME SUBPARTITION ' || subpart_rec.subpartition_name || ' TO ' || v_new_subpart_name;
                    EXCEPTION
                        WHEN OTHERS THEN
                            -- 중복 이름 등 에러는 무시
                            NULL;
                    END;
                END LOOP;
            END IF;
            
        EXCEPTION
            WHEN OTHERS THEN
                -- 에러 발생 시에도 계속 진행
                NULL;
        END;
    END LOOP;
    
    -- P_START 파티션의 서브파티션명도 변경 (처음 한 번만)
    FOR subpart_rec IN (
        SELECT 
            subpartition_name,
            high_value
        FROM user_tab_subpartitions
        WHERE table_name = 'METRIC_DATA_TEST'
          AND partition_name = 'P_START'
          AND (subpartition_name LIKE 'SYS_SUBP%' OR subpartition_name LIKE 'SP_START_%')
    ) LOOP
        BEGIN
            -- interval_type 값 추출
            v_interval_type := REPLACE(REPLACE(SUBSTR(subpart_rec.high_value, 2, LENGTH(subpart_rec.high_value) - 2), '''', ''), ' ', '');
            v_new_subpart_name := 'SP_' || UPPER(v_interval_type);
            
            -- 이미 변경되었는지 확인
            IF subpart_rec.subpartition_name != v_new_subpart_name THEN
                EXECUTE IMMEDIATE 'ALTER TABLE metric_data_test RENAME SUBPARTITION ' || subpart_rec.subpartition_name || ' TO ' || v_new_subpart_name;
            END IF;
        EXCEPTION
            WHEN OTHERS THEN
                NULL;
        END;
    END LOOP;
    
EXCEPTION
    WHEN OTHERS THEN
        -- 최상위 레벨 에러도 무시
        NULL;
END;
/

-- 6. 스케줄러 업데이트 (존재하는 경우)
BEGIN
    BEGIN
        DBMS_SCHEDULER.DROP_JOB('RENAME_PARTITION_DAILY', TRUE);
        DBMS_OUTPUT.PUT_LINE('기존 스케줄러 삭제 완료');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -27475 THEN  -- ORA-27475: job does not exist
                DBMS_OUTPUT.PUT_LINE('RENAME_PARTITION_DAILY 스케줄러가 존재하지 않습니다.');
            ELSE
                DBMS_OUTPUT.PUT_LINE('스케줄러 삭제 중 오류: ' || SQLERRM);
            END IF;
    END;
    
    -- 스케줄러 재생성
    BEGIN
        DBMS_SCHEDULER.CREATE_JOB(
            job_name        => 'RENAME_PARTITION_DAILY',
            job_type        => 'STORED_PROCEDURE',
            job_action      => 'rename_partition_daily_proc',
            start_date      => SYSTIMESTAMP,
            repeat_interval => 'FREQ=DAILY; BYHOUR=01; BYMINUTE=00',
            enabled         => TRUE,
            comments        => '매일 새로 생성된 파티션과 서브파티션의 이름 자동 변경 (metric_data_test)'
        );
        DBMS_OUTPUT.PUT_LINE('새 스케줄러 생성 완료');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('스케줄러 생성 중 오류: ' || SQLERRM);
    END;
END;
/

-- 7. 변경 확인
SELECT 
    'metric_data_test 테이블 존재 확인' as check_type,
    table_name,
    num_rows,
    last_analyzed
FROM user_tables
WHERE table_name = 'METRIC_DATA_TEST';

-- 8. 파티션 확인 (파티션 테이블인 경우)
SELECT 
    'partition 확인' as check_type,
    partition_name,
    high_value,
    num_rows
FROM user_tab_partitions
WHERE table_name = 'METRIC_DATA_TEST'
ORDER BY partition_position;

COMMIT;

DBMS_OUTPUT.PUT_LINE('===========================================');
DBMS_OUTPUT.PUT_LINE('테이블 이름 변경 완료!');
DBMS_OUTPUT.PUT_LINE('metric_data -> metric_data_test');
DBMS_OUTPUT.PUT_LINE('===========================================');

