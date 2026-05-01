package com.aigo.speech.curriculum.entity;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.aigo.speech.global.entity.BaseTimeEntity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Curriculum extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(nullable = false, updatable = false)
	private UUID uuid;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_posting_id", nullable = true)
	private JobPosting jobPosting;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CurriculmContent content;

	@Column(nullable = false)
	private LocalDate scheduleDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "interview_schedule_id", nullable = false)
	private InterviewSchedule interviewSchedule;

	public static Curriculum create (
		User user, InterviewSchedule interviewSchedule, CurriculmContent content, LocalDate scheduleDate) {
		Curriculum curriculum = new Curriculum();
		curriculum.uuid = UUID.randomUUID();
		curriculum.user = user;
		curriculum.interviewSchedule = interviewSchedule;
		curriculum.content = content;
		curriculum.scheduleDate = scheduleDate;
		return curriculum;
	}
}
