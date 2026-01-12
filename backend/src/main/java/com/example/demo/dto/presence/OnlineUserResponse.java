package com.example.demo.dto.presence;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUserResponse {

    private Long id;
    private String username;
    private String role;
    private Instant lastSeenAt;
}
