package com.aigo.speech.curriculum.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.curriculum.dto.CurriculumResponse;
import com.aigo.speech.curriculum.entity.Curriculum;
import com.aigo.speech.curriculum.entity.CurriculumContent;
import com.aigo.speech.curriculum.entity.InterviewSchedule;
import com.aigo.speech.curriculum.exception.UnauthorizedCurriculumException;
import com.aigo.speech.curriculum.repository.CurriculumRepository;
import com.aigo.speech.curriculum.repository.InterviewScheduleRepository;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.jobposting.exception.JobPostingNotFoundException;
import com.aigo.speech.jobposting.repository.JobPostingRepository;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumService {
	private final CurriculumRepository curriculumRepository;
	private final JobPostingRepository jobPostingRepository;
	private final UserRepository userRepository;
	private final InterviewScheduleRepository interviewScheduleRepository;

	/* 홈 화면에서 면접 일정 등록 시 호출 */
	@Transactional
	public List<CurriculumResponse> generateCurriculum (User user, InterviewSchedule interviewSchedule) {
		LocalDate interviewDate = interviewSchedule.getInterviewDate();
		LocalDate today = LocalDate.now();

		long d_day = ChronoUnit.DAYS.between(today, interviewDate);
		if (d_day <= 0) {
			throw new IllegalStateException("면접 날짜가 오늘 이전이라 커리큘럼을 생성할 수 없습니다.");
		}

		/* 최대 7개의 커리큘럼 항목 생성 */
		int n = (int)Math.min(d_day, 7);
		CurriculumContent[] master = CurriculumContent.MASTER;
		List<Curriculum> curriculum = new ArrayList<>();

		for (int i = 1; i <= n; i++) {
			LocalDate scheduleDate = interviewDate.minusDays(n - i);
			curriculum.add(Curriculum.create(user, interviewSchedule, master[i - 1], scheduleDate));
		}

		return curriculumRepository.saveAll(curriculum)
			.stream().map(CurriculumResponse::from).toList();
	}

	public List<CurriculumResponse> getCurriculumsBySchedule (UUID userUuid, UUID scheduleUuid, LocalDate date) {
		InterviewSchedule schedule = interviewScheduleRepository.findByUuid(scheduleUuid)
			.orElseThrow(() -> new IllegalArgumentException("해당 일정을 찾을 수 없습니다."));

		if (!schedule.getUser().getUuid().equals(userUuid)) {
			throw new UnauthorizedCurriculumException("해당 일정을 조회할 권한이 없습니다.");
		}
		List<Curriculum> curriculums = curriculumRepository
			.findByInterviewScheduleOrderByScheduleDateAsc(schedule);

		if (date != null) {
			curriculums = curriculums.stream()
				.filter(c -> c.getScheduleDate().equals(date))
				.toList();
		}

		return curriculums.stream()
			.map(CurriculumResponse::from)
			.toList();
	}

	public List<CurriculumResponse> getCurriculumsByDate (UUID userUuid, LocalDate date) {
		User user = userRepository.findByUuid(userUuid)
			.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사람입니다."));

		return curriculumRepository.findByUserUuidAndScheduleDate(userUuid, date)
			.stream().map(CurriculumResponse::from).toList();
	}

	@Transactional
	public void deleteAllBySchedule (InterviewSchedule schedule) {
		curriculumRepository.deleteAllByInterviewSchedule(schedule);
	}

	private User findUser (String userUuid) {
		return userRepository.findByUuid(UUID.fromString(userUuid))
			.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사람입니다."));
	}

	private JobPosting findJobPosting (UUID jobPostingUuid) {
		return jobPostingRepository.findByUuid(jobPostingUuid)
			.orElseThrow(() -> new JobPostingNotFoundException("채용 공고를 찾을 수 없습니다."));
	}
}
