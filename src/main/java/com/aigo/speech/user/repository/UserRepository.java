package com.aigo.speech.user.repository;

import com.aigo.speech.user.entity.Provider;
import com.aigo.speech.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}
