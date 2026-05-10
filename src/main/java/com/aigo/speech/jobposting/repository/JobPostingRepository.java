package com.aigo.speech.jobposting.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.user.entity.User;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

	Optional<JobPosting> findByUuid(UUID uuid);

	Optional<JobPosting> findByUserAndUrl(User user, String url);

	Optional<JobPosting> findByUuidAndUser(UUID uuid, User user);

	List<JobPosting> findAllByUserOrderByCreatedAtDesc(User user);

	@Modifying
	void deleteAllByUser(User user);
}
