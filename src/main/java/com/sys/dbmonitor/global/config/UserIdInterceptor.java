package com.sys.dbmonitor.global.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserIdInterceptor implements HandlerInterceptor {
    private static final String USER_ID_HEADER = "X-User-ID";
    // 현재 요청의 사용자 ID를 저장하는 ThreadLocal 객체입니다. 이를 통해 스레드 간 데이터 충돌을 방지합니다.
    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();

    // 컨트롤러가 실행되기 전에 사용자 ID를 검증 및 저장합니다.
    // 반환값이 true일 경우 요청 처리가 계속 진행됩니다.
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userIdStr = request.getHeader(USER_ID_HEADER);
        if(userIdStr == null || userIdStr.isEmpty()) {
            throw new IllegalStateException("X-User-ID header is required");
        }

        try{
            currentUserId.set(Long.parseLong(userIdStr));       // Thread에 현재 UserId 값 저장
            return true;
        }catch (NumberFormatException e){
            throw new IllegalStateException("Invalid X-User-ID header");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        currentUserId.remove();     // Thread애 저장된 값 삭제
    }

    // 현재 스레드에 저장된 사용자 ID를 반환
    public static Long getCurrentUserId(){
        Long userId = currentUserId.get();
        if(userId == null) {
            throw new IllegalStateException("X-User-ID header is required");
        }
        return userId;
    }
}
