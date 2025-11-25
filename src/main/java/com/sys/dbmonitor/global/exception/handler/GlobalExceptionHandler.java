package com.sys.dbmonitor.global.exception.handler;


import com.sys.dbmonitor.global.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Client Error 4xx - Custom Exception 요청에 문제가 있는 경우
     */

    //예시 코드
    @ExceptionHandler(BadRequestException.class)
    ProblemDetail handleBadRequestException(final BadRequestException e) {
        //status와 에러에 대한 자세한 설명
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        // 아래와 같이 필드 확장 가능
        problemDetail.setTitle("잘못된 요청입니다");

        return problemDetail;
    }

    /**
     * 정적 리소스 없음 예외 처리
     * - favicon.ico, .well-known 등 브라우저 자동 요청은 로그 출력 안 함
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNoResourceFoundException(NoResourceFoundException e) {
        String message = e.getMessage();

        // 브라우저 자동 요청 무시 (로그 출력 안 함)
        if (message != null && (
                message.contains("favicon.ico") ||
                        message.contains(".well-known") ||
                        message.contains("robots.txt") ||
                        message.contains("sitemap.xml")
        )) {
            return; // 조용히 무시
        }

        // 실제 리소스 에러는 로그 출력
        log.error("Resource not found - {}", message);
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthenticationException(final AuthenticationException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        problemDetail.setTitle("인증 실패");

        return problemDetail;
    }

    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail handleForbiddenException(final ForbiddenException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());

        problemDetail.setTitle("접근 권한 없음");
        return problemDetail;
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFoundException(final NotFoundException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());

        problemDetail.setTitle("데이터 없음");
        return problemDetail;
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflictException(final ConflictException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());

        problemDetail.setTitle("데이터 충돌");
        return problemDetail;
    }

    /**
     * Redis 연결 실패 예외 처리
     */
    @ExceptionHandler(RedisConnectionFailureException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ProblemDetail handleRedisConnectionFailureException(final RedisConnectionFailureException e) {
        log.error("Redis 연결 실패 (필수 서비스): {}", e.getMessage(), e);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "Redis 서비스에 연결할 수 없습니다. 시스템 관리자에게 문의하세요."
        );
        problemDetail.setTitle("Redis 서비스 연결 실패");
        return problemDetail;
    }

    /**
     * 클라이언트 연결 끊김 예외 처리 (Broken pipe)
     * - 클라이언트가 요청을 취소하거나 연결을 끊었을 때 발생
     * - 정상적인 상황이므로 조용히 무시
     */
    @ExceptionHandler({ClientAbortException.class, AsyncRequestNotUsableException.class})
    @ResponseStatus(HttpStatus.OK)
    public void handleClientAbortException(Exception e) {
        // Broken pipe는 클라이언트가 연결을 끊은 정상적인 상황
        // DEBUG 레벨로만 로깅하여 로그 노이즈 최소화
        log.debug("클라이언트 연결 끊김 (정상): {}", e.getClass().getSimpleName());
    }

    /**
     * IOException 처리 (Broken pipe 포함)
     * - Broken pipe는 클라이언트 연결 끊김으로 정상적인 상황
     */
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.OK)
    public void handleIOException(IOException e) {
        // Broken pipe는 조용히 무시
        if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
            log.debug("클라이언트 연결 끊김 (Broken pipe): {}", e.getMessage());
            return;
        }
        // 다른 IOException은 로깅
        log.warn("IO 오류 발생: {}", e.getMessage());
    }

    /**
     * Internal Server Error 5xx :
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleInternalError(final Exception e) {
        // Broken pipe 관련 예외는 이미 위에서 처리했으므로 여기서는 무시
        if (e instanceof ClientAbortException || 
            e instanceof AsyncRequestNotUsableException ||
            (e instanceof IOException && e.getMessage() != null && e.getMessage().contains("Broken pipe"))) {
            log.debug("클라이언트 연결 끊김 (정상): {}", e.getClass().getSimpleName());
            return null; // 응답 없음
        }
        
        log.error("Uncaught {} - {}", e.getClass().getSimpleName(), e.getMessage());
        e.printStackTrace();
        ProblemDetail problemDetail = ProblemDetail
                .forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        return problemDetail;
    }


}


