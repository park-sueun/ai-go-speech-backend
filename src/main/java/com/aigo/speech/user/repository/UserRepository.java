package com.aigo.speech.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.user.entity.Provider;
import com.aigo.speech.user.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail (String email);

	boolean existsByEmail (String email);

	Optional<User> findByProviderAndProviderId (Provider provider, String providerId);
}
