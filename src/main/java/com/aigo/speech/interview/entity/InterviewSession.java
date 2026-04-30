package com.aigo.speech.interview.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.aigo.speech.global.entity.BaseTimeEntity;
import com.aigo.speech.interview.exception.InvalidSessionStatusException;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "interview_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class InterviewSession extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false, updatable = false)
	private UUID uuid;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private InterviewStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_posting_id", nullable = true)
	private JobPosting jobPosting;

	@Column(name = "retry", nullable = false)
	private boolean retry;

	@Column(name = "interview_date")
	private LocalDate interviewDate;

	private LocalDateTime startedAt;

	private LocalDateTime endedAt;

	public InterviewSession (User user, JobPosting jobPosting, boolean retry, LocalDate interviewDate) {
		this.uuid = UUID.randomUUID();
		this.user = user;
		this.jobPosting = jobPosting;
		this.retry = retry;
		this.interviewDate = interviewDate;
		this.status = InterviewStatus.READY;
	}

	public void start () {
		if (this.status != InterviewStatus.READY) {
			throw new InvalidSessionStatusException("READY 상태에서만 시작할 수 있습니다.");
		}
		this.status = InterviewStatus.IN_PROGRESS;
		this.startedAt = LocalDateTime.now();
	}

	public void complete () {
		if (this.status != InterviewStatus.IN_PROGRESS) {
			throw new InvalidSessionStatusException("진행 중인 면접 세션만 완료할 수 있습니다.");
		}
		this.status = InterviewStatus.COMPLETED;
		this.endedAt = LocalDateTime.now();
	}
}
