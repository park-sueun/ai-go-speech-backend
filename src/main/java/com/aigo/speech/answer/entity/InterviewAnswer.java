package com.aigo.speech.answer.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.aigo.speech.global.entity.BaseTimeEntity;
import com.aigo.speech.question.entity.InterviewQuestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interview_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewAnswer extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false, updatable = false)
	private UUID uuid;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_id", nullable = false)
	private InterviewQuestion question;

	@Column(name = "audio_url", nullable = false, length = 1024)
	private String audioUrl;

	@Column(name = "stt_text", columnDefinition = "TEXT")
	private String sttText;

	private Integer duration;

	@Column(name = "silence_intervals_json", columnDefinition = "TEXT")
	private String silenceIntervalsJson;

	@Column(name = "answer_started_at")
	private LocalDateTime answerStartedAt;

	@Column(name = "answer_ended_at")
	private LocalDateTime answerEndedAt;

	public InterviewAnswer(InterviewQuestion question, String audioUrl, String sttText,
		Integer duration, String silenceIntervalsJson,
		LocalDateTime answerStartedAt, LocalDateTime answerEndedAt) {
		this.uuid = UUID.randomUUID();
		this.question = question;
		this.audioUrl = audioUrl;
		this.sttText = sttText;
		this.duration = duration;
		this.silenceIntervalsJson = silenceIntervalsJson;
		this.answerStartedAt = answerStartedAt;
		this.answerEndedAt = answerEndedAt;
	}
}
