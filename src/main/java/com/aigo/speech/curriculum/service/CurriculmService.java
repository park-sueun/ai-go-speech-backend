package com.aigo.speech.curriculum.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.curriculum.dto.CurriculmResponse;
import com.aigo.speech.curriculum.entity.CurriculmContent;
import com.aigo.speech.curriculum.entity.Curriculum;
import com.aigo.speech.curriculum.exception.CurriculumAlreadyExistsException;
import com.aigo.speech.curriculum.repository.CurriculmRepository;
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
public class CurriculmService {
	private final CurriculmRepository curriculmRepository;
	private final JobPostingRepository jobPostingRepository;
	private final UserRepository userRepository;

	// 면접 일정 등록 시 호출
	@Transactional
	public List<CurriculmResponse> generateCurriculum (String userUuid, UUID jobPostingUuid) {
		User user = findUser(userUuid);
		JobPosting jobPosting = findJobPosting(jobPostingUuid);

		if (curriculmRepository.existsByJobPosting(jobPosting)) {
			throw new CurriculumAlreadyExistsException("이미 커리큘럼이 생성된 채용 공고입니다.");
		}
		LocalDate interviewDate = jobPosting.getInterviewDate();
		if (interviewDate == null) {
			throw new IllegalStateException("면접 날짜가 설정되지 않은 공고입니다.");
		}

		LocalDate today = LocalDate.now();
		long D_day = java.time.temporal.ChronoUnit.DAYS.between(today, interviewDate);
		if (D_day <= 0) {
			throw new IllegalStateException("면접 날짜가 오늘 이전이라 커리큘럼을 생성할 수 없습니다.");
		}

		// 생성할 커리큘럼 수
		int n = (int)Math.min(D_day, 7);
		CurriculmContent[] master = CurriculmContent.MASTER;
		List<Curriculum> curriculm = new ArrayList<>();

		// 총 7개의 항목에서 앞에서부터 n개씩 끊어서 보여줌
		for (int i = 1; i <= n; i++) {
			CurriculmContent content = master[i - 1];
			LocalDate scheduleDate = interviewDate.minusDays(n - i);
			curriculm.add(Curriculum.create(user, jobPosting, content, scheduleDate));
		}

		List<Curriculum> saved = curriculmRepository.saveAll(curriculm);
		log.info("[Curriculum] Generated {} items for jopPosting uuid = {}", saved.size(), jobPostingUuid);

		return saved.stream().map(CurriculmResponse::from).toList();
	}

	public List<CurriculmResponse> getCurriculumsByJobPosting (String userUuid, UUID jobPostingUuid) {
		findUser(userUuid);
		JobPosting jobPosting = findJobPosting(jobPostingUuid);

		return curriculmRepository.findByJobPostingOrderByScheduleDateAsc(jobPosting)
			.stream().map(CurriculmResponse::from).toList();
	}

	public List<CurriculmResponse> getTodayCurriculums (String userUuid) {
		User user = findUser(userUuid);
		LocalDate today = LocalDate.now();

		return curriculmRepository.findByUserIdAndScheduleDate(user.getId(), today)
			.stream().map(CurriculmResponse::from).toList();
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
