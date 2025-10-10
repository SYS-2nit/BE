package com.sys.dbmonitor.domains.coupon.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CouponPageController {

    @GetMapping("/ui/coupon-test")
    public String couponTestPage() {
        return "coupon_test";
    }
}

