package com.example.demo.dto.lobby;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LobbyCountdownPayload {

    private int roundNumber;
    private Instant countdownEndsAt;
    private int activePlayers;
}
