package com.bubli.localsync.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.localsync.dto.LocalFileEvent;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import com.bubli.user.service.UserPublicService;
import com.bubli.user.type.ConsentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class LocalFileSyncServiceTest {

	@Mock
	ResourcePublicService resourcePublicService;

	@Mock
	UserPublicService userPublicService;

	@InjectMocks
	LocalFileSyncService localFileSyncService;

	@Test
	void syncUpdatedLocalFileRenamesPersonalResource() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		given(userPublicService.isPrivacyConsentEnabled(userId, ConsentType.MANAGED_FOLDER))
				.willReturn(true);
		given(resourcePublicService.updatePersonalResource(userId, resourceId, "updated-contract.pdf"))
				.willReturn(resourceResult(resourceId, "updated-contract.pdf"));

		var response = localFileSyncService.sync(userId, List.of(new LocalFileEvent(
				"UPDATED",
				"updated-contract.pdf",
				1234L,
				"local-event-1",
				"application/pdf",
				resourceId
		)));

		assertThat(response.results()).hasSize(1);
		assertThat(response.results().get(0).eventType()).isEqualTo("UPDATED");
		assertThat(response.results().get(0).localEventId()).isEqualTo("local-event-1");
		assertThat(response.results().get(0).resourceId()).isEqualTo(resourceId);
		assertThat(response.results().get(0).status()).isEqualTo("SYNCED");
		verify(userPublicService).isPrivacyConsentEnabled(userId, ConsentType.MANAGED_FOLDER);
		verify(resourcePublicService).updatePersonalResource(userId, resourceId, "updated-contract.pdf");
	}

	@Test
	void syncUpdatedLocalFileSkipsWhenResourceIdIsMissing() {
		UUID userId = UUID.randomUUID();
		given(userPublicService.isPrivacyConsentEnabled(userId, ConsentType.MANAGED_FOLDER))
				.willReturn(true);

		var response = localFileSyncService.sync(userId, List.of(new LocalFileEvent(
				"UPDATED",
				"local-only.txt",
				10L,
				"local-event-2",
				"text/plain",
				null
		)));

		assertThat(response.results()).hasSize(1);
		assertThat(response.results().get(0).eventType()).isEqualTo("UPDATED");
		assertThat(response.results().get(0).localEventId()).isEqualTo("local-event-2");
		assertThat(response.results().get(0).resourceId()).isNull();
		assertThat(response.results().get(0).status()).isEqualTo("SKIPPED");
		verify(userPublicService).isPrivacyConsentEnabled(userId, ConsentType.MANAGED_FOLDER);
		verifyNoMoreInteractions(resourcePublicService);
	}

	@Test
	void syncRejectsWhenManagedFolderConsentIsDisabled() {
		UUID userId = UUID.randomUUID();
		given(userPublicService.isPrivacyConsentEnabled(userId, ConsentType.MANAGED_FOLDER))
				.willReturn(false);

		assertThatThrownBy(() -> localFileSyncService.sync(userId, List.of(new LocalFileEvent(
				"CREATED",
				"draft.txt",
				1L,
				"local-event-3",
				"text/plain",
				null
		))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.LOCALSYNC_403_001);

		verify(userPublicService).isPrivacyConsentEnabled(userId, ConsentType.MANAGED_FOLDER);
		verifyNoInteractions(resourcePublicService);
	}

	private ResourceResult resourceResult(UUID resourceId, String title) {
		return new ResourceResult(
				resourceId,
				UUID.randomUUID(),
				null,
				title,
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				ResourceStatus.READY,
				Instant.now(),
				Instant.now()
		);
	}
}
