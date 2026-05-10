package com.aigo.speech.interview.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.entity.InterviewStatus;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.user.entity.User;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
	Optional<InterviewSession> findByUuid(UUID uuid);
	List<InterviewSession> findByUserOrderByCreatedAtDesc(User user);
	List<InterviewSession> findByUserAndStatusOrderByCreatedAtDesc(User user, InterviewStatus status);
	boolean existsByUserAndJobPostingAndStatus(User user, JobPosting jobPosting, InterviewStatus status);
	long countByUserAndJobPostingAndStatus(User user, JobPosting jobPosting, InterviewStatus status);
	List<InterviewSession> findByUserAndJobPostingOrderByCreatedAtAsc(User user, JobPosting jobPosting);

	@Modifying
	void deleteAllByUser(User user);
}
