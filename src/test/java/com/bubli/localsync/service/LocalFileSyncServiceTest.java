package com.bubli.localsync.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.localsync.dto.LocalFileEvent;
import com.bubli.localsync.entity.LocalFileSyncEvent;
import com.bubli.localsync.repository.LocalFileSyncEventRepository;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import com.bubli.user.service.UserPublicService;
import com.bubli.user.type.ConsentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class LocalFileSyncServiceTest {

	@Mock
	ResourcePublicService resourcePublicService;

	@Mock
	UserPublicService userPublicService;

	@Mock
	LocalFileSyncEventRepository localFileSyncEventRepository;

	@InjectMocks
	LocalFileSyncService localFileSyncService;

	@BeforeEach
	void setUp() {
		lenient().when(localFileSyncEventRepository.save(any(LocalFileSyncEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void syncUpdatedLocalFileRenamesPersonalResource() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		given(userPublicService.isPrivacyConsentEnabled(userId, ConsentType.MANAGED_FOLDER))
				.willReturn(true);
		given(resourcePublicService.updatePersonalLocalFileResource(userId, resourceId, "updated-contract.pdf", 1234L, "application/pdf"))
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
		verify(resourcePublicService).updatePersonalLocalFileResource(userId, resourceId, "updated-contract.pdf", 1234L, "application/pdf");
	}

	@Test
	void syncCreatedLocalFileReturnsCachedResultWhenLocalEventIsRetried() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		String localEventId = "local-event-created-1";
		LocalFileEvent event = new LocalFileEvent(
				"CREATED",
				"brief.txt",
				20L,
				localEventId,
				"text/plain",
				null
		);
		given(userPublicService.isPrivacyConsentEnabled(userId, ConsentType.MANAGED_FOLDER))
				.willReturn(true);
		given(localFileSyncEventRepository.findByUserIdAndLocalEventId(userId, localEventId))
				.willReturn(Optional.empty())
				.willReturn(Optional.of(LocalFileSyncEvent.create(userId, localEventId, "CREATED", resourceId, "SYNCED")));
		given(resourcePublicService.createPersonalLocalFileResource(userId, "brief.txt", 20L, "text/plain"))
				.willReturn(resourceResult(resourceId, "brief.txt"));

		var first = localFileSyncService.sync(userId, List.of(event));
		var retry = localFileSyncService.sync(userId, List.of(event));

		assertThat(first.results()).hasSize(1);
		assertThat(first.results().get(0).resourceId()).isEqualTo(resourceId);
		assertThat(first.results().get(0).status()).isEqualTo("SYNCED");
		assertThat(retry.results()).hasSize(1);
		assertThat(retry.results().get(0).resourceId()).isEqualTo(resourceId);
		assertThat(retry.results().get(0).status()).isEqualTo("SYNCED");
		verify(resourcePublicService, times(1)).createPersonalLocalFileResource(userId, "brief.txt", 20L, "text/plain");
		verify(localFileSyncEventRepository).save(any(LocalFileSyncEvent.class));
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
	void syncContinuesWhenOneEventFails() {
		UUID userId = UUID.randomUUID();
		UUID missingResourceId = UUID.randomUUID();
		UUID updatedResourceId = UUID.randomUUID();
		given(userPublicService.isPrivacyConsentEnabled(userId, ConsentType.MANAGED_FOLDER))
				.willReturn(true);
		given(resourcePublicService.updatePersonalLocalFileResource(userId, missingResourceId, "deleted-local-file.rtf", 10L, null))
				.willThrow(new BusinessException(ErrorCode.RESOURCE_404_001));
		given(resourcePublicService.updatePersonalLocalFileResource(userId, updatedResourceId, "README.md", 20L, "text/markdown"))
				.willReturn(resourceResult(updatedResourceId, "README.md"));

		var response = localFileSyncService.sync(userId, List.of(
				new LocalFileEvent(
						"UPDATED",
						"deleted-local-file.rtf",
						10L,
						"local-event-failed",
						null,
						missingResourceId
				),
				new LocalFileEvent(
						"UPDATED",
						"README.md",
						20L,
						"local-event-synced",
						"text/markdown",
						updatedResourceId
				)
		));

		assertThat(response.results()).hasSize(2);
		assertThat(response.results().get(0).localEventId()).isEqualTo("local-event-failed");
		assertThat(response.results().get(0).resourceId()).isEqualTo(missingResourceId);
		assertThat(response.results().get(0).status()).isEqualTo("FAILED");
		assertThat(response.results().get(1).localEventId()).isEqualTo("local-event-synced");
		assertThat(response.results().get(1).resourceId()).isEqualTo(updatedResourceId);
		assertThat(response.results().get(1).status()).isEqualTo("SYNCED");
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
