package com.ll.demo03.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.ll.demo03.global.error.ErrorCode;

@Getter
@AllArgsConstructor
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;
}
