package com.bubli.agent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AgentQuerySupport {

	enum WorkStateIntent {
		ACTIVE,
		COMPLETED,
		ANY
	}

	private static final List<String> COMMAND_STOPWORDS = List.of(
			"bubli", "질문", "todo", "to", "do", "task", "wbs", "만들어줘", "만들어", "생성해줘",
			"알려줘", "알려", "보여줘", "보여", "정리해줘", "정리", "추천해줘", "추천", "기준", "바탕",
			"기반", "토대로", "보고", "현재", "기존", "이", "그", "저", "해당", "파일", "문서", "자료",
			"내용", "기능", "주요", "무엇", "어떤", "에는", "에서", "으로", "으로는", "pdf",
			"what", "which", "tell", "show", "make", "create", "based", "from", "using",
			"document", "file", "resource", "material"
	);

	private AgentQuerySupport() {
	}

	static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
	}

	static String searchQuery(String value) {
		List<ResourceToken> tokens = resourceTokens(value);
		if (tokens.isEmpty()) {
			String normalized = compactResourceText(value);
			return normalized.isBlank() ? nullToEmpty(value).trim() : normalized;
		}
		return tokens.stream()
				.map(ResourceToken::value)
				.distinct()
				.reduce((left, right) -> left + " " + right)
				.orElse(nullToEmpty(value).trim());
	}

	static boolean isDocumentSourceRequest(String value) {
		String normalized = normalize(value);
		if (containsAny(normalized,
				"문서", "자료", "파일", "요구사항", "요구 명세", "요구명세", "명세서", "계약", "계약서",
				"회의록", "제안서", "보고서", "pdf", "resource", "document", "file", "contract",
				"agreement", "material", "requirements", "proposal", "meeting notes", "資料", "文書",
				"ファイル", "契約", "契約書", "要件")) {
			return true;
		}
		if (containsAny(normalized,
				"주요 내용", "주요 기능", "어떤 내용", "어떤 기능", "무슨 내용", "무슨 기능",
				"무엇을 만들어", "뭘 만들어", "만들어야", "어떤 파일", "이 파일", "this file")) {
			return true;
		}
		return containsAny(normalized, "바탕", "기반", "기준", "토대로", "based on", "from", "using")
				&& containsAny(normalized, "todo", "할 일", "할일", "작업", "task", "질문", "question", "정리");
	}

	static boolean isTaskSourceRequest(String value) {
		String normalized = normalize(value);
		return containsAny(normalized,
				"todo", "to-do", "할 일", "할일", "작업", "태스크", "task", "업무", "일감",
				"미완료", "완료", "완성", "끝난", "남은", "진행 중", "진행중", "done", "finished",
				"completed", "remaining", "unfinished", "open", "タスク", "作業", "完了", "未完了");
	}

	static boolean isWbsSourceRequest(String value) {
		String normalized = normalize(value);
		return containsAny(normalized, "wbs", "work breakdown", "업무분해", "作業分解");
	}

	static boolean isScheduleSourceRequest(String value) {
		String normalized = normalize(value);
		return containsAny(normalized, "일정", "스케줄", "캘린더", "오늘", "내일", "이번 주", "다음 주",
				"schedule", "calendar", "today", "tomorrow", "this week", "next week",
				"予定", "スケジュール", "カレンダー", "今日", "明日", "今週", "来週");
	}

	static boolean isAgentSuggestionSourceRequest(String value) {
		String normalized = normalize(value);
		return containsAny(normalized, "ai 후보", "ai후보", "후보함", "후보", "제안함", "suggestion", "candidate",
				"draft", "ai候補", "候補");
	}

	static boolean hasSourceIntent(String value) {
		String normalized = normalize(value);
		return containsAny(normalized, "기준", "바탕", "기반", "보고", "토대로", "현재", "기존", "미완료", "완료",
				"목록", "정리", "참고", "based on", "from", "using", "current", "existing", "基準", "もと",
				"基づ", "見て", "現在", "既存", "未完了", "一覧");
	}

	static WorkStateIntent workStateIntent(String value) {
		String normalized = normalize(value);
		boolean active = containsAny(normalized, "미완료", "남은", "진행 중", "진행중", "할 일", "할일",
				"todo", "in_progress", "blocked", "review", "remaining", "unfinished", "open", "未完了");
		boolean completed = containsAny(normalized, "완료된", "완료", "완성된", "완성", "끝난", "끝낸", "done",
				"finished", "completed", "完了");
		if (active && completed) {
			return WorkStateIntent.ANY;
		}
		if (active) {
			return WorkStateIntent.ACTIVE;
		}
		if (completed) {
			return WorkStateIntent.COMPLETED;
		}
		return WorkStateIntent.ANY;
	}

	static List<ResourceToken> resourceTokens(String value) {
		String compact = compactResourceText(value);
		if (compact.isBlank()) {
			return List.of();
		}
		List<ResourceToken> tokens = new ArrayList<>();
		for (String token : compact.split(" ")) {
			if (token.length() < 2 || isResourceStopword(token)) {
				continue;
			}
			tokens.add(new ResourceToken(token, Math.min(token.length(), 12)));
		}
		return tokens;
	}

	static String compactResourceText(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		StringBuilder builder = new StringBuilder(value.length());
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (Character.isLetterOrDigit(character)) {
				builder.append(Character.toLowerCase(character));
			} else {
				builder.append(' ');
			}
		}
		return builder.toString().replaceAll("\\s+", " ").trim();
	}

	static String removeAppendedNoAnswer(String answer, String noAnswer) {
		if (answer == null || answer.isBlank() || noAnswer == null || noAnswer.isBlank()) {
			return answer;
		}
		String trimmed = answer.trim();
		if (trimmed.equals(noAnswer)) {
			return trimmed;
		}
		int index = trimmed.indexOf(noAnswer);
		if (index < 0) {
			return trimmed;
		}
		String before = trimmed.substring(0, index)
				.replaceAll("[\\s\\-–—:：,，]+$", "")
				.trim();
		String after = trimmed.substring(index + noAnswer.length()).trim();
		if (!before.isBlank() && before.length() >= 12 && after.isBlank()) {
			return before;
		}
		return trimmed;
	}

	static List<String> suggestionItems(String answer, String fallback, int limit) {
		List<String> items = new ArrayList<>();
		for (String line : nullToEmpty(answer).split("\\R")) {
			String item = cleanupSuggestionLine(line);
			if (item.isBlank() || item.equals(fallback)) {
				continue;
			}
			if (item.length() > 120) {
				item = item.substring(0, 120).trim();
			}
			if (!items.contains(item)) {
				items.add(item);
			}
			if (items.size() >= limit) {
				break;
			}
		}
		return items;
	}

	static boolean containsAny(String value, String... candidates) {
		String safeValue = value == null ? "" : value;
		for (String candidate : candidates) {
			if (safeValue.contains(candidate.toLowerCase(Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isResourceStopword(String token) {
		return COMMAND_STOPWORDS.contains(token);
	}

	private static String cleanupSuggestionLine(String line) {
		String item = nullToEmpty(line).trim();
		item = item.replaceFirst("^[-*•]\\s*", "");
		item = item.replaceFirst("^\\d+[.)]\\s*", "");
		item = item.replaceFirst("^(TODO|TASK|WBS|할 일|할일|작업)\\s*[:：-]\\s*", "");
		return item.trim();
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	record ResourceToken(
			String value,
			int weight
	) {
	}
}
