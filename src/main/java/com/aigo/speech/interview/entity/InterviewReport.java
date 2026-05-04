package com.aigo.speech.interview.entity;

import java.util.UUID;

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
@Table(name = "interview_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewReport extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false, updatable = false)
	private UUID uuid;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id", nullable = false, unique = true)
	private InterviewSession session;

	@Lob
	@Column(name = "ai_summary", columnDefinition = "TEXT")
	private String aiSummary;

	@Column(name = "silence_score")
	private Integer silenceScore;

	@Column(name = "filler_score")
	private Integer fillerScore;

	@Column(name = "logic_score")
	private Integer logicScore;

	@Column(name = "total_score")
	private Integer totalScore;

	public InterviewReport (
		InterviewSession session, String aiSummary,
		Integer silenceScore, Integer fillerScore, Integer logicScore, Integer totalScore
	) {
		this.uuid = UUID.randomUUID();
		this.session = session;
		this.aiSummary = aiSummary;
		this.silenceScore = silenceScore;
		this.fillerScore = fillerScore;
		this.logicScore = logicScore;
		this.totalScore = totalScore;
	}
}
