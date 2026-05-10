package com.aigo.speech.curriculum.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.aigo.speech.curriculum.entity.Curriculum;
import com.aigo.speech.curriculum.entity.InterviewSchedule;
import com.aigo.speech.user.entity.User;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

	Optional<Curriculum> findByUuid (UUID uuid);

	List<Curriculum> findByInterviewScheduleOrderByScheduleDateAsc (InterviewSchedule schedule);

	List<Curriculum> findByUserUuidAndScheduleDate (UUID userUuid, LocalDate scheduleDate);

	void deleteAllByInterviewSchedule (InterviewSchedule schedule);

	@Modifying
	void deleteAllByUser (User user);
}
