package com.aigo.speech.s3.service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;

	@Value("${cloud.aws.s3.bucket}")
	private String bucketName;

	@Value("${cloud.aws.region.static}")
	private String region;

	private static final Set<String> ALLOWED_CONTENT_TYPES =
		Set.of("image/jpeg", "image/png", "image/webp");

	public PreSignedUrlInfo generatePreSignedUploadUrl (
		UUID userId,
		String fileExtension,
		String contentType
	) {
		if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new IllegalArgumentException("혀용되지 않는 파일 형식: " + contentType);
		}

		String s3Key = String.format(
			"profiles/%d/%s.%s",
			userId,
			UUID.randomUUID(),
			fileExtension
		);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(s3Key)
			.contentType(contentType)
			.build();

		PutObjectPresignRequest preSignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(5)) // 5분 유효
			.putObjectRequest(putObjectRequest)
			.build();

		PresignedPutObjectRequest preSignedRequest
			= s3Presigner.presignPutObject(preSignRequest);

		String preSignedUrl = preSignedRequest.url().toString();

		/* 업로드 완료 후 접근 URL */
		String publicUrl = String.format(
			"https://%s.s3.%s.amazonaws.com/%s",
			bucketName, region, s3Key
		);

		log.info("Presigned URL 발급 완료 - userId: {}, s3Key: {}", userId, s3Key);

		return new PreSignedUrlInfo(preSignedUrl, s3Key, publicUrl);
	}

	/* 기존 프로필 이미지 교체 시 사용 */
	public void deleteObject (String s3Key) {
		try {
			DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
				.bucket(bucketName)
				.key(s3Key)
				.build();
			s3Client.deleteObject(deleteRequest);
			log.info("S3 오브젝트 삭제 완료 - key: {}", s3Key);
		} catch (Exception e) {
			/* 삭제 실패해도 프로필 업데이트는 계속 진행 */
			log.warn("S3 오브젝트 삭제 실패 - key: {}, error: {}", s3Key, e.getMessage());
		}
	}

	/* s3 url에서 key 추출*/
	public String extractKeyFromUrl (String url) {
		try {
			/* url에서 도메인 이후 경로만 추출 */
			String prefix = String.format(
				"https://%s.s3.%s.amazonaws.com/", bucketName, region
			);
			if (url.startsWith(prefix)) {
				return url.substring(prefix.length());
			}
		} catch (Exception e) {
			log.warn("S3 key 추출 실패 - url: {}", url);
		}
		return null;
	}

	public String buildPublicUrl (String s3Key) {
		return String.format(
			"https://%s.s3.%s.amazonaws.com/%s",
			bucketName, region, s3Key
		);
	}

	public record PreSignedUrlInfo(
		String preSignedUrl,
		String s3Key,
		String publicUrl
	) {
	}
}
