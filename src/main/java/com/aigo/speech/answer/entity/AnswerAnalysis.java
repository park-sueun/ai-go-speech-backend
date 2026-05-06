package com.aigo.speech.answer.entity;

import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.aigo.speech.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "answer_analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerAnalysis extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false, updatable = false)
	private UUID uuid;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "answer_id", nullable = false, unique = true)
	private InterviewAnswer answer;

	@Column(name = "total_silence_duration", nullable = false)
	private Integer totalSilenceDuration; // ms단위

	@Column(name = "silence_count", nullable = false)
	private Integer silenceCount;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "filler_words", columnDefinition = "json")
	private Map<String, Integer> fillerWords; // {"음": 3, "어": 2, "그": 1}

	@Lob
	@Column(name = "ai_review", columnDefinition = "TEXT")
	private String aiReview;

	@Column(name = "logic_score", nullable = false)
	private Integer logicScore;

	@Column(name = "silence_score", nullable = false)
	private Integer silenceScore;

	@Column(name = "filler_score", nullable = false)
	private Integer fillerScore;

	@Column(name = "total_score", nullable = false)
	private Integer totalScore;

	public AnswerAnalysis (
		InterviewAnswer answer, Integer totalSilenceDuration, Integer silenceCount,
		Map<String, Integer> fillerWords, String aiReview,
		Integer logicScore, Integer silenceScore, Integer fillerScore, Integer totalScore
	) {
		this.uuid = UUID.randomUUID();
		this.answer = answer;
		this.totalSilenceDuration = totalSilenceDuration;
		this.silenceCount = silenceCount;
		this.fillerWords = fillerWords;
		this.aiReview = aiReview;
		this.logicScore = logicScore;
		this.silenceScore = silenceScore;
		this.fillerScore = fillerScore;
		this.totalScore = totalScore;
	}
}
