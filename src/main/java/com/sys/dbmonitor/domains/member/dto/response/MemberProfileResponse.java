package com.sys.dbmonitor.domains.member.dto.response;

public record MemberProfileResponse(
        Long userId,
        String userName,
        String email,
        String name
){
}