package com.aigo.speech.ranking.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import com.aigo.speech.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
	name = "ranking_backup",
	indexes = @Index(name = "idx_ranking_backup_best_score", columnList = "best_score DESC")
)
public class RankingBackup {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "user_uuid", nullable = false, unique = true, columnDefinition = "BINARY(16)")
	private UUID userUuid;

	@Column(name = "best_score", nullable = false)
	private int bestScore;

	@Column(name = "total_session_count", nullable = false)
	private int totalSessionCount;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "job_title")
	private String jobTitle;

	@Builder
	public RankingBackup (User user, UUID userUuid, int bestScore, int totalSessionCount, String jobTitle) {
		this.user = user;
		this.userUuid = userUuid;
		this.bestScore = bestScore;
		this.totalSessionCount = totalSessionCount;
		this.jobTitle = jobTitle;
	}

	public void update (int newScore) {
		if (newScore > this.bestScore) {
			this.bestScore = newScore;
		}
		this.totalSessionCount++;
	}

	public void updateJobTitle (String jobTitle) {
		if (jobTitle != null && !jobTitle.isBlank()) {
			this.jobTitle = jobTitle;
		}
	}
}
