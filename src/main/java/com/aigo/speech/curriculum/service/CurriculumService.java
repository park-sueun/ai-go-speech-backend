package com.aigo.speech.curriculum.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.ai.dto.AiPromptRequest;
import com.aigo.speech.ai.prompt.PromptTemplate;
import com.aigo.speech.ai.service.AiService;
import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.curriculum.dto.CurriculumGenerationRequest;
import com.aigo.speech.curriculum.dto.CurriculumResponse;
import com.aigo.speech.curriculum.entity.Curriculum;
import com.aigo.speech.curriculum.entity.InterviewSchedule;
import com.aigo.speech.curriculum.exception.UnauthorizedCurriculumException;
import com.aigo.speech.curriculum.repository.CurriculumRepository;
import com.aigo.speech.curriculum.repository.InterviewScheduleRepository;
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
	private final InterviewScheduleRepository interviewScheduleRepository;
	private final UserRepository userRepository;
	private final AiService aiService;

	@Transactional
	public List<CurriculumResponse> createPendingCurriculums (User user, InterviewSchedule schedule) {
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
		long dDay = ChronoUnit.DAYS.between(today, schedule.getInterviewDate());
		if (dDay <= 0) {
			throw new IllegalStateException("면접 날짜가 오늘 이전이라 커리큘럼을 생성할 수 없습니다.");
		}

		int n = (int)dDay;
		List<Curriculum> items = new ArrayList<>();
		for (int i = 1; i <= n; i++) {
			LocalDate scheduleDate = schedule.getInterviewDate().minusDays(n - i + 1);
			items.add(Curriculum.createPending(user, schedule, scheduleDate));
		}
		return curriculumRepository.saveAll(items).stream().map(CurriculumResponse::from).toList();
	}

	@Async("aiExecutor")
	@Transactional
	public void generateCurriculumContentAsync (CurriculumGenerationRequest req) {
		try {
			InterviewSchedule schedule = interviewScheduleRepository.findByUuid(req.scheduleUuid())
				.orElseThrow(() -> new IllegalArgumentException("해당 일정을 찾을 수 없습니다."));
			List<Curriculum> curriculums = curriculumRepository
				.findByInterviewScheduleOrderByScheduleDateAsc(schedule);

			if (curriculums.isEmpty()) return;

			AiPromptRequest aiRequest = AiPromptRequest.of(
				PromptTemplate.CURRICULUM_V1.getContent(),
				Map.ofEntries(
					Map.entry("companyName", req.companyName()),
					Map.entry("position", req.position()),
					Map.entry("requiredSkills", req.requiredSkills()),
					Map.entry("preferredSkills", req.preferredSkills()),
					Map.entry("fillerScore", req.fillerScore()),
					Map.entry("logicScore", req.logicScore()),
					Map.entry("silenceScore", req.silenceScore()),
					Map.entry("fillerTarget", req.fillerTarget()),
					Map.entry("logicTarget", req.logicTarget()),
					Map.entry("silenceTarget", req.silenceTarget()),
					Map.entry("n", String.valueOf(curriculums.size()))
				)
			);

			String rawContent = aiService.complete(aiRequest).content();
			if (rawContent == null || rawContent.isBlank()) {
				log.warn("[Curriculum] AI 응답이 비어있습니다. scheduleUuid={}", req.scheduleUuid());
				return;
			}

			String[] lines = rawContent.strip().split("\n");

			for (int i = 0; i < Math.min(curriculums.size(), lines.length); i++) {
				String line = lines[i].strip().replaceAll("^\\d+[.)\\s]+", "");
				if (!line.isBlank()) {
					curriculums.get(i).updateContent(line);
				}
			}

			log.info("[Curriculum] AI 커리큘럼 생성 완료. scheduleUuid={}, count={}", req.scheduleUuid(), curriculums.size());
		} catch (Exception e) {
			log.error("[Curriculum] AI 커리큘럼 생성 실패. scheduleUuid={}, error={}", req.scheduleUuid(), e.getMessage());
		}
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

		return curriculums.stream().map(CurriculumResponse::from).toList();
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
}
