package com.aigo.speech.question.entity;

import java.util.UUID;

import com.aigo.speech.global.entity.BaseTimeEntity;
import com.aigo.speech.interview.entity.InterviewSession;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interview_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewQuestion extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false, updatable = false)
	private UUID uuid;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id", nullable = false)
	private InterviewSession session;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "sequence_order", nullable = false)
	private int sequenceOrder;

	public InterviewQuestion (InterviewSession session, String content, int sequenceOrder) {
		this.uuid = UUID.randomUUID();
		this.session = session;
		this.content = content;
		this.sequenceOrder = sequenceOrder;
	}
}