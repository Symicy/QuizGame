package com.example.demo.domain;

import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.SourceType;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;
    
    @Column(name = "text", nullable = false, length = 1000)
    private String text;

    @Column(name = "option_a", nullable = false)
    private String optionA;

    @Column(name = "option_b", nullable = false)
    private String optionB;

    @Column(name = "option_c", nullable = false)
    private String optionC;

    @Column(name = "option_d", nullable = false)
    private String optionD;

    @Column(name = "correct_option", nullable = false)
    private Integer correctOption;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false)
    private DifficultyLevel difficultyLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "explanation", length = 2000)
    private String explanation;

    @ManyToOne
    @Column(name = "category_id", nullable = false)
    private Category category;
}
