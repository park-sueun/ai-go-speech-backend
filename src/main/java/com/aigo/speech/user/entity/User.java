package com.aigo.speech.user.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private Provider provider;

	@Column(name = "provider_id")
	private String providerId;

	@Column(unique = true, nullable = false)
	private String email;

	private String password;

	private String username;   // 로컬 회원가입 시 사용자가 입력한 이름

	private String nickname;   // OAuth 로그인 시 제공자에서 가져온 이름

	@Column(name = "profile_image")
	private String profileImage;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Role role;

	@Column(length = 512)
	private String refreshToken;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Builder
	public User (
		Provider provider, String providerId, String email, String password,
		String username, String nickname, String profileImage, Role role
	) {
		this.provider = provider;
		this.providerId = providerId;
		this.email = email;
		this.password = password;
		this.username = username;
		this.nickname = nickname;
		this.profileImage = profileImage;
		this.role = role;
	}

	public void update (String nickname, String profileImage) {
		this.nickname = nickname;
		this.profileImage = profileImage;
	}

	public void updateRefreshToken (String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public void changePassword (String encodedPassword) {
		this.password = encodedPassword;
	}
}
