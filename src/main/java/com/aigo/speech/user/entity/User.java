package com.aigo.speech.user.entity;

import jakarta.persistence.CascadeType;
import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.aigo.speech.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "users",
	uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false, updatable = false)
	private UUID uuid;

	@Column(unique = true, nullable = false)
	private String email;

	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private Provider provider;

	@Column(name = "provider_id")
	private String providerId;

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private Profile profile;

	@Column(length = 512)
	private String refreshToken;

	@PrePersist
	protected void prePersist () {
		if (this.uuid == null) {
			this.uuid = UUID.randomUUID();
		}
	}

	@Builder
	public User (
		Provider provider, String providerId, String email, String password, Role role
	) {
		this.provider = provider;
		this.providerId = providerId;
		this.email = email;
		this.password = password;
		this.role = role;
	}

	public void updateRefreshToken (String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public void changePassword (String encodedPassword) {
		this.password = encodedPassword;
	}

	public void changeEmail(String email) { this.email = email;	}
}
