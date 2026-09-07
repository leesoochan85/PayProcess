package com.payflow.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e){
        ErrorCode errorCode = e.getErrorCode();

        ErrorResponse response = new ErrorResponse(errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse>handleValidationException(MethodArgumentNotValidException e){
        String message = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        ErrorResponse response = new ErrorResponse("INVALID_REQUEST", message);
        return ResponseEntity.badRequest().body(response);
    }
}
