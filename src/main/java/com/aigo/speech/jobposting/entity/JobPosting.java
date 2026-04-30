package com.aigo.speech.jobposting.entity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.aigo.speech.global.entity.BaseTimeEntity;
import com.aigo.speech.jobposting.dto.JobPostingAnalyzeResponse;
import com.aigo.speech.jobposting.entity.converter.StringListConverter;
import com.aigo.speech.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "job_postings",
	uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "url"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class JobPosting extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private UUID uuid;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 2048)
	private String url;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private JobPostingStatus status;

	private String companyName;

	@Column(columnDefinition = "TEXT")
	private String companyDescription;

	private String position;

	@Convert(converter = StringListConverter.class)
	@Column(columnDefinition = "TEXT")
	private List<String> responsibilities;

	@Convert(converter = StringListConverter.class)
	@Column(name = "required_skills", columnDefinition = "TEXT")
	private List<String> requiredSkills;

	@Convert(converter = StringListConverter.class)
	@Column(name = "preferred_skills", columnDefinition = "TEXT")
	private List<String> preferredSkills;

	@Column(name = "required_experience")
	private String requiredExperience;

	@Column(columnDefinition = "TEXT")
	private String failureReason;

	@Column(name = "interview_date")
	private LocalDate interviewDate;

	@PrePersist
	protected void prePersist () {
		if (this.uuid == null) {
			this.uuid = UUID.randomUUID();
		}
	}

	public static JobPosting pending (User user, String url) {
		JobPosting jp = new JobPosting();
		jp.user = user;
		jp.url = url;
		jp.status = JobPostingStatus.PENDING;
		return jp;
	}

	public void markAnalyzing () {
		this.status = JobPostingStatus.ANALYZING;
	}

	public void markDone (JobPostingAnalyzeResponse result) {
		this.status = JobPostingStatus.DONE;
		this.companyName = result.companyName();
		this.companyDescription = result.companyDescription();
		this.position = result.position();
		this.responsibilities = result.responsibilities();
		this.requiredSkills = result.requiredSkills();
		this.preferredSkills = result.preferredSkills();
		this.requiredExperience = result.requiredExperience();
	}

	public void markFailed (String reason) {
		this.status = JobPostingStatus.FAILED;
		this.failureReason = reason;
	}

	public void updateCompanyName(String companyName) {
		this.companyName = companyName;
	}
}
