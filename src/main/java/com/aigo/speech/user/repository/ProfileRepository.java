package com.aigo.speech.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.aigo.speech.user.entity.Profile;
import com.aigo.speech.user.entity.User;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

	Optional<Profile> findByUser (User user);

	boolean existsByNickname (String nickname);

	@Modifying
	void deleteByUser (User user);

}
