package com.carepay.demo.security;

import java.io.Serializable;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ZanzibarPermissionEvaluator implements PermissionEvaluator {
    private final ZanzibarClient zanzibarClient;

    public boolean hasPermission(final Authentication authentication, final Object targetDomainObject, final Object permission) {
        throw new IllegalArgumentException("Permission check not supported without namespace");
    }

    public boolean hasPermission(final Authentication authentication, final Serializable targetId, final String targetType, final Object permission) {
        return zanzibarClient.checkPermission(authentication, targetType, targetId.toString(), permission.toString());
    }
}