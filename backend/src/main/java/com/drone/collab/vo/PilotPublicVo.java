package com.drone.collab.vo;

import lombok.Data;

@Data
public class PilotPublicVo {
    private Long id;
    private String fullName;
    private String phoneMasked;
    private String region;
    private String recordNo;
    private String status;
    private String filingNo;
    private int certCount;
}
