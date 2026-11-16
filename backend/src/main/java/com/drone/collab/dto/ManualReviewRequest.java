package com.drone.collab.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManualReviewRequest {
    @NotBlank
    private String decision;
    private String comment;
}
