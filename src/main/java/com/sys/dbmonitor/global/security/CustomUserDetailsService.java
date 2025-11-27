/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.global.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 고정된 사용자 정보 반환
        switch (username) {
            case "admin":
                return CustomUserDetails.createAdminUser();
            case "developer":
                return CustomUserDetails.createDeveloperUser();
            default:
                throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
        }
    }
}