package com.drone.collab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    /** PILOT、ORG（线上报名）或 GUEST（访客，仅公开查询） */
    @NotBlank
    @Pattern(regexp = "PILOT|ORG|GUEST")
    private String role;
    /** 飞手姓名或单位名称；访客可选 */
    private String displayName;
    private String phone;
    private String email;
    private String idCard;
    private String recordNo;
    private String region;
    private String orgType;
    private String creditCode;
    private String contactName;
    private String address;
}
