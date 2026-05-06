package com.aigo.speech.interview.entity;

import java.util.UUID;

import com.aigo.speech.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	public enum ReportStatus {
		GENERATING, COMPLETED, FAILED
	}

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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReportStatus status = ReportStatus.GENERATING;

	// GENERATING 상태로 선삽입 — 중복 방지 (session_id UNIQUE 제약)
	public InterviewReport(InterviewSession session) {
		this.uuid = UUID.randomUUID();
		this.session = session;
		this.status = ReportStatus.GENERATING;
	}

	public void complete(
		String aiSummary, Integer silenceScore, Integer fillerScore,
		Integer logicScore, Integer totalScore
	) {
		this.aiSummary = aiSummary;
		this.silenceScore = silenceScore;
		this.fillerScore = fillerScore;
		this.logicScore = logicScore;
		this.totalScore = totalScore;
		this.status = ReportStatus.COMPLETED;
	}

	public void fail() {
		this.status = ReportStatus.FAILED;
	}

	public void retry() {
		this.status = ReportStatus.GENERATING;
	}
}
