package com.aigo.speech.curriculum.dto;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.aigo.speech.curriculum.entity.Curriculum;
import com.aigo.speech.curriculum.entity.CurriculumContent;
import com.aigo.speech.curriculum.entity.InterviewSchedule;

public class CurriculumResponseTest {
	@Test
	@DisplayName("한국 시간 기준으로 과거 날짜인 경우 isPast가 true여야 한다")
	void isPast_True_WhenDateIsBeforeKST () {
		LocalDate todayKST = LocalDate.now(ZoneId.of("Asia/Seoul"));
		LocalDate yesterdayKST = todayKST.minusDays(1);

		Curriculum curriculum = Mockito.mock(Curriculum.class);
		InterviewSchedule schedule = Mockito.mock(InterviewSchedule.class);

		given(curriculum.getScheduleDate()).willReturn(yesterdayKST);
		given(curriculum.getInterviewSchedule()).willReturn(schedule);
		given(curriculum.getContent()).willReturn(CurriculumContent.MASTER[0]);
		given(schedule.getCompanyName()).willReturn("Test Company");

		CurriculumResponse response = CurriculumResponse.from(curriculum);

		assertThat(response.isPast()).isTrue();
	}

	@Test
	@DisplayName("오늘 날짜이거나 미래인 경우 isPast는 false여야 한다")
	void isPast_False_WhenDateIsTodayOrFuture () {
		LocalDate todayKST = LocalDate.now(ZoneId.of("Asia/Seoul"));

		Curriculum curriculum = Mockito.mock(Curriculum.class);
		InterviewSchedule schedule = Mockito.mock(InterviewSchedule.class);

		given(curriculum.getScheduleDate()).willReturn(todayKST);
		given(curriculum.getInterviewSchedule()).willReturn(schedule);
		given(curriculum.getContent()).willReturn(CurriculumContent.MASTER[0]);
		given(schedule.getCompanyName()).willReturn("Test Company");

		CurriculumResponse response = CurriculumResponse.from(curriculum);

		assertThat(response.isPast()).isFalse();
	}
}
