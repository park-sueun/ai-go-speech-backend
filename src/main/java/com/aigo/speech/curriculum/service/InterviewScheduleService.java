package com.aigo.speech.curriculum.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.curriculum.dto.CurriculumResponse;
import com.aigo.speech.curriculum.dto.InterviewScheduleRequest;
import com.aigo.speech.curriculum.dto.InterviewScheduleResponse;
import com.aigo.speech.curriculum.entity.InterviewSchedule;
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
	private final CurriculmService curriculmService;

	@Transactional
	public InterviewScheduleResponse register (UUID userUuid, InterviewScheduleRequest request) {
		User user = userRepository.findByUuid(userUuid).orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));
		InterviewSchedule schedule = InterviewSchedule.create(
			user,
			request.companyName(),
			request.interviewDate()
		);
		InterviewSchedule savedSchedule = scheduleRepository.save(schedule);
		List<CurriculumResponse> curriculum = curriculmService.generateCurriculum(user, savedSchedule)
			.stream()
			.limit(5)
			.toList();

		return InterviewScheduleResponse.of(savedSchedule, curriculum);
	}
}
