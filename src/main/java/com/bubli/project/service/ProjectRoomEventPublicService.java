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
}
