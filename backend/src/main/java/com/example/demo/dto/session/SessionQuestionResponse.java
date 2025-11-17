package com.example.demo.dto.session;

import com.example.demo.domain.Question;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionQuestionResponse {

    private Long id;
    private String text;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private Integer points;
    private Boolean answered;

    public static SessionQuestionResponse fromEntity(Question question, boolean answered) {
        if (question == null) {
            return null;
        }
        return SessionQuestionResponse.builder()
                .id(question.getId())
                .text(question.getText())
                .optionA(question.getOptionA())
                .optionB(question.getOptionB())
                .optionC(question.getOptionC())
                .optionD(question.getOptionD())
                .points(question.getPoints())
                .answered(answered)
                .build();
    }
}
