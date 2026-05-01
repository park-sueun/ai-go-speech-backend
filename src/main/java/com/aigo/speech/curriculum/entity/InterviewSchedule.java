package com.aigo.speech.curriculum.entity;

import java.time.LocalDate;
import java.util.UUID;

import com.aigo.speech.global.entity.BaseTimeEntity;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewSchedule extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private UUID uuid;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	private String companyName;

	@Column(name = "interview_date")
	private LocalDate interviewDate;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_posting_id", nullable = true)
	private JobPosting jobPosting;

	public static InterviewSchedule create (User user, String companyName, LocalDate interviewDate) {
		InterviewSchedule schedule = new InterviewSchedule();
		schedule.uuid = UUID.randomUUID();
		schedule.user = user;
		schedule.companyName = companyName;
		schedule.interviewDate = interviewDate;
		return schedule;
	}
}
