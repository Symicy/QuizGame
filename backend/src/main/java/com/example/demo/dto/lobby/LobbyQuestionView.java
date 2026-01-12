package com.example.demo.dto.lobby;

import com.example.demo.enums.DifficultyLevel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LobbyQuestionView {

    private Long id;
    private String text;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private DifficultyLevel difficulty;
}
