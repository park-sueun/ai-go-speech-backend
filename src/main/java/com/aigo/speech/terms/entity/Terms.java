package com.aigo.speech.terms.entity;

import java.util.UUID;

import com.aigo.speech.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Terms extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private UUID uuid = UUID.randomUUID();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TermsType type;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	@Column(nullable = false, length = 20)
	private String version;

	@Column(nullable = false)
	private Boolean required = true;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive = true;

	@Builder
	public Terms (
		Long id, TermsType type, String title, String content, String version, Boolean required, Boolean isActive) {
		this.id = id;
		this.type = type;
		this.title = title;
		this.content = content;
		this.version = version;
		this.required = required;
		this.isActive = isActive;
	}

	public void update (String title, String content, String version, Boolean required, Boolean isActive) {
		this.title = title;
		this.content = content;
		this.version = version;
		this.required = required;
		this.isActive = isActive;
	}
}
