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
import com.aigo.speech.curriculum.dto.InterviewScheduleUpdateRequest;
import com.aigo.speech.curriculum.entity.InterviewSchedule;
import com.aigo.speech.curriculum.exception.UnauthorizedCurriculumException;
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
			new CurriculumResponse(UUID.randomUUID(), null, null, "1차 모의면접", LocalDate.now(), false),
			new CurriculumResponse(UUID.randomUUID(), null, null, "2차 모의면접", LocalDate.now(), false)
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

	@Test
	@DisplayName("면접 날짜를 변경하면 커리큘럼이 삭제 후 재생성된다")
	void updateSchedule_dateChanged_regeneratesCurriculum () {
		UUID scheduleUuid = UUID.randomUUID();
		User user = User.builder().email("test@example.com").build();
		ReflectionTestUtils.setField(user, "uuid", USER_UUID);

		LocalDate originalDate = LocalDate.of(2026, 5, 20);
		LocalDate newDate = LocalDate.of(2026, 6, 10);
		InterviewSchedule schedule = InterviewSchedule.create(user, COMPANY_NAME, originalDate);
		ReflectionTestUtils.setField(schedule, "uuid", scheduleUuid);

		List<CurriculumResponse> regenerated = List.of(
			new CurriculumResponse(UUID.randomUUID(), null, null, "1차 모의면접", newDate, false)
		);

		given(scheduleRepository.findByUuid(scheduleUuid)).willReturn(Optional.of(schedule));
		given(curriculumService.generateCurriculum(any(User.class), any(InterviewSchedule.class)))
			.willReturn(regenerated);

		InterviewScheduleUpdateRequest request = new InterviewScheduleUpdateRequest(newDate);
		InterviewScheduleResponse response = interviewScheduleService.updateSchedule(USER_UUID, scheduleUuid, request);

		assertThat(response.companyName()).isEqualTo(COMPANY_NAME);
		assertThat(schedule.getInterviewDate()).isEqualTo(newDate);
		verify(curriculumService).deleteAllBySchedule(schedule);
		verify(curriculumService).generateCurriculum(any(User.class), any(InterviewSchedule.class));
	}

	@Test
	@DisplayName("면접 날짜가 동일하면 커리큘럼을 재생성하지 않는다")
	void updateSchedule_sameDateNotChanged_doesNotRegenerateCurriculum () {
		UUID scheduleUuid = UUID.randomUUID();
		User user = User.builder().email("test@example.com").build();
		ReflectionTestUtils.setField(user, "uuid", USER_UUID);

		LocalDate sameDate = LocalDate.of(2026, 5, 20);
		InterviewSchedule schedule = InterviewSchedule.create(user, COMPANY_NAME, sameDate);
		ReflectionTestUtils.setField(schedule, "uuid", scheduleUuid);

		given(scheduleRepository.findByUuid(scheduleUuid)).willReturn(Optional.of(schedule));
		given(curriculumService.getCurriculumsBySchedule(any(), any(), any())).willReturn(List.of());

		InterviewScheduleUpdateRequest request = new InterviewScheduleUpdateRequest(sameDate);
		InterviewScheduleResponse response = interviewScheduleService.updateSchedule(USER_UUID, scheduleUuid, request);

		assertThat(response.companyName()).isEqualTo(COMPANY_NAME);
		verify(curriculumService, never()).deleteAllBySchedule(any());
		verify(curriculumService, never()).generateCurriculum(any(), any());
	}

	@Test
	@DisplayName("수정 요청에 companyName이 없어도 기존 회사명이 유지된다")
	void updateSchedule_companyNamePreserved () {
		UUID scheduleUuid = UUID.randomUUID();
		User user = User.builder().email("test@example.com").build();
		ReflectionTestUtils.setField(user, "uuid", USER_UUID);

		LocalDate date = LocalDate.of(2026, 5, 20);
		InterviewSchedule schedule = InterviewSchedule.create(user, COMPANY_NAME, date);
		ReflectionTestUtils.setField(schedule, "uuid", scheduleUuid);

		given(scheduleRepository.findByUuid(scheduleUuid)).willReturn(Optional.of(schedule));
		given(curriculumService.getCurriculumsBySchedule(any(), any(), any())).willReturn(List.of());

		InterviewScheduleUpdateRequest request = new InterviewScheduleUpdateRequest(date);
		InterviewScheduleResponse response = interviewScheduleService.updateSchedule(USER_UUID, scheduleUuid, request);

		assertThat(response.companyName()).isEqualTo(COMPANY_NAME);
	}

	@Test
	@DisplayName("다른 사용자의 일정을 수정하려 하면 예외가 발생한다")
	void updateSchedule_fail_unauthorizedUser () {
		UUID scheduleUuid = UUID.randomUUID();
		UUID otherUserUuid = UUID.randomUUID();
		User owner = User.builder().email("owner@example.com").build();
		ReflectionTestUtils.setField(owner, "uuid", USER_UUID);

		InterviewSchedule schedule = InterviewSchedule.create(owner, COMPANY_NAME, LocalDate.of(2026, 5, 20));
		ReflectionTestUtils.setField(schedule, "uuid", scheduleUuid);

		given(scheduleRepository.findByUuid(scheduleUuid)).willReturn(Optional.of(schedule));

		InterviewScheduleUpdateRequest request = new InterviewScheduleUpdateRequest(LocalDate.of(2026, 6, 1));

		assertThatThrownBy(() -> interviewScheduleService.updateSchedule(otherUserUuid, scheduleUuid, request))
			.isInstanceOf(UnauthorizedCurriculumException.class);

		verify(curriculumService, never()).generateCurriculum(any(), any());
	}

}