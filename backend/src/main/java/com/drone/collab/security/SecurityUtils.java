package com.drone.collab.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static SecurityUser requireUser() {
        SecurityUser u = currentUser();
        if (u == null || u.getUserId() == null) {
            throw new com.drone.collab.common.BusinessException(401, "未登录");
        }
        return u;
    }

    public static SecurityUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser)) {
            return null;
        }
        return (SecurityUser) auth.getPrincipal();
    }
}
