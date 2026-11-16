package com.drone.collab.dto;

import lombok.Data;

@Data
public class OrgSaveRequest {
    private String name;
    private String orgType;
    private String creditCode;
    private String contactName;
    private String phone;
    private String address;
}
