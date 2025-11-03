package com.sys.dbmonitor.global.common.entity;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;


@Getter
@Service
public abstract class MybatisBaseEntity {

    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Boolean isDeleted;

    // 생성 시 자동 호출할 메서드
    public void onCreate() {
        this.createdAt = Timestamp.valueOf(LocalDateTime.now());
        this.updatedAt = Timestamp.valueOf(LocalDateTime.now());
        this.isDeleted = false;
    }

    // 수정 시 자동 호출할 메서드
    public void onUpdate() {
        this.updatedAt = Timestamp.valueOf(LocalDateTime.now());
    }

    // 삭제 시 자동 호출할 메서드
    public void markAsDeleted() {
        this.isDeleted = true;
        this.updatedAt = Timestamp.valueOf(LocalDateTime.now());
    }
}
