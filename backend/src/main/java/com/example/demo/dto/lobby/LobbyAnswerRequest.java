package com.example.demo.dto.lobby;

import com.example.demo.dto.room.RoomPlayerRequest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LobbyAnswerRequest extends RoomPlayerRequest {

    @NotNull(message = "Question id is required")
    private Long questionId;

    @NotNull(message = "Answer index is required")
    @Min(value = 0, message = "Answer index must be between 0 and 3")
    @Max(value = 3, message = "Answer index must be between 0 and 3")
    private Integer answerIndex;
}
