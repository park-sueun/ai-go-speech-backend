package com.aigo.speech.s3.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.net.URL;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

	@Mock
	private S3Client s3Client;

	@Mock
	private S3Presigner s3Presigner;

	@InjectMocks
	private S3Service s3Service;

	@BeforeEach
	void setUp () {
		ReflectionTestUtils.setField(s3Service, "bucketName", "uug-profile-images");
		ReflectionTestUtils.setField(s3Service, "region", "ap-northeast-2");
	}

	@Nested
	@DisplayName("generatePreSignedUploadUrl")
	class GeneratePreSignedUploadUrl {

		@Test
		@DisplayName("정상 요청 시 PreSignedUrlInfo 반환")
		void success () throws Exception {
			UUID userId = UUID.randomUUID();
			String fileExtension = "jpg";
			String contentType = "image/jpeg";

			PresignedPutObjectRequest mockPresigned = mock(PresignedPutObjectRequest.class);
			given(mockPresigned.url()).willReturn(new URL("https://presigned.example.com/upload"));
			given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
				.willReturn(mockPresigned);

			S3Service.PreSignedUrlInfo result =
				s3Service.generatePreSignedUploadUrl(userId, fileExtension, contentType);

			assertThat(result.preSignedUrl()).isEqualTo("https://presigned.example.com/upload");
			assertThat(result.s3Key()).startsWith("profiles/" + userId + "/");
			assertThat(result.s3Key()).endsWith(".jpg");
			assertThat(result.publicUrl()).startsWith(
				"https://uug-profile-images.s3.ap-northeast-2.amazonaws.com/profiles/" + userId + "/"
			);
		}

		@Test
		@DisplayName("허용되지 않는 ContentType이면 예외 발생")
		void invalidContentType () {
			UUID userId = UUID.randomUUID();

			assertThatThrownBy(() ->
				s3Service.generatePreSignedUploadUrl(userId, "gif", "image/gif")
			)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("허용되지 않는 파일 형식");
		}

		@Test
		@DisplayName("png ContentType 정상 처리")
		void successWithPng () throws Exception {
			UUID userId = UUID.randomUUID();
			PresignedPutObjectRequest mockPresigned = mock(PresignedPutObjectRequest.class);
			given(mockPresigned.url()).willReturn(new URL("https://presigned.example.com/upload"));
			given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
				.willReturn(mockPresigned);

			S3Service.PreSignedUrlInfo result =
				s3Service.generatePreSignedUploadUrl(userId, "png", "image/png");

			assertThat(result.s3Key()).endsWith(".png");
		}
	}

	@Nested
	@DisplayName("deleteObject")
	class DeleteObject {

		@Test
		@DisplayName("정상 키로 삭제 성공")
		void success () {
			String s3Key = "profiles/uuid-123/test.jpg";
			given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
				.willReturn(DeleteObjectResponse.builder().build());

			assertThatCode(() -> s3Service.deleteObject(s3Key))
				.doesNotThrowAnyException();

			then(s3Client).should().deleteObject(any(DeleteObjectRequest.class));
		}

		@Test
		@DisplayName("null 키 전달 시 S3 호출 없이 종료")
		void nullKey () {
			s3Service.deleteObject(null);

			then(s3Client).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("blank 키 전달 시 S3 호출 없이 종료")
		void blankKey () {
			s3Service.deleteObject("   ");

			then(s3Client).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("S3 삭제 실패해도 예외 전파 없이 warn 로그만")
		void deleteFailSilently () {
			String s3Key = "profiles/uuid-123/test.jpg";
			given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
				.willThrow(new RuntimeException("S3 연결 오류"));

			assertThatCode(() -> s3Service.deleteObject(s3Key))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("extractKeyFromUrl")
	class ExtractKeyFromUrl {

		@Test
		@DisplayName("올바른 S3 URL에서 key 추출 성공")
		void success () {
			String url = "https://uug-profile-images.s3.ap-northeast-2.amazonaws.com/profiles/uuid-1/abc.jpg";

			String key = s3Service.extractKeyFromUrl(url);

			assertThat(key).isEqualTo("profiles/uuid-1/abc.jpg");
		}

		@Test
		@DisplayName("다른 버킷 URL이면 null 반환")
		void differentBucket () {
			String url = "https://other-bucket.s3.ap-northeast-2.amazonaws.com/profiles/1/abc.jpg";

			String key = s3Service.extractKeyFromUrl(url);

			assertThat(key).isNull();
		}

		@Test
		@DisplayName("소셜 로그인 이미지 URL(외부)이면 null 반환")
		void externalUrl () {
			String url = "https://k.kakaocdn.net/user/profile.jpg";

			String key = s3Service.extractKeyFromUrl(url);

			assertThat(key).isNull();
		}
	}

	@Nested
	@DisplayName("buildPublicUrl")
	class BuildPublicUrl {

		@Test
		@DisplayName("s3Key로 공개 URL 정상 생성")
		void success () {
			String s3Key = "profiles/uuid-1/abc.jpg";

			String url = s3Service.buildPublicUrl(s3Key);

			assertThat(url).isEqualTo(
				"https://uug-profile-images.s3.ap-northeast-2.amazonaws.com/profiles/uuid-1/abc.jpg"
			);
		}
	}
}