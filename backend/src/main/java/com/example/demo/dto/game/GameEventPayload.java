package com.example.demo.dto.game;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameEventPayload<T> {

    private String type;
    private String roomCode;
    private T payload;
}
