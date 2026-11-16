package com.drone.collab.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CertificateSaveRequest {
    private String certType;
    private String certNo;
    private LocalDate validUntil;
    private String attachmentUrl;
}
