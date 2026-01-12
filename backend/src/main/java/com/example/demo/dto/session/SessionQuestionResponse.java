package com.example.demo.dto.session;

import com.example.demo.domain.Question;
import com.example.demo.domain.Submission;
import com.example.demo.enums.SessionStatus;

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
    private Integer selectedOption;
    private Integer correctOption;
    private Boolean correct;

    public static SessionQuestionResponse fromEntity(Question question, Submission submission, SessionStatus status) {
        if (question == null) {
            return null;
        }
        boolean answered = submission != null;
        boolean revealAnswer = answered || SessionStatus.COMPLETED.equals(status);
        return SessionQuestionResponse.builder()
                .id(question.getId())
                .text(question.getText())
                .optionA(question.getOptionA())
                .optionB(question.getOptionB())
                .optionC(question.getOptionC())
                .optionD(question.getOptionD())
                .points(question.getPoints())
                .answered(answered)
                .selectedOption(submission != null ? submission.getAnswerIndex() : null)
                .correctOption(revealAnswer ? question.getCorrectOption() : null)
                .correct(submission != null ? submission.getIsCorrect() : null)
                .build();
    }
}
