package com.aigo.speech.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aigo.speech.auth.exception.UserNotFoundException;
import com.aigo.speech.s3.dto.PreSignedUrlRequest;
import com.aigo.speech.s3.dto.PreSignedUrlResponse;
import com.aigo.speech.s3.service.S3Service;
import com.aigo.speech.user.entity.Profile;
import com.aigo.speech.user.entity.User;
import com.aigo.speech.user.repository.ProfileRepository;
import com.aigo.speech.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private ProfileRepository profileRepository;
	@Mock
	private S3Service s3Service;

	@InjectMocks
	private UserService userService;

	@Nested
	@DisplayName("getPreSignedUploadUrl")
	class GetPreSignedUploadUrl {

		@Test
		@DisplayName("정상 요청 시 PreSignedUrlResponse 반환")
		void success () {
			UUID uuid = UUID.randomUUID();

			User mockUser = mock(User.class);
			given(mockUser.getUuid()).willReturn(uuid);
			given(userRepository.findByUuid(uuid)).willReturn(Optional.of(mockUser));

			PreSignedUrlRequest request = mock(PreSignedUrlRequest.class);
			given(request.getFileExtension()).willReturn("jpg");
			given(request.getContentType()).willReturn("image/jpeg");

			S3Service.PreSignedUrlInfo info = new S3Service.PreSignedUrlInfo(
				"https://presigned.url",
				"profiles/" + uuid + "/abc.jpg",
				"https://bucket.s3.region.amazonaws.com/profiles/" + uuid + "/abc.jpg"
			);
			given(s3Service.generatePreSignedUploadUrl(uuid, "jpg", "image/jpeg"))
				.willReturn(info);

			PreSignedUrlResponse response = userService.getPreSignedUploadUrl(uuid, request);

			assertThat(response.getPreSignedUrl()).isEqualTo("https://presigned.url");
			assertThat(response.getS3Key()).isEqualTo("profiles/" + uuid + "/abc.jpg");
		}

		@Test
		@DisplayName("존재하지 않는 UUID면 UserNotFoundException")
		void userNotFound () {
			UUID uuid = UUID.randomUUID();
			PreSignedUrlRequest request = mock(PreSignedUrlRequest.class);
			given(userRepository.findByUuid(uuid)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.getPreSignedUploadUrl(uuid, request))
				.isInstanceOf(UserNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("confirmProfileImage")
	class ConfirmProfileImage {

		@Test
		@DisplayName("기존 이미지 없을 때 새 이미지 저장")
		void successWithNoExistingImage () {
			UUID uuid = UUID.randomUUID();
			String s3Key = "profiles/" + uuid + "/new.jpg";
			String newUrl = "https://bucket.s3.region.amazonaws.com/" + s3Key;

			Profile mockProfile = mock(Profile.class);
			given(mockProfile.getProfileImageUrl()).willReturn(null);

			User mockUser = mock(User.class);
			given(mockUser.getProfile()).willReturn(mockProfile);
			given(userRepository.findByUuid(uuid)).willReturn(Optional.of(mockUser));
			given(s3Service.buildPublicUrl(s3Key)).willReturn(newUrl);

			userService.confirmProfileImage(uuid, s3Key);

			then(s3Service).should(never()).deleteObject(any());
			then(mockProfile).should().updateProfileImage(newUrl);
		}

		@Test
		@DisplayName("기존 S3 이미지 있을 때 기존 이미지 삭제 후 새 이미지 저장")
		void successWithExistingS3Image () {
			UUID uuid = UUID.randomUUID();
			String oldUrl = "https://bucket.s3.region.amazonaws.com/profiles/" + uuid + "/old.jpg";
			String oldKey = "profiles/" + uuid + "/old.jpg";
			String s3Key = "profiles/" + uuid + "/new.jpg";
			String newUrl = "https://bucket.s3.region.amazonaws.com/" + s3Key;

			Profile mockProfile = mock(Profile.class);
			given(mockProfile.getProfileImageUrl()).willReturn(oldUrl);

			User mockUser = mock(User.class);
			given(mockUser.getProfile()).willReturn(mockProfile);
			given(userRepository.findByUuid(uuid)).willReturn(Optional.of(mockUser));
			given(s3Service.buildPublicUrl(s3Key)).willReturn(newUrl);
			given(s3Service.extractKeyFromUrl(oldUrl)).willReturn(oldKey);

			userService.confirmProfileImage(uuid, s3Key);

			then(s3Service).should().deleteObject(oldKey);
			then(mockProfile).should().updateProfileImage(newUrl);
		}

		@Test
		@DisplayName("소셜 로그인 이미지(외부 URL)는 S3 삭제 호출 안 함")
		void existingImageIsExternalUrl () {
			UUID uuid = UUID.randomUUID();
			String oldUrl = "https://k.kakaocdn.net/profile.jpg";
			String s3Key = "profiles/" + uuid + "/new.jpg";
			String newUrl = "https://bucket.s3.region.amazonaws.com/" + s3Key;

			Profile mockProfile = mock(Profile.class);
			given(mockProfile.getProfileImageUrl()).willReturn(oldUrl);

			User mockUser = mock(User.class);
			given(mockUser.getProfile()).willReturn(mockProfile);
			given(userRepository.findByUuid(uuid)).willReturn(Optional.of(mockUser));
			given(s3Service.buildPublicUrl(s3Key)).willReturn(newUrl);
			given(s3Service.extractKeyFromUrl(oldUrl)).willReturn(null);

			userService.confirmProfileImage(uuid, s3Key);

			then(s3Service).should(never()).deleteObject(any());
		}

		@Test
		@DisplayName("잘못된 s3Key prefix면 IllegalArgumentException")
		void invalidS3KeyPrefix () {
			UUID uuid = UUID.randomUUID();
			String invalidKey = "profiles/other-uuid/hacked.jpg";

			assertThatThrownBy(() -> userService.confirmProfileImage(uuid, invalidKey))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("잘못된 s3Key 경로");
		}

		@Test
		@DisplayName("존재하지 않는 UUID면 UserNotFoundException")
		void userNotFound () {
			UUID uuid = UUID.randomUUID();
			String s3Key = "profiles/" + uuid + "/new.jpg";
			given(userRepository.findByUuid(uuid)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.confirmProfileImage(uuid, s3Key))
				.isInstanceOf(UserNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("deleteProfileImage")
	class DeleteProfileImage {

		@Test
		@DisplayName("S3 이미지 있을 때 삭제 후 URL null 처리")
		void successWithS3Image () {
			UUID uuid = UUID.randomUUID();
			String oldUrl = "https://bucket.s3.region.amazonaws.com/profiles/" + uuid + "/old.jpg";
			String oldKey = "profiles/" + uuid + "/old.jpg";

			Profile mockProfile = mock(Profile.class);
			given(mockProfile.getProfileImageUrl()).willReturn(oldUrl);

			User mockUser = mock(User.class);
			given(mockUser.getProfile()).willReturn(mockProfile);
			given(userRepository.findByUuid(uuid)).willReturn(Optional.of(mockUser));
			given(s3Service.extractKeyFromUrl(oldUrl)).willReturn(oldKey);

			userService.deleteProfileImage(uuid);

			then(s3Service).should().deleteObject(oldKey);
			then(mockProfile).should().updateProfileImage(null);
		}

		@Test
		@DisplayName("이미지 없을 때 S3 삭제 호출 없음")
		void noImage () {
			UUID uuid = UUID.randomUUID();

			Profile mockProfile = mock(Profile.class);
			given(mockProfile.getProfileImageUrl()).willReturn(null);

			User mockUser = mock(User.class);
			given(mockUser.getProfile()).willReturn(mockProfile);
			given(userRepository.findByUuid(uuid)).willReturn(Optional.of(mockUser));

			userService.deleteProfileImage(uuid);

			then(s3Service).should(never()).deleteObject(any());
		}

		@Test
		@DisplayName("존재하지 않는 UUID면 UserNotFoundException")
		void userNotFound () {
			UUID uuid = UUID.randomUUID();
			given(userRepository.findByUuid(uuid)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.deleteProfileImage(uuid))
				.isInstanceOf(UserNotFoundException.class);
		}
	}
}