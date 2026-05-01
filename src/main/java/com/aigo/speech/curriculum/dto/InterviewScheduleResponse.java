package com.aigo.speech.curriculum.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.aigo.speech.curriculum.entity.InterviewSchedule;

public record InterviewScheduleResponse(
	UUID scheduleUuid,
	String companyName,
	LocalDate interviewDate,
	List<CurriculumResponse> curriculums
) {
	public static InterviewScheduleResponse of (InterviewSchedule schedule, List<CurriculumResponse> curriculums) {
		return new InterviewScheduleResponse(
			schedule.getUuid(),
			schedule.getCompanyName(),
			schedule.getInterviewDate(),
			curriculums
		);
	}
}
