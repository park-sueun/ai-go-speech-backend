package com.aigo.speech.answer.entity;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.aigo.speech.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "answer_score")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AnswerScore extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "answer_id", nullable = false, unique = true)
	private InterviewAnswer answer;

	@Column(name = "silence_count")
	private Integer silenceCount;

	@Column(name = "total_silence_duration")
	private Integer totalSilenceDuration;

	@Column(name = "answer_duration")
	private Integer answerDuration;

	@Column(name = "silence_score", precision = 5, scale = 2)
	private BigDecimal silenceScore;

	@Column(name = "repeat_score", precision = 5, scale = 2)
	private BigDecimal repeatScore;

	@Column(name = "word_score", precision = 5, scale = 2)
	private BigDecimal wordScore;

	@Column(name = "logic_score", precision = 5, scale = 2)
	private BigDecimal logicScore;

	@Column(name = "speed_score", precision = 5, scale = 2)
	private BigDecimal speedScore;

	public AnswerScore(InterviewAnswer answer, Integer silenceCount,
		Integer totalSilenceDuration, Integer answerDuration) {
		this.answer = answer;
		this.silenceCount = silenceCount;
		this.totalSilenceDuration = totalSilenceDuration;
		this.answerDuration = answerDuration;
	}
}
