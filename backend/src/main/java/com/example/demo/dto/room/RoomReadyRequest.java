package com.example.demo.dto.room;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoomReadyRequest extends RoomPlayerRequest {

    @NotNull(message = "Ready flag is required")
    private Boolean ready;
}
