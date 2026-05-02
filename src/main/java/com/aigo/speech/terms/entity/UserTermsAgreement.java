package com.aigo.speech.terms.entity;

import java.time.LocalDateTime;

import com.aigo.speech.user.entity.User;

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
@Table(name = "USER_TERMS_AGREEMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTermsAgreement {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "terms_id", nullable = false)
	private Terms terms;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "agreed_at", updatable = false)
	private LocalDateTime agreedAt = LocalDateTime.now();

	public UserTermsAgreement (User user, Terms terms) {
		this.user = user;
		this.terms = terms;
	}

}
