package com.aigo.speech.curriculum.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.curriculum.dto.CompletedJobPostingResponse;
import com.aigo.speech.curriculum.dto.CurriculumResponse;
import com.aigo.speech.curriculum.dto.InterviewScheduleFromJobPostingRequest;
import com.aigo.speech.curriculum.dto.InterviewScheduleListResponse;
import com.aigo.speech.curriculum.dto.InterviewScheduleResponse;
import com.aigo.speech.curriculum.dto.InterviewScheduleUpdateRequest;
import com.aigo.speech.curriculum.entity.InterviewSchedule;
import com.aigo.speech.curriculum.exception.DuplicateScheduleException;
import com.aigo.speech.curriculum.exception.UnauthorizedCurriculumException;
import com.aigo.speech.curriculum.repository.InterviewScheduleRepository;
import com.aigo.speech.interview.entity.InterviewReport;
import com.aigo.speech.interview.entity.InterviewStatus;
import com.aigo.speech.interview.repository.InterviewReportRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.jobposting.entity.JobPosting;
import com.aigo.speech.jobposting.exception.JobPostingNotFoundException;
import com.aigo.speech.jobposting.repository.JobPostingRepository;
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
	private final InterviewSessionRepository sessionRepository;
	private final InterviewReportRepository reportRepository;
	private final JobPostingRepository jobPostingRepository;

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
		schedule.update(request.interviewDate());

		if (isDateChanged) {
			curriculumService.deleteAllBySchedule(schedule);
			List<CurriculumResponse> curriculums = curriculumService.createPendingCurriculums(
				schedule.getUser(), schedule
			);
			triggerCurriculumGeneration(schedule);
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

	public List<CompletedJobPostingResponse> getCompletedJobPostings (UUID userUuid) {
		User user = userRepository.findByUuid(userUuid)
			.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));
		return sessionRepository.findDistinctJobPostingsByUserAndStatus(user, InterviewStatus.COMPLETED)
			.stream().map(CompletedJobPostingResponse::from).toList();
	}

	@Transactional
	public InterviewScheduleResponse registerFromJobPosting (
		UUID userUuid, InterviewScheduleFromJobPostingRequest request) {
		User user = userRepository.findByUuid(userUuid)
			.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));
		JobPosting jobPosting = jobPostingRepository.findByUuidAndUser(request.jobPostingUuid(), user)
			.orElseThrow(() -> new JobPostingNotFoundException("채용 공고를 찾을 수 없습니다."));
		if (scheduleRepository.existsByJobPosting(jobPosting)) {
			throw new DuplicateScheduleException("이미 해당 채용 공고에 대한 면접 일정이 등록되어 있습니다.");
		}
		InterviewSchedule schedule = scheduleRepository.save(
			InterviewSchedule.createFromJobPosting(user, jobPosting, request.interviewDate())
		);
		List<CurriculumResponse> curriculums = curriculumService.createPendingCurriculums(user, schedule);
		triggerCurriculumGeneration(schedule);
		return InterviewScheduleResponse.of(schedule, curriculums);
	}

	private void triggerCurriculumGeneration (InterviewSchedule schedule) {
		JobPosting jp = schedule.getJobPosting();
		String companyName = jp != null ? orEmpty(jp.getCompanyName()) : orEmpty(schedule.getCompanyName());
		String position = jp != null ? orEmpty(jp.getPosition()) : "";
		String requiredSkills = jp != null ? joinList(jp.getRequiredSkills()) : "";
		String preferredSkills = jp != null ? joinList(jp.getPreferredSkills()) : "";

		ScoreContext scores = jp != null
			? buildScoreContext(schedule.getUser(), jp)
			: new ScoreContext("데이터 없음", "데이터 없음", "데이터 없음", "없음", "없음", "없음");

		curriculumService.generateCurriculumContentAsync(
			schedule.getUuid(),
			companyName, position, requiredSkills, preferredSkills,
			scores.filler(), scores.logic(), scores.silence(),
			scores.fillerTarget(), scores.logicTarget(), scores.silenceTarget()
		);
	}

	private ScoreContext buildScoreContext (User user, JobPosting jobPosting) {
		var latestOpt = sessionRepository
			.findTopByUserAndJobPostingAndStatusOrderByCreatedAtDesc(user, jobPosting, InterviewStatus.COMPLETED);

		if (latestOpt.isEmpty()) {
			return new ScoreContext("데이터 없음", "데이터 없음", "데이터 없음", "없음", "없음", "없음");
		}

		var reportOpt = reportRepository.findBySession(latestOpt.get())
			.filter(r -> r.getStatus() == InterviewReport.ReportStatus.COMPLETED
				&& r.getFillerScore() != null && r.getLogicScore() != null && r.getSilenceScore() != null);

		if (reportOpt.isEmpty()) {
			return new ScoreContext("데이터 없음", "데이터 없음", "데이터 없음", "없음", "없음", "없음");
		}

		InterviewReport r = reportOpt.get();
		return new ScoreContext(
			String.valueOf(r.getFillerScore()),
			String.valueOf(r.getLogicScore()),
			String.valueOf(r.getSilenceScore()),
			toTarget(r.getFillerScore()),
			toTarget(r.getLogicScore()),
			toTarget(r.getSilenceScore())
		);
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

	private String joinList (List<String> list) {
		if (list == null || list.isEmpty())
			return "";
		return String.join(", ", list);
	}

	private String orEmpty (String value) {
		return value != null ? value : "";
	}

	private static String toTarget (int rawScore) {
		if (rawScore == 0)
			return "60";
		return String.valueOf(Math.min(100, (int)Math.round(rawScore * 1.1)));
	}

	private record ScoreContext(String filler, String logic, String silence,
								String fillerTarget, String logicTarget, String silenceTarget) {
	}
}
