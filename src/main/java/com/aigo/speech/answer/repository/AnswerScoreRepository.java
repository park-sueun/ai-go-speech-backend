package com.aigo.speech.answer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.answer.entity.AnswerScore;

public interface AnswerScoreRepository extends JpaRepository<AnswerScore, Long> {
}
