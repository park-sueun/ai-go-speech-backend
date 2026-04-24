package com.aigo.speech.interview.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.interview.entity.AnswerScore;

public interface AnswerScoreRepository extends JpaRepository<AnswerScore, Long> {
}
