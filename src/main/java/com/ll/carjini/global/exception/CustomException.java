package com.ll.carjini.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.ll.carjini.global.error.ErrorCode;

@Getter
@AllArgsConstructor
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;
}
