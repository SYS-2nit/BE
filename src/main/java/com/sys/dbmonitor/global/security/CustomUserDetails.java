package com.sys.dbmonitor.global.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final String userName;
    private final String password;
    private final String email;
    private final String name;
    private final Long userId;
    private final Collection<? extends GrantedAuthority> authorities;


    // 고정된 사용자 정보를 반환하는 팩토리 메서드
    public static CustomUserDetails createAdminUser() {
        return new CustomUserDetails(
                "admin",
                "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKyVqUJxHjJ3YjJ3YjJ3YjJ3YjJ3Y", // admin123
                "admin@dbmonitor.com",
                "관리자",
                1L,
                Collections.singletonList(() -> "ROLE_USER")
        );
    }

    public static CustomUserDetails createDeveloperUser() {
        return new CustomUserDetails(
                "developer",
                "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKyVqUJxHjJ3YjJ3YjJ3YjJ3YjJ3Y", // test123
                "test@dbmonitor.com",
                "테스트 사용자",
                2L,
                Collections.singletonList(() -> "ROLE_USER")
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}