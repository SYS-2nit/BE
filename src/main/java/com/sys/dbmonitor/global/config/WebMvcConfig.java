package com.sys.dbmonitor.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// UserIdInterceptor를 동작시키기 위한 mvc 설정
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final UserIdInterceptor userIdInterceptor;

    // Interceptors를 사용하기 위한 설정
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // /api/**/coupons/**를 통해 들어올때는 userIdInterceptor가 자동으로 실행됨
        registry.addInterceptor(userIdInterceptor)
                .addPathPatterns("/api/**/coupons/**");
    }
}
