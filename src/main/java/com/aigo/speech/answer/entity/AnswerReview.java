package com.aigo.speech.answer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "answer_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerReview {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "answer_id", nullable = false, unique = true)
	private InterviewAnswer answer;

	@Column(precision = 5, scale = 2)
	private BigDecimal score;

	@Column(columnDefinition = "TEXT")
	private String comment;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public AnswerReview(InterviewAnswer answer) {
		this.answer = answer;
		this.createdAt = LocalDateTime.now();
	}
}
