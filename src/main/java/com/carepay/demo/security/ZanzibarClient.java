package com.carepay.demo.security;

import java.util.Set;

import org.springframework.security.core.Authentication;

public interface ZanzibarClient {
    String addRelation(String resourceType, Object resourceId, String relation, String subjectType, Object subjectId);

    boolean checkPermission(String resourceType, Object resourceId, String permission, String subjectType, Object subjectId);

    boolean checkPermission(Authentication authentication, String resourceType, Object resourceId, String permission);

    Set<String> getAllowedResources(String resourceType, String permission, String subjectType, Object subjectId);

    Set<String> getAllowedResources(Authentication authentication, String resourceType, String permission);

    Set<String> getAllowedResources(String resourceType, String permission);
}
