package com.example.demo.dto.duel;

import java.util.List;

import com.example.demo.dto.session.SessionResponse;
import com.example.demo.dto.session.SubmissionResponse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DuelAnswerResponse {

    boolean accepted;
    SubmissionResponse submission;
    SessionResponse session;
    List<DuelPlayerState> leaderboard;
    boolean duelCompleted;
    Long winnerUserId;
}
