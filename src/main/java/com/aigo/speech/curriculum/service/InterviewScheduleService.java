package com.aigo.speech.curriculum.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.curriculum.dto.CurriculumResponse;
import com.aigo.speech.curriculum.dto.InterviewScheduleListResponse;
import com.aigo.speech.curriculum.dto.InterviewScheduleRequest;
import com.aigo.speech.curriculum.dto.InterviewScheduleResponse;
import com.aigo.speech.curriculum.dto.InterviewScheduleUpdateRequest;
import com.aigo.speech.curriculum.entity.InterviewSchedule;
import com.aigo.speech.curriculum.exception.UnauthorizedCurriculumException;
import com.aigo.speech.curriculum.repository.InterviewScheduleRepository;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewScheduleService {
	private final InterviewScheduleRepository scheduleRepository;
	private final UserRepository userRepository;
	private final CurriculumService curriculumService;

	@Transactional
	public InterviewScheduleResponse register (UUID userUuid, InterviewScheduleRequest request) {
		User user = userRepository.findByUuid(userUuid).orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));
		InterviewSchedule schedule = InterviewSchedule.create(
			user,
			request.companyName(),
			request.interviewDate()
		);
		InterviewSchedule savedSchedule = scheduleRepository.save(schedule);
		List<CurriculumResponse> curriculum = curriculumService.generateCurriculum(user, savedSchedule)
			.stream()
			.limit(5)
			.toList();

		return InterviewScheduleResponse.of(savedSchedule, curriculum);
	}

	public List<InterviewScheduleListResponse> getSchedules (UUID userUuid) {
		User user = userRepository.findByUuid(userUuid)
			.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));
		return scheduleRepository.findByUserOrderByInterviewDateAsc(user)
			.stream()
			.map(InterviewScheduleListResponse::from)
			.toList();
	}

	public InterviewScheduleResponse getSchedule (UUID userUuid, UUID scheduleUuid) {
		InterviewSchedule schedule = findSchedule(scheduleUuid);
		validateOwner(schedule, userUuid);
		List<CurriculumResponse> curriculums = curriculumService.getCurriculumsBySchedule(
			userUuid, scheduleUuid, null
		);
		return InterviewScheduleResponse.of(schedule, curriculums);
	}

	@Transactional
	public InterviewScheduleResponse updateSchedule (
		UUID userUuid, UUID scheduleUuid, InterviewScheduleUpdateRequest request) {
		InterviewSchedule schedule = findSchedule(scheduleUuid);
		validateOwner(schedule, userUuid);
		boolean isDateChanged = !schedule.getInterviewDate().equals(request.interviewDate());
		schedule.update(request.companyName(), request.interviewDate());
		if (isDateChanged) {
			// 날짜 변경 시 커리큘럼 삭제 후 재생성
			curriculumService.deleteAllBySchedule(schedule);
			User user = schedule.getUser();
			List<CurriculumResponse> curriculums = curriculumService.generateCurriculum(user, schedule)
				.stream()
				.limit(5)
				.toList();
			return InterviewScheduleResponse.of(schedule, curriculums);
		}

		List<CurriculumResponse> curriculums = curriculumService.getCurriculumsBySchedule(
			userUuid, scheduleUuid, LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
		return InterviewScheduleResponse.of(schedule, curriculums);
	}

	@Transactional
	public void deleteSchedule (UUID userUuid, UUID scheduleUuid) {
		InterviewSchedule schedule = findSchedule(scheduleUuid);
		validateOwner(schedule, userUuid);
		curriculumService.deleteAllBySchedule(schedule);
		scheduleRepository.delete(schedule);
	}

	private InterviewSchedule findSchedule (UUID scheduleUuid) {
		return scheduleRepository.findByUuid(scheduleUuid)
			.orElseThrow(() -> new IllegalArgumentException("해당 일정을 찾을 수 없습니다."));
	}

	private void validateOwner (InterviewSchedule schedule, UUID userUuid) {
		if (!schedule.getUser().getUuid().equals(userUuid)) {
			throw new UnauthorizedCurriculumException("해당 일정에 접근할 권한이 없습니다.");
		}
	}
}
