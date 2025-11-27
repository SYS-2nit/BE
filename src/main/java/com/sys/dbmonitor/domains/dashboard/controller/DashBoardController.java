/**************************************************
 작성자 : 최온유
 *************************************************/

package com.sys.dbmonitor.domains.dashboard.controller;


import com.sys.dbmonitor.domains.dashboard.service.CollectorService;
import com.sys.dbmonitor.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;


@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
@Tag(name = "TEST Command API", description = "TEST Command API")
public class DashBoardController {

}
