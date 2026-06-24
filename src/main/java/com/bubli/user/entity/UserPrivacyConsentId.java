package com.bubli.user.entity;

import com.bubli.user.type.ConsentType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPrivacyConsentId implements Serializable {
	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "consent_type", nullable = false)
	@Enumerated(EnumType.STRING)
	private ConsentType consentType;
}
