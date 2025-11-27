package com.sys.dbmonitor.domains.sql.util;

/**************************************************
 작성자 : 오수경
 *************************************************/

/**
 * SQL 통계 집계를 위한 클래스
 * 한 번의 Stream 순회로 모든 통계를 계산하기 위해 사용
 */
public class SqlStats {
    public long elapsed = 0L;
    public long cpu = 0L;
    public long exec = 0L;
    public long buffer = 0L;
    public long disk = 0L;
    public long wait = 0L;
    public long waitTime = 0L;
    public long waitUserIo = 0L;
    public long waitConcurrency = 0L;
    public long waitApplication = 0L;
    public long waitCluster = 0L;
}

