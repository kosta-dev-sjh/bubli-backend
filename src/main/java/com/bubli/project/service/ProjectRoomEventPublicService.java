package com.bubli.project.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectRoomEventPublicService {

    private final ProjectRoomEventRecorder projectRoomEventRecorder;

    public void recordAgentSuggestionsCreated(
            UUID actorUserId,
            UUID roomId,
            List<UUID> suggestionIds,
            List<String> suggestionTypes
    ) {
        projectRoomEventRecorder.recordAgentSuggestionsCreated(
                actorUserId,
                roomId,
                suggestionIds,
                suggestionTypes
        );
    }

    public void recordAgentSuggestionReviewed(
            UUID actorUserId,
            UUID roomId,
            UUID suggestionId,
            String suggestionType,
            String status,
            String action
    ) {
        projectRoomEventRecorder.recordAgentSuggestionReviewed(
                actorUserId,
                roomId,
                suggestionId,
                suggestionType,
                status,
                action
        );
    }

    public void recordAgentJobCompleted(
            UUID actorUserId,
            UUID roomId,
            UUID jobId,
            String jobType,
            String status,
            String message
    ) {
        projectRoomEventRecorder.recordAgentJobCompleted(actorUserId, roomId, jobId, jobType, status, message);
    }

    public void recordVoiceRoomCreated(UUID actorUserId, UUID roomId, UUID voiceRoomId, String livekitRoomName) {
        projectRoomEventRecorder.recordVoiceRoomCreated(actorUserId, roomId, voiceRoomId, livekitRoomName);
    }

    public void recordVoiceParticipantJoined(UUID actorUserId, UUID roomId, UUID voiceRoomId, UUID participantId, UUID userId) {
        projectRoomEventRecorder.recordVoiceParticipantJoined(actorUserId, roomId, voiceRoomId, participantId, userId);
    }

    public void recordVoiceParticipantLeft(UUID actorUserId, UUID roomId, UUID voiceRoomId, UUID participantId, UUID userId) {
        projectRoomEventRecorder.recordVoiceParticipantLeft(actorUserId, roomId, voiceRoomId, participantId, userId);
    }

    public void recordVoiceParticipantMicUpdated(
            UUID actorUserId,
            UUID roomId,
            UUID voiceRoomId,
            UUID participantId,
            UUID userId,
            String micStatus
    ) {
        projectRoomEventRecorder.recordVoiceParticipantMicUpdated(
                actorUserId,
                roomId,
                voiceRoomId,
                participantId,
                userId,
                micStatus
        );
    }

    public void recordVoiceRoomEnded(UUID actorUserId, UUID roomId, UUID voiceRoomId) {
        projectRoomEventRecorder.recordVoiceRoomEnded(actorUserId, roomId, voiceRoomId);
    }
}
