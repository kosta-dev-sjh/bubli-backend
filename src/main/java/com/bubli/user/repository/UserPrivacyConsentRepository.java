package com.bubli.user.repository;

import com.bubli.user.entity.UserPrivacyConsent;
import com.bubli.user.entity.UserPrivacyConsentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserPrivacyConsentRepository extends JpaRepository<UserPrivacyConsent, UserPrivacyConsentId> {

	List<UserPrivacyConsent> findByIdUserId(UUID userId);
}
