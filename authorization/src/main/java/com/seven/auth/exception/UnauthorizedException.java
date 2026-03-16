package com.seven.auth.exception;

public class UnauthorizedException extends AuthorizationException{
    public UnauthorizedException(String message) {super(message);}
}
