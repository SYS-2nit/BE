package com.sys.dbmonitor.domains.board.service;


import com.sys.dbmonitor.domains.board.dao.BoardRepository;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;


    @Override
    public int testCount() {

        Integer num =  boardRepository.getDeptCount()
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.LOGIN_FAILED));
        return num;
    }
}
