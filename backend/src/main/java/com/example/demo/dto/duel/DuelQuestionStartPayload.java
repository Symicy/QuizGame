package com.example.demo.dto.duel;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DuelQuestionStartPayload {

    Long questionId;
    Integer questionIndex;
    Integer totalQuestions;
    LocalDateTime startsAt;
}
