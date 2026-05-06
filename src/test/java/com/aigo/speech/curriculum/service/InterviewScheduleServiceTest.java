package com.aigo.speech.curriculum.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.curriculum.dto.CurriculumResponse;
import com.aigo.speech.curriculum.dto.InterviewScheduleRequest;
import com.aigo.speech.curriculum.dto.InterviewScheduleResponse;
import com.aigo.speech.curriculum.entity.InterviewSchedule;
import com.aigo.speech.curriculum.repository.InterviewScheduleRepository;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class InterviewScheduleServiceTest {

	@Mock
	private InterviewScheduleRepository scheduleRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private CurriculumService curriculumService;

	@InjectMocks
	private InterviewScheduleService interviewScheduleService;

	@Test
	void register () {
	}

	private final UUID USER_UUID = UUID.randomUUID();
	private final String COMPANY_NAME = "음어그";
	private final LocalDateTime INTERVIEW_DATE = LocalDateTime.of(2026, 5, 20, 14, 0);

	@Test
	@DisplayName("면접 일정을 등록하면 일정 저장 후 커리큘럼이 생성된다")
	void register_success () {
		User user = User.builder().email("test@example.com").build();
		ReflectionTestUtils.setField(user, "uuid", USER_UUID);

		InterviewScheduleRequest request = new InterviewScheduleRequest(COMPANY_NAME, INTERVIEW_DATE.toLocalDate());

		InterviewSchedule savedSchedule = InterviewSchedule.create(user, COMPANY_NAME, INTERVIEW_DATE.toLocalDate());
		ReflectionTestUtils.setField(savedSchedule, "id", 1L);

		List<CurriculumResponse> mockCurriculum = List.of(
			new CurriculumResponse(UUID.randomUUID(), null, "1차 모의면접", LocalDate.now(), false),
			new CurriculumResponse(UUID.randomUUID(), null, "2차 모의면접", LocalDate.now(), false)
		);

		given(userRepository.findByUuid(USER_UUID)).willReturn(Optional.of(user));
		given(scheduleRepository.save(any(InterviewSchedule.class))).willReturn(savedSchedule);
		given(curriculumService.generateCurriculum(any(User.class), any(InterviewSchedule.class)))
			.willReturn(mockCurriculum);

		InterviewScheduleResponse response = interviewScheduleService.register(USER_UUID, request);

		assertThat(response.companyName()).isEqualTo(COMPANY_NAME);
		assertThat(response.curriculums()).hasSize(2);
		assertThat(response.curriculums().get(0).content()).isEqualTo("1차 모의면접");

		verify(userRepository).findByUuid(USER_UUID);
		verify(scheduleRepository).save(any(InterviewSchedule.class));
		verify(curriculumService).generateCurriculum(any(User.class), any(InterviewSchedule.class));
	}

	@Test
	@DisplayName("존재하지 않는 사용자의 경우 일정 등록 시 예외가 발생한다")
	void register_fail_userNotFound () {
		InterviewScheduleRequest request = new InterviewScheduleRequest(COMPANY_NAME, INTERVIEW_DATE.toLocalDate());
		given(userRepository.findByUuid(USER_UUID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> interviewScheduleService.register(USER_UUID, request))
			.isInstanceOf(UserNotFoundException.class)
			.hasMessage("존재하지 않는 사용자입니다.");

		verify(scheduleRepository, never()).save(any());
	}

}