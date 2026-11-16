package com.drone.collab.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityUser {
    private Long userId;
    private String username;
    private String role;
    private Long pilotId;
    private Long orgId;
}
