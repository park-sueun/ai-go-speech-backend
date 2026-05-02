package com.aigo.speech.curriculum.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.aigo.speech.curriculum.entity.InterviewSchedule;

public record InterviewScheduleListResponse(
	UUID scheduleUuid,
	String companyName,
	LocalDate interviewDate
) {
	public static InterviewScheduleListResponse from (InterviewSchedule schedule) {
		return new InterviewScheduleListResponse(
			schedule.getUuid(),
			schedule.getCompanyName(),
			schedule.getInterviewDate()
		);
	}
}
