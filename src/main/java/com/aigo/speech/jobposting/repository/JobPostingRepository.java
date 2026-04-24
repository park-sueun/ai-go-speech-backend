package com.aigo.speech.jobposting.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.jobposting.entity.JobPosting;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
	Optional<JobPosting> findByUuid(UUID uuid);
}
