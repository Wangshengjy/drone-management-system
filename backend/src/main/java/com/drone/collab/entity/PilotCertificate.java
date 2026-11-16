package com.drone.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pilot_certificate")
public class PilotCertificate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pilotId;
    private String certType;
    private String certNo;
    private LocalDate validUntil;
    private String attachmentUrl;
    private LocalDateTime createdAt;
}
