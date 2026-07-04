package com.bubli.user.repository;

import com.bubli.user.entity.UserPrivacyConsent;
import com.bubli.user.entity.UserPrivacyConsentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserPrivacyConsentRepository extends JpaRepository<UserPrivacyConsent, UserPrivacyConsentId> {

	List<UserPrivacyConsent> findByIdUserId(UUID userId);

	@Modifying
	@Query(value = """
			INSERT INTO user_privacy_consents (
			    user_id, consent_type, enabled, updated_at
			)
			VALUES (
			    :userId, :consentType, :enabled, CURRENT_TIMESTAMP
			)
			ON CONFLICT (user_id, consent_type)
			DO UPDATE SET
			    enabled = EXCLUDED.enabled,
			    updated_at = CURRENT_TIMESTAMP
			""", nativeQuery = true)
	int upsertEnabled(
			@Param("userId") UUID userId,
			@Param("consentType") String consentType,
			@Param("enabled") boolean enabled
	);
}
