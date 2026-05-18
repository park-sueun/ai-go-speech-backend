package com.aigo.speech.user.entity;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.aigo.speech.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Profile extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(unique = true, nullable = false)
	private String nickname;

	@Column(name = "profile_image_url")
	private String profileImageUrl;

	@Builder
	public Profile (User user, String nickname, String profileImageUrl) {
		this.user = user;
		this.nickname = nickname;
		this.profileImageUrl = blankToNull(profileImageUrl);
	}

	public void update (String nickname, String profileImageUrl) {
		this.nickname = nickname;
		this.profileImageUrl = profileImageUrl;
	}

	public void update (String nickname) {
		this.nickname = nickname;
	}

	public void updateProfileImage (String profileImageUrl) {
		this.profileImageUrl = blankToNull(profileImageUrl);
	}

	private static String blankToNull (String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

}
