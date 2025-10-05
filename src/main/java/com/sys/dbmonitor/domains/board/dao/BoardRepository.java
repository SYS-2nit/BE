package com.sys.dbmonitor.domains.board.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface BoardRepository {
    Optional<Integer> getDeptCount();
}
