package com.bubli.user.entity;

import com.bubli.user.type.ConsentType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "user_privacy_consents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPrivacyConsent {
	@EmbeddedId
	private UserPrivacyConsentId id;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	@PreUpdate
	protected void touch() {
		this.updatedAt = Instant.now();
	}

	private UserPrivacyConsent(UUID userId, ConsentType consentType, boolean enabled) {
		this.id = UserPrivacyConsentId.of(userId, consentType);
		this.enabled = enabled;
		this.updatedAt = Instant.now();
	}

	public static UserPrivacyConsent create(UUID userId, ConsentType consentType, boolean enabled) {
		return new UserPrivacyConsent(userId, consentType, enabled);
	}

	public void updateEnabled(boolean enabled) {
		this.enabled = enabled;
		this.updatedAt = Instant.now();
	}

	public UUID getUserId() {
		return id.getUserId();
	}

	public ConsentType getConsentType() {
		return id.getConsentType();
	}
}
