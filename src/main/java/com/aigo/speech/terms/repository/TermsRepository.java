package com.aigo.speech.terms.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.terms.entity.Terms;
import com.aigo.speech.terms.entity.TermsType;

public interface TermsRepository extends JpaRepository<Terms, Long> {
	Optional<Terms> findByTypeAndIsActiveTrue (TermsType type);

	List<Terms> findAllByIsActiveTrue ();

	Optional<Terms> findByUuid (UUID uuid);

	List<Terms> findAllByUuidIn (List<UUID> uuids);
}
