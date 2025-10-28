package com.sys.dbmonitor.domains.swingbench.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwingBenchPageController {

    @GetMapping("/ui/swingbench")
    public String swingBenchPage() {
        return "swingbench";
    }
}

