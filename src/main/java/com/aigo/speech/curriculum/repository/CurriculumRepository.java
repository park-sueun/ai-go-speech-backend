package com.aigo.speech.curriculum.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.curriculum.entity.Curriculum;
import com.aigo.speech.curriculum.entity.InterviewSchedule;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

	Optional<Curriculum> findByUuid (UUID uuid);

	List<Curriculum> findByInterviewScheduleOrderByScheduleDateAsc (InterviewSchedule schedule);

	List<Curriculum> findByUserIdAndScheduleDate (Long userId, LocalDate scheduleDate);

	void deleteAllByInterviewSchedule (InterviewSchedule schedule);
}
