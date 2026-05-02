package com.aigo.speech.terms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aigo.speech.terms.entity.UserTermsAgreement;
import com.aigo.speech.user.entity.User;

public interface UserTermsAgreementRepository extends JpaRepository<UserTermsAgreement, Long> {

	List<UserTermsAgreement> findAllByUser (User user);
}
