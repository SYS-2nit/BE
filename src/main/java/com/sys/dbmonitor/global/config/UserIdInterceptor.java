/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.global.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserIdInterceptor implements HandlerInterceptor {
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final Long DEFAULT_USER_ID = 1L; // 기본 사용자 ID (로그인 기능 구현 전까지 고정)
    
    // 현재 요청의 사용자 ID를 저장하는 ThreadLocal 객체입니다. 이를 통해 스레드 간 데이터 충돌을 방지합니다.
    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();

    // 컨트롤러가 실행되기 전에 사용자 ID를 검증 및 저장합니다.
    // 반환값이 true일 경우 요청 처리가 계속 진행됩니다.
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userIdStr = request.getHeader(USER_ID_HEADER);
        
        // 헤더가 없거나 비어있으면 기본 사용자 ID(1) 사용
        if(userIdStr == null || userIdStr.isEmpty()) {
            currentUserId.set(DEFAULT_USER_ID);
            return true;
        }

        // 헤더가 있으면 파싱하여 사용
        try{
            currentUserId.set(Long.parseLong(userIdStr));       // Thread에 현재 UserId 값 저장
            return true;
        }catch (NumberFormatException e){
            // 파싱 실패 시 기본 사용자 ID 사용
            currentUserId.set(DEFAULT_USER_ID);
            return true;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        currentUserId.remove();     // Thread에 저장된 값 삭제
    }

    // 현재 스레드에 저장된 사용자 ID를 반환
    // 저장된 값이 없으면 기본 사용자 ID(1) 반환
    public static Long getCurrentUserId(){
        Long userId = currentUserId.get();
        if(userId == null) {
            // ThreadLocal에 값이 없으면 기본 사용자 ID 반환
            return DEFAULT_USER_ID;
        }
        return userId;
    }
}
