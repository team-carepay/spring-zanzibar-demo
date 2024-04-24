package com.carepay.demo.exception;

import java.lang.reflect.ParameterizedType;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends ApplicationException {
}
