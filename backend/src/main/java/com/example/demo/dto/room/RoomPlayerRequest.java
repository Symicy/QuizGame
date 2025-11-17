package com.example.demo.dto.room;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomPlayerRequest {

    @NotNull(message = "User id is required")
    private Long userId;
}
