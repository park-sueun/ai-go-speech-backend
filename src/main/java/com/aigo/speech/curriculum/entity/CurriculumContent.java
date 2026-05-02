package com.aigo.speech.curriculum.entity;

public enum CurriculumContent {

	MOCK_INTERVIEWS_1("1차 모의면접"),
	HABIT_CORRECTION("습관어 집중 교정"),
	MOCK_INTERVIEWS_2("2차 모의면접"),
	LOGIC_RESTRUCTURE("답변 논리 재구조화"),
	MOCK_INTERVIEWS_3("3차 모의면접"),
	SCRIPT_REVIEW("베스트 답변 스크립트 복기"),
	MOCK_INTERVIEW_4("4차 모의면접");

	private final String title;

	CurriculumContent (String title) {
		this.title = title;
	}

	public String getTitle () {
		return title;
	}

	//d-day 수만큼 앞에서 끊어서 사용
	public static final CurriculumContent[] MASTER = CurriculumContent.values();
}
