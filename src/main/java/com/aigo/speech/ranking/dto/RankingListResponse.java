package com.aigo.speech.ranking.dto;

import java.util.List;

public record RankingListResponse(
	MyRankingResponse myRankingResponse,
	List<RankingItemResponse> rankingItemResponseList,
	long totalCount /* 전체 인원 수 */
) {
	public record RankingItemResponse(
		long rank,
		String nickname,
		String profileImageUrl,
		String jobTitle,
		double bestScore,
		int totalSessionCount
	) {
	}

	public record MyRankingResponse(
		long rank,
		String nickname,
		String profileImageUrl,
		String jobTitle,
		int bestScore,
		int totalSessionCount
	) {
	}
}
