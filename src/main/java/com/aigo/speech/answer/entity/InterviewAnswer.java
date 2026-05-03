package com.aigo.speech.answer.entity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.aigo.speech.global.entity.BaseTimeEntity;
import com.aigo.speech.jobposting.entity.converter.StringListConverter;
import com.aigo.speech.question.entity.InterviewQuestion;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@EntityListeners(AuditingEntityListener.class)
public class InterviewAnswer extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false, updatable = false)
	private UUID uuid;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_id", nullable = false)
	private InterviewQuestion question;

	@Column(name = "total_elapsed_ms")
	private Integer totalElapsedMs;

	@Column(columnDefinition = "TEXT")
	private String transcript;

	@Convert(converter = StringListConverter.class)
	@Column(name = "filler_words", columnDefinition = "TEXT")
	private List<String> fillerWords;

	@Column(name = "silence_periods_json", columnDefinition = "TEXT")
	private String silencePeriodsJson;

	@Column(name = "speech_periods_json", columnDefinition = "TEXT")
	private String speechPeriodsJson;

	public InterviewAnswer(InterviewQuestion question, Integer totalElapsedMs, String transcript,
		List<String> fillerWords, String silencePeriodsJson, String speechPeriodsJson) {
		this.uuid = UUID.randomUUID();
		this.question = question;
		this.totalElapsedMs = totalElapsedMs;
		this.transcript = transcript;
		this.fillerWords = fillerWords;
		this.silencePeriodsJson = silencePeriodsJson;
		this.speechPeriodsJson = speechPeriodsJson;
	}

	public FillerStats getFillerStats() {
		if (fillerWords == null || fillerWords.isEmpty()) {
			return new FillerStats(0, Map.of());
		}
		Map<String, Long> counts = fillerWords.stream()
			.collect(Collectors.groupingBy(w -> w, Collectors.counting()));
		return new FillerStats(fillerWords.size(), counts);
	}

	public record FillerStats(int total, Map<String, Long> counts) {}
}
