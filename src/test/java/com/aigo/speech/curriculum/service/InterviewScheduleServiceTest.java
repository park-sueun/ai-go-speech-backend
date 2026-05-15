package com.aigo.speech.curriculum.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
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

import com.aigo.speech.curriculum.dto.CompletedJobPostingResponse;
import com.aigo.speech.curriculum.dto.CurriculumGenerationRequest;
import com.aigo.speech.curriculum.dto.CurriculumResponse;
import com.aigo.speech.curriculum.dto.InterviewScheduleFromJobPostingRequest;
import com.aigo.speech.curriculum.dto.InterviewScheduleResponse;
import com.aigo.speech.curriculum.dto.InterviewScheduleUpdateRequest;
import com.aigo.speech.curriculum.entity.InterviewSchedule;
import com.aigo.speech.curriculum.exception.DuplicateScheduleException;
import com.aigo.speech.curriculum.exception.UnauthorizedCurriculumException;
import com.aigo.speech.curriculum.repository.InterviewScheduleRepository;
import com.aigo.speech.interview.entity.InterviewStatus;
import com.aigo.speech.interview.repository.InterviewReportRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.jobposting.exception.JobPostingNotFoundException;
import com.aigo.speech.jobposting.repository.JobPostingRepository;
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

	@Mock
	private InterviewSessionRepository sessionRepository;

	@Mock
	private InterviewReportRepository reportRepository;

	@Mock
	private JobPostingRepository jobPostingRepository;

	@InjectMocks
	private InterviewScheduleService interviewScheduleService;

	private final UUID USER_UUID = UUID.randomUUID();
	private final String COMPANY_NAME = "음어그";

	// ──────────────────────────────────── updateSchedule ────────────────────────────────────

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

		List<CurriculumResponse> pending = List.of(
			new CurriculumResponse(UUID.randomUUID(), null, COMPANY_NAME, null, newDate.minusDays(1), false)
		);

		given(scheduleRepository.findByUuid(scheduleUuid)).willReturn(Optional.of(schedule));
		given(curriculumService.createPendingCurriculums(any(User.class), any(InterviewSchedule.class)))
			.willReturn(pending);

		InterviewScheduleUpdateRequest request = new InterviewScheduleUpdateRequest(newDate);
		InterviewScheduleResponse response = interviewScheduleService.updateSchedule(USER_UUID, scheduleUuid, request);

		assertThat(response.companyName()).isEqualTo(COMPANY_NAME);
		assertThat(schedule.getInterviewDate()).isEqualTo(newDate);
		verify(curriculumService).deleteAllBySchedule(schedule);
		verify(curriculumService).createPendingCurriculums(any(User.class), any(InterviewSchedule.class));
		verify(curriculumService).generateCurriculumContentAsync(any(CurriculumGenerationRequest.class));
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
		verify(curriculumService, never()).createPendingCurriculums(any(), any());
		verify(curriculumService, never()).generateCurriculumContentAsync(any(CurriculumGenerationRequest.class));
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

		verify(curriculumService, never()).createPendingCurriculums(any(), any());
	}

	// ──────────────────────────────────── getCompletedJobPostings ────────────────────────────────────

	@Test
	@DisplayName("연습 완료 채용공고 목록을 조회하면 COMPLETED 세션이 있는 고유 채용공고가 반환된다")
	void getCompletedJobPostings_success () {
		User user = User.builder().email("test@example.com").build();
		ReflectionTestUtils.setField(user, "uuid", USER_UUID);

		JobPosting jobPosting = JobPosting.pending(user, "https://jobkorea.co.kr/job");
		ReflectionTestUtils.setField(jobPosting, "uuid", UUID.randomUUID());
		ReflectionTestUtils.setField(jobPosting, "companyName", COMPANY_NAME);
		ReflectionTestUtils.setField(jobPosting, "position", "백엔드 개발자");

		given(userRepository.findByUuid(USER_UUID)).willReturn(Optional.of(user));
		given(sessionRepository.findDistinctJobPostingsByUserAndStatus(user, InterviewStatus.COMPLETED))
			.willReturn(List.of(jobPosting));

		List<CompletedJobPostingResponse> result = interviewScheduleService.getCompletedJobPostings(USER_UUID);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).companyName()).isEqualTo(COMPANY_NAME);
		assertThat(result.get(0).position()).isEqualTo("백엔드 개발자");
		assertThat(result.get(0).site()).isEqualTo("잡코리아");
	}

	@Test
	@DisplayName("완료된 세션이 없으면 빈 리스트가 반환된다")
	void getCompletedJobPostings_empty () {
		User user = User.builder().email("test@example.com").build();
		ReflectionTestUtils.setField(user, "uuid", USER_UUID);

		given(userRepository.findByUuid(USER_UUID)).willReturn(Optional.of(user));
		given(sessionRepository.findDistinctJobPostingsByUserAndStatus(user, InterviewStatus.COMPLETED))
			.willReturn(List.of());

		List<CompletedJobPostingResponse> result = interviewScheduleService.getCompletedJobPostings(USER_UUID);

		assertThat(result).isEmpty();
	}

	// ──────────────────────────────────── registerFromJobPosting ────────────────────────────────────

	@Test
	@DisplayName("채용공고 UUID로 면접 일정을 등록하면 pending 커리큘럼이 생성되고 AI 생성이 트리거된다")
	void registerFromJobPosting_success () {
		User user = User.builder().email("test@example.com").build();
		ReflectionTestUtils.setField(user, "uuid", USER_UUID);

		UUID jobPostingUuid = UUID.randomUUID();
		JobPosting jobPosting = JobPosting.pending(user, "https://wanted.co.kr/job");
		ReflectionTestUtils.setField(jobPosting, "uuid", jobPostingUuid);
		ReflectionTestUtils.setField(jobPosting, "companyName", COMPANY_NAME);

		LocalDate interviewDate = LocalDate.of(2026, 6, 20);
		InterviewSchedule savedSchedule = InterviewSchedule.createFromJobPosting(user, jobPosting, interviewDate);
		ReflectionTestUtils.setField(savedSchedule, "id", 1L);
		ReflectionTestUtils.setField(savedSchedule, "uuid", UUID.randomUUID());

		List<CurriculumResponse> pending = List.of(
			new CurriculumResponse(UUID.randomUUID(), null, COMPANY_NAME, null, interviewDate.minusDays(1), false)
		);

		given(userRepository.findByUuid(USER_UUID)).willReturn(Optional.of(user));
		given(jobPostingRepository.findByUuidAndUser(jobPostingUuid, user)).willReturn(Optional.of(jobPosting));
		given(scheduleRepository.existsByJobPosting(jobPosting)).willReturn(false);
		given(scheduleRepository.save(any(InterviewSchedule.class))).willReturn(savedSchedule);
		given(sessionRepository.findTopByUserAndJobPostingAndStatusOrderByCreatedAtDesc(user, jobPosting, InterviewStatus.COMPLETED))
			.willReturn(java.util.Optional.empty());
		given(curriculumService.createPendingCurriculums(any(User.class), any(InterviewSchedule.class)))
			.willReturn(pending);

		InterviewScheduleFromJobPostingRequest request =
			new InterviewScheduleFromJobPostingRequest(jobPostingUuid, interviewDate);
		InterviewScheduleResponse response = interviewScheduleService.registerFromJobPosting(USER_UUID, request);

		assertThat(response.companyName()).isEqualTo(COMPANY_NAME);
		assertThat(response.curriculums()).hasSize(1);
		assertThat(response.curriculums().get(0).content()).isNull();
		verify(scheduleRepository).save(any(InterviewSchedule.class));
		verify(curriculumService).createPendingCurriculums(any(User.class), any(InterviewSchedule.class));
		verify(curriculumService).generateCurriculumContentAsync(any(CurriculumGenerationRequest.class));
	}

	@Test
	@DisplayName("같은 채용공고에 이미 면접 일정이 있으면 DuplicateScheduleException이 발생한다")
	void registerFromJobPosting_duplicateThrows () {
		User user = User.builder().email("test@example.com").build();
		ReflectionTestUtils.setField(user, "uuid", USER_UUID);

		UUID jobPostingUuid = UUID.randomUUID();
		JobPosting jobPosting = JobPosting.pending(user, "https://example.com/job");
		ReflectionTestUtils.setField(jobPosting, "uuid", jobPostingUuid);

		given(userRepository.findByUuid(USER_UUID)).willReturn(Optional.of(user));
		given(jobPostingRepository.findByUuidAndUser(jobPostingUuid, user)).willReturn(Optional.of(jobPosting));
		given(scheduleRepository.existsByJobPosting(jobPosting)).willReturn(true);

		InterviewScheduleFromJobPostingRequest request =
			new InterviewScheduleFromJobPostingRequest(jobPostingUuid, LocalDate.of(2026, 6, 20));

		assertThatThrownBy(() -> interviewScheduleService.registerFromJobPosting(USER_UUID, request))
			.isInstanceOf(DuplicateScheduleException.class)
			.hasMessage("이미 해당 채용 공고에 대한 면접 일정이 등록되어 있습니다.");

		verify(scheduleRepository, never()).save(any());
	}

	@Test
	@DisplayName("존재하지 않는 채용공고 UUID로 등록하면 JobPostingNotFoundException이 발생한다")
	void registerFromJobPosting_jobPostingNotFound () {
		User user = User.builder().email("test@example.com").build();
		ReflectionTestUtils.setField(user, "uuid", USER_UUID);

		UUID unknownUuid = UUID.randomUUID();

		given(userRepository.findByUuid(USER_UUID)).willReturn(Optional.of(user));
		given(jobPostingRepository.findByUuidAndUser(unknownUuid, user)).willReturn(Optional.empty());

		InterviewScheduleFromJobPostingRequest request =
			new InterviewScheduleFromJobPostingRequest(unknownUuid, LocalDate.of(2026, 6, 20));

		assertThatThrownBy(() -> interviewScheduleService.registerFromJobPosting(USER_UUID, request))
			.isInstanceOf(JobPostingNotFoundException.class);

		verify(scheduleRepository, never()).save(any());
	}
}
