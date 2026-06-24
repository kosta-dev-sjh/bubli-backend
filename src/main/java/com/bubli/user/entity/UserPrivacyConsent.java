package com.bubli.user.entity;

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
}
