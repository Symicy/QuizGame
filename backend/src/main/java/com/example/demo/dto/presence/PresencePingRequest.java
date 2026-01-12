package com.example.demo.dto.presence;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PresencePingRequest {

    @NotNull
    private Long userId;
}
