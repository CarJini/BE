package com.ll.demo03.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "제공된 입력 값이 유효하지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 요청 방식입니다."),
    ENTITY_NOT_FOUND(HttpStatus.BAD_REQUEST, "요청한 엔티티를 찾을 수 없습니다."),
    MISSING_INPUT_VALUE(HttpStatus.BAD_REQUEST, "필수 입력 값이 누락되었습니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Token
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "액세스 토큰이 만료되었습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),

    //크레딧
    NO_CREDIT(HttpStatus.BAD_REQUEST, "크레딧이 부족합니다."),

    // 이미지 없음
    IMAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 이미지가 존재하지 않습니다."),


    // User
    NOT_FOUND_USER(HttpStatus.BAD_REQUEST, "해당 유저가 존재하지 않습니다"),

    //에러 추가해서 넣으시면 됩니다!
    ACCESS_DENIED(HttpStatus.UNAUTHORIZED, "인증되지 않은 유저입니다."),
    SC_FORBIDDEN(HttpStatus.UNAUTHORIZED, "권한이 없는 유저입니다."),
    INVALID_JWT_SIGNATURE(HttpStatus.UNAUTHORIZED, "서명 검증에 실패했습니다." ),
    ILLEGAL_REGISTRATION_ID(HttpStatus.BAD_REQUEST,"해당 사항이 없는 로그인 경로입니다."),
    DUPLICATED_METHOD(HttpStatus.METHOD_NOT_ALLOWED, "이미 적용된 사항입니다"),

    DUPLICATED_NICKNAME(HttpStatus.BAD_REQUEST, "이미 존재하는 닉네임입니다."),

    INAPPROPRIATE_CONTENT(HttpStatus.BAD_REQUEST, "부적절한 프롬프트입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
