package com.drone.collab.vo;

import lombok.Data;

@Data
public class OrgPublicVo {
    private Long id;
    private String name;
    private String orgType;
    private String phoneMasked;
    private String address;
    private String status;
    private String contactPerson;
    private String unifiedCreditCode;
}
