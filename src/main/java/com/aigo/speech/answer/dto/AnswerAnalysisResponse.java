package com.aigo.speech.answer.dto;

import java.util.Map;
import java.util.UUID;

import com.aigo.speech.answer.entity.AnswerAnalysis;
import com.aigo.speech.answer.entity.InterviewAnswer;
import com.aigo.speech.question.entity.InterviewQuestion;

public record AnswerAnalysisResponse(
	UUID uuid,
	int sequenceOrder,
	String questionContent,
	String answerTranscript,
	String aiReview,
	Integer totalScore,
	Integer silenceScore,
	Integer fillerScore,
	Integer logicScore,
	Integer totalSilenceDuration,
	Integer silenceCount,
	Map<String, Integer> fillerWords
) {
	public static AnswerAnalysisResponse from(AnswerAnalysis analysis) {
		InterviewAnswer answer = analysis.getAnswer();
		InterviewQuestion question = answer.getQuestion();
		return new AnswerAnalysisResponse(
			analysis.getUuid(),
			question.getSequenceOrder(),
			question.getContent(),
			answer.getTranscript(),
			analysis.getAiReview(),
			analysis.getTotalScore(),
			analysis.getSilenceScore(),
			analysis.getFillerScore(),
			analysis.getLogicScore(),
			analysis.getTotalSilenceDuration(),
			analysis.getSilenceCount(),
			analysis.getFillerWords()
		);
	}
}
