package com.aigo.speech.answer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.answer.entity.AnswerReview;

public interface AnswerReviewRepository extends JpaRepository<AnswerReview, Long> {
}
