package com.bubli.agent.service;

import com.bubli.agent.dispatch.AgentJobQueueMessage;
import com.bubli.agent.dto.AgentJobContext;
import com.bubli.agent.dto.ProjectRoomAgentCommandResponse;
import com.bubli.agent.model.AiCallExecutor;
import com.bubli.agent.type.AgentCommandMode;
import com.bubli.agent.type.AgentJobType;
import com.bubli.chat.dto.ChatMessageResponse;
import com.bubli.chat.service.ChatMessagePublicService;
import com.bubli.memory.dto.RoomMemorySummaryContextResult;
import com.bubli.memory.service.RoomMemoryPublicService;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectRoomAgentCommandService {

	private static final String PROMPT_VERSION = "project-room-agent-command-v1";

	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final AgentJobContextCollector contextCollector;
	private final ChatMessagePublicService chatMessagePublicService;
	private final RoomMemoryPublicService roomMemoryPublicService;
	private final ObjectProvider<ChatModel> chatModelProvider;
	private final ObjectProvider<AiCallExecutor> aiCallExecutorProvider;
	private final ObjectMapper objectMapper;

	@Transactional
	public ProjectRoomAgentCommandResponse execute(
			UUID userId,
			UUID roomId,
			String message,
			AgentCommandMode mode,
			List<UUID> resourceIds
	) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		AgentCommandMode commandMode = mode == null ? AgentCommandMode.ANSWER : mode;
		AgentJobContext context = contextCollector.collect(new AgentJobQueueMessage(
				UUID.randomUUID(),
				userId,
				roomId,
				resourceIds == null || resourceIds.isEmpty() ? null : resourceIds.getFirst(),
				AgentJobType.GENERATE_QUESTIONS,
				Map.of(
						"source", "PROJECT_ROOM_AGENT_COMMAND",
						"mode", commandMode.name(),
						"message", message,
						"resourceIds", resourceIds == null ? List.of() : resourceIds
				),
				java.time.Instant.now()
		));
		String answer = answer(message, commandMode, context);
		ChatMessageResponse chatMessage = ChatMessageResponse.from(chatMessagePublicService.createRoomAgentResponse(
				userId,
				roomId,
				responseBody(message, commandMode, answer, context),
				resourceIds == null || resourceIds.isEmpty() ? null : resourceIds.getFirst()
		));
		RoomMemorySummaryContextResult memory = roomMemoryPublicService.createDraft(
				userId,
				roomId,
				chatMessage.roomSequence(),
				chatMessage.roomSequence(),
				memoryJson(message, commandMode, answer, context)
		);
		return new ProjectRoomAgentCommandResponse(chatMessage, memory);
	}

	private String answer(String message, AgentCommandMode mode, AgentJobContext context) {
		ChatModel chatModel = chatModelProvider.getIfAvailable();
		if (chatModel == null) {
			return fallbackAnswer(message, mode, context);
		}
		AiCallExecutor executor = aiCallExecutorProvider.getIfAvailable();
		String prompt = prompt(message, mode, context);
		if (executor == null) {
			return chatModel.call(prompt);
		}
		return executor.execute("project-room-agent-command", () -> chatModel.call(prompt));
	}

	private String prompt(String message, AgentCommandMode mode, AgentJobContext context) {
		return """
				You are Bubli's project room agent. Write a concise Korean response.
				Mode: %s
				Do not invent confirmed facts. Use the provided project context and say what should be checked when context is insufficient.

				User message:
				%s

				Project context:
				%s
				""".formatted(mode, message, context.promptBlock());
	}

	private String fallbackAnswer(String message, AgentCommandMode mode, AgentJobContext context) {
		return switch (mode) {
			case ANSWER -> "현재 수집된 프로젝트 맥락을 기준으로 확인했습니다. 요청: %s".formatted(message);
			case SUMMARIZE -> "현재 프로젝트 맥락 요약입니다. %s".formatted(context.promptBlock());
			case SUGGEST -> "다음 조치를 제안합니다: 요청 내용을 기준으로 TODO 또는 검토 항목을 생성해 승인 흐름에서 확정하세요.";
		};
	}

	private JsonNode responseBody(String request, AgentCommandMode mode, String answer, AgentJobContext context) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("text", answer);
		body.put("request", request);
		body.put("mode", mode.name());
		body.put("promptVersion", PROMPT_VERSION);
		body.put("contextCharacters", context.characterCount());
		return objectMapper.valueToTree(body);
	}

	private String memoryJson(String request, AgentCommandMode mode, String answer, AgentJobContext context) {
		Map<String, Object> memory = new LinkedHashMap<>();
		memory.put("source", "PROJECT_ROOM_AGENT_COMMAND");
		memory.put("mode", mode.name());
		memory.put("request", request);
		memory.put("answer", answer);
		memory.put("contextCharacters", context.characterCount());
		try {
			return objectMapper.writeValueAsString(memory);
		} catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize room memory summary.", exception);
		}
	}
}
