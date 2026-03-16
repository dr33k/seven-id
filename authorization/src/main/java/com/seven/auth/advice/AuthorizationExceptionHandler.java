package com.seven.auth.advice;

import com.seven.auth.exception.*;
import com.seven.auth.dto.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static com.seven.auth.dto.response.Responder.*;

@ControllerAdvice
public class AuthorizationExceptionHandler {
    @ExceptionHandler(AuthorizationException.class)
    protected ResponseEntity <Response> handleAuthorizationException(AuthorizationException ex) {
        Class<? extends AuthorizationException> exClass = ex.getClass();
        if (exClass.equals(ClientException.class)) {
            return badRequest(ex.getMessage());
        } else if (exClass.equals(NotFoundException.class)) {
            return notFound(ex.getMessage());
        } else if (exClass.equals(UnauthorizedException.class)) {
            return unauthorized(ex.getMessage());
        } else if (exClass.equals(ForbiddenException.class)) {
            return forbidden(ex.getMessage());
        } else if (exClass.equals(ConflictException.class)) {
            return conflict(ex.getMessage());
        }
        return internalServerError(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidationExceptions(MethodArgumentNotValidException e){
        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach(error -> {

            String fieldName;
            String errorMessage;

            if (error instanceof FieldError) {
                fieldName = ((FieldError) error).getField();
                errorMessage = error.getDefaultMessage();
            } else {
                fieldName = error.getObjectName();
                errorMessage = error.getDefaultMessage();
            }
            errors.put(fieldName, errorMessage);
        });
        return badRequest(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response> handleHttpMessageNotReadableException(HttpMessageNotReadableException e){
        return badRequest(e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Response> handleHttpMessageNotReadableException(ResponseStatusException e){
        return forbidden(e.getMessage());
    }
}
