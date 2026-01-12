package com.example.demo.dto.room;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomInviteRequest {

    @NotNull
    private Long inviterId;

    @NotNull
    private Long targetUserId;
}
