/**************************************************
 작성자 : 최온유
 *************************************************/

package com.sys.dbmonitor.domains.dashboard.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
@Tag(name = "TEST Command API", description = "TEST Command API")
public class DashBoardController {

}
