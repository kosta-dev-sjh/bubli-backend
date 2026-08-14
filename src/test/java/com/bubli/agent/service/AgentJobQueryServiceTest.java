package com.bubli.agent.service;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.agent.type.AgentJobType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentJobQueryServiceTest {

    @Test
    void requesterCanReadOwnJobWithoutRoomMembershipCheck() {
        UUID requesterId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        AgentJob job = job(jobId, requesterId, roomId);
        AgentJobRepository jobRepository = mock(AgentJobRepository.class);
        AgentSuggestionRepository suggestionRepository = mock(AgentSuggestionRepository.class);
        ProjectMembershipPublicService membershipService = mock(ProjectMembershipPublicService.class);
        AgentJobQueryService service = service(jobRepository, suggestionRepository, membershipService);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(suggestionRepository.findAllByJobIdOrderByCreatedAtAsc(jobId)).thenReturn(List.of());

        var result = service.getJob(requesterId, jobId);

        assertThat(result.jobId()).isEqualTo(jobId);
        verify(membershipService, never()).assertActiveMember(requesterId, roomId);
    }

    @Test
    void activeRoomMemberCanReadRoomJobRequestedByAnotherUser() {
        UUID requesterId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        AgentJob job = job(jobId, requesterId, roomId);
        AgentJobRepository jobRepository = mock(AgentJobRepository.class);
        AgentSuggestionRepository suggestionRepository = mock(AgentSuggestionRepository.class);
        ProjectMembershipPublicService membershipService = mock(ProjectMembershipPublicService.class);
        AgentJobQueryService service = service(jobRepository, suggestionRepository, membershipService);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(suggestionRepository.findAllByJobIdOrderByCreatedAtAsc(jobId)).thenReturn(List.of());

        var result = service.getJob(memberId, jobId);

        assertThat(result.jobId()).isEqualTo(jobId);
        verify(membershipService).assertActiveMember(memberId, roomId);
    }

    @Test
    void otherUserCannotReadPersonalJob() {
        UUID requesterId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        AgentJob job = job(jobId, requesterId, null);
        AgentJobRepository jobRepository = mock(AgentJobRepository.class);
        ProjectMembershipPublicService membershipService = mock(ProjectMembershipPublicService.class);
        AgentJobQueryService service = service(
                jobRepository,
                mock(AgentSuggestionRepository.class),
                membershipService
        );
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.getJob(otherUserId, jobId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AGENT_404_001);

        verify(membershipService, never()).assertActiveMember(otherUserId, null);
    }

    private AgentJobQueryService service(
            AgentJobRepository jobRepository,
            AgentSuggestionRepository suggestionRepository,
            ProjectMembershipPublicService membershipService
    ) {
        return new AgentJobQueryService(
                jobRepository,
                suggestionRepository,
                mock(ResourceAnalysisPublicService.class),
                membershipService
        );
    }

    private AgentJob job(UUID jobId, UUID requesterId, UUID roomId) {
        AgentJob job = AgentJob.create(
                requesterId,
                roomId,
                null,
                AgentJobType.GENERATE_TASKS
        );
        ReflectionTestUtils.setField(job, "id", jobId);
        return job;
    }
}
