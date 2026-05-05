package com.aigo.speech.interview.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aigo.speech.answer.entity.AnswerAnalysis;
import com.aigo.speech.answer.repository.AnswerAnalysisRepository;
import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.interview.dto.InterviewReportDetailResponse;
import com.aigo.speech.interview.dto.InterviewReportSummaryResponse;
import com.aigo.speech.interview.entity.InterviewReport;
import com.aigo.speech.interview.entity.InterviewSession;
import com.aigo.speech.interview.exception.InterviewReportPendingException;
import com.aigo.speech.interview.repository.InterviewReportRepository;
import com.aigo.speech.interview.repository.InterviewSessionRepository;
import com.aigo.speech.question.repository.InterviewQuestionRepository;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewReportService {

	private final UserRepository userRepository;
	private final InterviewSessionRepository sessionRepository;
	private final InterviewReportRepository reportRepository;
	private final AnswerAnalysisRepository analysisRepository;
	private final InterviewQuestionRepository questionRepository;
	private final InterviewSessionService sessionService;

	public List<InterviewReportSummaryResponse> getReportList(String userUuidStr) {
		User user = userRepository.findByUuid(UUID.fromString(userUuidStr))
			.orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

		List<InterviewSession> sessions = sessionRepository.findByUserOrderByCreatedAtDesc(user);

		Map<Long, InterviewReport> reportBySessionId = reportRepository.findBySessionIn(sessions)
			.stream()
			.collect(Collectors.toMap(r -> r.getSession().getId(), r -> r));

		return sessions.stream()
			.map(session -> {
				InterviewReport report = reportBySessionId.get(session.getId());
				long questionCount = questionRepository.countBySession(session);
				return InterviewReportSummaryResponse.from(session, report, questionCount);
			})
			.toList();
	}

	public InterviewReportDetailResponse getReportDetail(UUID sessionUuid, String userUuidStr) {
		InterviewSession session = sessionService.findByUuid(sessionUuid);
		sessionService.validateOwnership(session, userUuidStr);

		InterviewReport report = reportRepository.findBySession(session)
			.orElseThrow(() -> new InterviewReportPendingException("아직 분석이 진행 중입니다."));

		List<AnswerAnalysis> analyses = analysisRepository.findBySessionWithDetails(session);

		return InterviewReportDetailResponse.from(session, report, analyses);
	}
}
