package com.seven.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Res<T> {
    private T data;
    private HttpStatus status;
    private Boolean isError;
    private String message;
    private LocalDateTime timestamp;
    private String token;
}