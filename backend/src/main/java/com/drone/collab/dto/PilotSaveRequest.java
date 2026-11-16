package com.drone.collab.dto;

import lombok.Data;

@Data
public class PilotSaveRequest {
    private String fullName;
    private String idCard;
    private String phone;
    private String email;
    private String recordNo;
    private String region;
}
