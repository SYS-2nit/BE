package com.sys.dbmonitor.domains.board.domain;


import com.sys.dbmonitor.global.common.entity.BaseEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class Board extends BaseEntity {

    private Long id;
}