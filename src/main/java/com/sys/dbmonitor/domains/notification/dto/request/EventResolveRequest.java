/*
 ******************************************************************
 작성자: 최영준
 ******************************************************************
 */
package com.sys.dbmonitor.domains.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 이벤트 해결 요청 DTO
 * 사용자 ID는 X-User-ID 헤더에서 자동으로 추출됩니다.
 * 이 DTO는 현재 사용되지 않지만, 향후 확장을 위해 유지됩니다.
 */
@Getter
@Setter
@Schema(description = "이벤트 해결 요청 (사용자 ID는 헤더에서 자동 추출)")
public class EventResolveRequest {
    // memberId 필드 제거 - X-User-ID 헤더에서 자동 추출
}

