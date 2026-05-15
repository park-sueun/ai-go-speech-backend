package com.aigo.speech.curriculum.dto;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.aigo.speech.curriculum.entity.Curriculum;
import com.aigo.speech.curriculum.entity.InterviewSchedule;
import com.aigo.speech.jobposting.entity.JobPosting;

public class CurriculumResponseTest {

	// ──────────────────────────────────── isPast ────────────────────────────────────

	@Test
	@DisplayName("한국 시간 기준 어제 날짜이면 isPast가 true다")
	void isPast_true_whenYesterday () {
		LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);

		CurriculumResponse response = buildResponse(yesterday, null, null);

		assertThat(response.isPast()).isTrue();
	}

	@Test
	@DisplayName("한국 시간 기준 오늘 날짜이면 isPast가 false다")
	void isPast_false_whenToday () {
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

		CurriculumResponse response = buildResponse(today, null, null);

		assertThat(response.isPast()).isFalse();
	}

	@Test
	@DisplayName("미래 날짜이면 isPast가 false다")
	void isPast_false_whenFuture () {
		LocalDate future = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(5);

		CurriculumResponse response = buildResponse(future, null, null);

		assertThat(response.isPast()).isFalse();
	}

	// ──────────────────────────────────── jobPostingUuid ────────────────────────────────────

	@Test
	@DisplayName("채용공고가 없으면 jobPostingUuid가 null이다")
	void jobPostingUuid_null_whenNoJobPosting () {
		CurriculumResponse response = buildResponse(LocalDate.now(ZoneId.of("Asia/Seoul")), null, null);

		assertThat(response.jobPostingUuid()).isNull();
	}

	@Test
	@DisplayName("채용공고가 있으면 jobPostingUuid가 채용공고의 UUID와 일치한다")
	void jobPostingUuid_set_whenJobPostingExists () {
		UUID jobPostingUuid = UUID.randomUUID();
		JobPosting jobPosting = Mockito.mock(JobPosting.class);
		given(jobPosting.getUuid()).willReturn(jobPostingUuid);

		CurriculumResponse response = buildResponse(LocalDate.now(ZoneId.of("Asia/Seoul")), null, jobPosting);

		assertThat(response.jobPostingUuid()).isEqualTo(jobPostingUuid);
	}

	// ──────────────────────────────────── content ────────────────────────────────────

	@Test
	@DisplayName("AI가 아직 생성하지 않은 경우 content가 null이다")
	void content_null_whenPending () {
		CurriculumResponse response = buildResponse(LocalDate.now(ZoneId.of("Asia/Seoul")), null, null);

		assertThat(response.content()).isNull();
	}

	@Test
	@DisplayName("AI가 생성 완료한 경우 content가 채워진다")
	void content_set_whenAiGenerated () {
		String aiContent = "논리 구조 개선 연습";

		CurriculumResponse response = buildResponse(LocalDate.now(ZoneId.of("Asia/Seoul")), aiContent, null);

		assertThat(response.content()).isEqualTo(aiContent);
	}

	// ──────────────────────────────────── companyName ────────────────────────────────────

	@Test
	@DisplayName("companyName은 InterviewSchedule에서 가져온다")
	void companyName_fromInterviewSchedule () {
		Curriculum curriculum = Mockito.mock(Curriculum.class);
		InterviewSchedule schedule = Mockito.mock(InterviewSchedule.class);

		given(curriculum.getScheduleDate()).willReturn(LocalDate.now(ZoneId.of("Asia/Seoul")));
		given(curriculum.getInterviewSchedule()).willReturn(schedule);
		given(curriculum.getContent()).willReturn(null);
		given(curriculum.getJobPosting()).willReturn(null);
		given(curriculum.getUuid()).willReturn(UUID.randomUUID());
		given(schedule.getCompanyName()).willReturn("음어그");

		CurriculumResponse response = CurriculumResponse.from(curriculum);

		assertThat(response.companyName()).isEqualTo("음어그");
	}

	// ──────────────────────────────────── helper ────────────────────────────────────

	private CurriculumResponse buildResponse (LocalDate scheduleDate, String content, JobPosting jobPosting) {
		Curriculum curriculum = Mockito.mock(Curriculum.class);
		InterviewSchedule schedule = Mockito.mock(InterviewSchedule.class);

		given(curriculum.getScheduleDate()).willReturn(scheduleDate);
		given(curriculum.getInterviewSchedule()).willReturn(schedule);
		given(curriculum.getContent()).willReturn(content);
		given(curriculum.getJobPosting()).willReturn(jobPosting);
		given(curriculum.getUuid()).willReturn(UUID.randomUUID());
		given(schedule.getCompanyName()).willReturn("테스트 회사");

		return CurriculumResponse.from(curriculum);
	}
}
