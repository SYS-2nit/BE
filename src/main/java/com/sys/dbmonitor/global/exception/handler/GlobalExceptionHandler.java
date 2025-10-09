package com.sys.dbmonitor.global.exception.handler;


import com.sys.dbmonitor.global.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
     * Internal Server Error 5xx :
     * 예외처리가 제대로 되지 않았거나 코드 자체의 문제인 경우일 확률 높음 코드를 고치거나 해당 예외처리 핸들러를 추가해줘야 함
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleInternalError(final Exception e) {
        log.error("Uncaught {} - {}", e.getClass().getSimpleName(), e.getMessage());
        e.printStackTrace();
        ProblemDetail problemDetail = ProblemDetail
                .forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        return problemDetail;
    }


}


