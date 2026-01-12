package com.example.demo.dto.duel;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DuelResultPayload {

    Long winnerUserId;
    LocalDateTime finishedAt;
    List<DuelPlayerState> leaderboard;
}
