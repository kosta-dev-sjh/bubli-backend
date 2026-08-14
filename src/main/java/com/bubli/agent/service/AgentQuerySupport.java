package com.bubli.agent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AgentQuerySupport {

	private static final Pattern REQUIREMENT_IDENTIFIER_PATTERN =
			Pattern.compile("req[-_\\s]*[a-z0-9]+[-_\\s]*\\d{2,}", Pattern.CASE_INSENSITIVE);
	private static final Pattern RESOURCE_FILE_NAME_PATTERN =
			Pattern.compile("(?i)\\b[\\p{L}\\p{N}_-]+(?:[ .][\\p{L}\\p{N}_-]+)*\\.(pdf|docx?|xlsx?|pptx?|txt|md)\\b");
	private static final Pattern QUOTED_PHRASE_PATTERN =
			Pattern.compile("[\"'“”‘’「」『』](.*?)[\"'“”‘’「」『』]");
	private static final Pattern JAPANESE_CONTENT_TOKEN_PATTERN = Pattern.compile(
			"[\\u3400-\\u4dbf\\u4e00-\\u9fff\\uf900-\\ufaff]{2,}"
					+ "|[\\u30a0-\\u30ff\\u31f0-\\u31ffー]{2,}"
					+ "|[a-z0-9]{2,}",
			Pattern.CASE_INSENSITIVE
	);

	private static final List<String> COMMAND_STOPWORDS = List.of(
			"bubli", "질문", "알려줘", "알려", "보여줘", "보여", "정리해줘", "정리", "추천해줘", "추천",
			"만들어줘", "만들어", "생성해줘", "생성", "기반", "바탕", "근거", "기준", "현재", "기존",
			"해당", "파일", "문서", "자료", "내용", "기능", "주요", "무엇", "어떤", "에서", "으로", "pdf",
			"todo", "to", "do", "task", "wbs", "what", "which", "tell", "show", "make", "create",
			"based", "from", "using", "document", "documents", "file", "files", "resource", "resources",
			"material", "materials", "upload", "uploaded", "a", "an", "the", "on", "in", "at", "by",
			"if", "do", "does", "did", "is", "are", "was", "were", "be", "been", "of", "for", "with",
			"as", "it", "happen", "happens", "please",
			"教えて", "見せて", "表示", "作って", "作成", "生成", "整理", "要約", "提案", "基づいて",
			"資料", "文書", "ファイル", "内容", "機能", "主要", "何", "どの", "どれ", "現在"
	);

	private static final List<String> SEARCH_QUERY_STOPWORDS = List.of(
			"어디", "어디에", "어디서", "있는지", "있어", "하나요", "부분", "몇", "몇번째", "페이지", "쪽",
			"줄", "문장", "원문", "인용", "출처", "근거", "확인", "찾아", "찾아줘", "설명", "요약", "초안",
			"where", "page", "line", "quote", "citation", "source", "evidence", "find", "summarize", "summary",
			"どこ", "ページ", "行", "引用", "出典", "根拠", "探して", "要約して", "説明して"
	);

	private static final List<String> ANSWERABILITY_STOPWORDS = List.of(
			"bubli", "pdf", "todo", "task", "wbs",
			"project", "projects", "uploaded",
			"document", "documents", "file", "files", "resource", "resources", "material", "materials",
			"source", "evidence", "citation", "quote", "page", "line", "find", "show", "tell", "using",
			"based", "from", "about", "what", "which", "where", "summary", "summarize", "overview",
			"\uBB38\uC11C", "\uC790\uB8CC", "\uD30C\uC77C", "\uADFC\uAC70", "\uCD9C\uCC98", "\uC778\uC6A9",
			"\uD504\uB85C\uC81D\uD2B8", "\uC5C5\uB85C\uB4DC", "\uC5C5\uB85C\uB4DC\uB41C",
			"\uB300\uD574", "\uB300\uD55C",
			"\uC694\uC57D", "\uC815\uB9AC", "\uC124\uBA85", "\uC9C8\uBB38", "\uB0B4\uC6A9", "\uC8FC\uC694",
			"\u8CC7\u6599", "\u6587\u66F8", "\u30D5\u30A1\u30A4\u30EB", "\u6839\u62E0", "\u51FA\u5178",
			"\u8981\u7D04", "\u8AAC\u660E", "\u5185\u5BB9"
	);
	private static final List<String> TITLE_ROUTING_STOPWORDS = List.of(
			"project", "projects", "uploaded", "upload",
			"\uD504\uB85C\uC81D\uD2B8", "\uC5C5\uB85C\uB4DC", "\uC5C5\uB85C\uB4DC\uB41C",
			"\u30D7\u30ED\u30B8\u30A7\u30AF\u30C8", "\u30A2\u30C3\u30D7\u30ED\u30FC\u30C9"
	);
	private static final List<String> GENERIC_ANSWERABILITY_KEYWORDS = List.of(
			"user", "users", "time", "date", "schedule", "period", "system", "version", "os", "upload", "uploaded",
			"사용자", "이용자", "시간", "일정", "기간", "시스템", "버전", "운영체제", "업로드",
			"利用者", "ユーザー", "時間", "日時", "予定", "スケジュール", "期間", "システム", "バージョン",
			"アップロード"
	);

	private AgentQuerySupport() {
	}

	static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
	}

	static AgentSearchQueryAnalysis analyze(String message, String locale) {
		return analyze(message, locale, queryIntent(message));
	}

	static AgentSearchQueryAnalysis analyze(
			String message,
			String locale,
			ProjectRoomQueryIntent intent
	) {
		String normalizedSearchQuery = searchQuery(message);
		List<String> requirementIdentifiers = requirementIdentifiers(message);
		List<String> quotedPhrases = quotedPhrases(message);
		List<String> keywords = keywordTokens(message, requirementIdentifiers, quotedPhrases, 8);
		List<String> titleTokens = resourceTokens(message).stream()
				.map(ResourceToken::value)
				.distinct()
				.limit(10)
				.toList();
		return new AgentSearchQueryAnalysis(
				intent.allowsDocumentSynthesis() ? synthesisSearchQuery(intent, message) : normalizedSearchQuery,
				locale == null || locale.isBlank() ? "ko-KR" : locale,
				keywords,
				requirementIdentifiers,
				quotedPhrases,
				titleTokens,
				keywords.isEmpty() ? List.of() : List.of(keywords),
				intent,
				DocumentScopeConfidence.NONE,
				perspective(message)
		);
	}

	static ProjectRoomQueryIntent queryIntent(String value) {
		String normalized = normalize(value);
		if (containsAny(normalized,
				"백엔드 개발자", "프론트엔드 개발자", "개발자 관점", "기획자 관점", "디자이너 관점",
				"관점에서", "관점으로", "중점 사항", "backend developer", "frontend developer",
				"from a backend", "from a frontend", "developer perspective", "の観点", "重点的")) {
			return ProjectRoomQueryIntent.ROLE_BASED_ANALYSIS;
		}
		if (containsAny(normalized,
				"비교", "차이", "공통점", "compare", "difference", "versus", " vs ", "比較", "違い")) {
			return ProjectRoomQueryIntent.DOCUMENT_COMPARISON;
		}
		if (containsAny(normalized,
				"확인해야", "확인할", "검토해야", "검토할", "주의사항", "주의 사항", "주의할 사항", "검토할 항목", "체크리스트",
				"중심적으로", "중점적으로", "중심적으로 봐야", "중점적으로 봐야", "봐야할 내용", "봐야 할 내용",
				"review checklist", "what should i check", "items to review", "確認すべき", "確認事項", "チェックリスト")) {
			return ProjectRoomQueryIntent.REVIEW_CHECKLIST;
		}
		if (isDocumentOverviewRequest(normalized)) {
			return ProjectRoomQueryIntent.DOCUMENT_OVERVIEW;
		}
		return ProjectRoomQueryIntent.GENERAL_DOCUMENT_QA;
	}

	static String synthesisSearchQuery(ProjectRoomQueryIntent intent, String message) {
		String base = stripDocumentSelectorFraming(semanticSearchQuery(message)
				.replaceFirst("(?i)^\\s*/?bubli\\s+", "")
				.trim());
		String expansion = switch (intent) {
			case GENERAL_DOCUMENT_QA, FACT_QA -> "";
			case DOCUMENT_OVERVIEW -> "주요 기능 요구사항 사용자 역할 업무 흐름 예외 조건";
			case REVIEW_CHECKLIST -> "필수 요구사항 검증 권한 상태 전이 예외 제약 미정 사항";
			case ROLE_BASED_ANALYSIS -> "기능 데이터 권한 상태 전이 검증 예외 API 연동 비기능 요구사항";
			case DOCUMENT_COMPARISON -> "주요 기능 요구사항 차이 공통점 제약 예외";
		};
		return (base + " " + expansion).replaceAll("\\s+", " ").trim();
	}

	private static String stripDocumentSelectorFraming(String value) {
		return nullToEmpty(value)
				.replaceFirst("^.*?(?:해당\\s*파일(?:에|에서)?|선택한\\s*(?:문서|파일)(?:에서|의)?|"
						+ "(?:문서|파일)(?:에서|의)\\s*|[을를]\\s*바탕으로)\\s*", "")
				.replaceFirst("(?i)^.*?(?:based\\s+on|from|using)\\s+(?:this|the|selected)?\\s*(?:document|file)\\s*", "")
				.replaceFirst("^.*?(?:この|選択した)?(?:文書|ファイル)(?:に基づいて|から|で|の)\\s*", "")
				.trim();
	}

	private static String perspective(String value) {
		String normalized = normalize(value);
		if (containsAny(normalized, "백엔드", "backend")) {
			return "BACKEND_DEVELOPER";
		}
		if (containsAny(normalized, "프론트엔드", "frontend")) {
			return "FRONTEND_DEVELOPER";
		}
		if (containsAny(normalized, "디자이너", "designer", "デザイナー")) {
			return "DESIGNER";
		}
		if (containsAny(normalized, "기획자", "planner", "product manager", "企画")) {
			return "PRODUCT_PLANNER";
		}
		return "";
	}

	static String searchQuery(String value) {
		List<String> requirementIdentifiers = requirementIdentifiers(value);
		List<String> quotedPhrases = quotedPhrases(value).stream()
				.map(AgentQuerySupport::compactResourceText)
				.filter(token -> token.length() >= 2)
				.toList();
		List<String> queryTokens = resourceTokens(value).stream()
				.map(ResourceToken::value)
				.map(AgentQuerySupport::normalizeSearchToken)
				.filter(token -> token.length() >= 2 && !isSearchQueryStopword(token))
				.distinct()
				.toList();
		List<String> mergedTokens = new ArrayList<>();
		appendDistinct(mergedTokens, requirementIdentifiers);
		appendDistinct(mergedTokens, quotedPhrases);
		appendDistinct(mergedTokens, queryTokens);
		if (mergedTokens.isEmpty()) {
			String normalized = compactResourceText(value);
			return normalized.isBlank() ? nullToEmpty(value).trim() : normalized;
		}
		return String.join(" ", mergedTokens);
	}

	static String semanticSearchQuery(String value) {
		String query = nullToEmpty(value).trim();
		if (query.isBlank()) {
			return query;
		}
		return query
				.replaceFirst("(?i)^\\s*based\\s+on\\s+(?:the\\s+)?uploaded\\s+(?:documents?|files?)\\s*[,：:]?\\s*", "")
				.replaceFirst("(?i)^\\s*(?:from|using)\\s+(?:the\\s+)?uploaded\\s+(?:documents?|files?)\\s*[,：:]?\\s*", "")
				.replaceFirst("^\\s*업로드(?:된|한)?\\s*(?:프로젝트\\s*)?(?:문서|자료|파일)(?:에서|를|에|을)?\\s*(?:기반으로|바탕으로|근거로)?\\s*[,：:]?\\s*", "")
				.replaceFirst("^\\s*アップロード(?:済み|された|した)?(?:の)?(?:資料|文書|ファイル)(?:に基づいて|から)?\\s*[,、：:]?\\s*", "")
				.trim();
	}

	static boolean isDocumentSourceRequest(String value) {
		String normalized = normalize(value);
		if (hasRequirementIdentifier(normalized)) {
			return true;
		}
		if (containsAny(normalized,
				"문서", "자료", "파일", "요구사항", "요구 명세", "요구명세", "명세서", "계약", "계약서",
				"회의록", "제안서", "보고서", "pdf", "resource", "document", "file", "contract",
				"agreement", "material", "requirements", "proposal", "meeting notes",
				"資料", "文書", "ファイル", "契約", "契約書", "要件", "要件定義", "提案書", "議事録")) {
			return true;
		}
		if (containsAny(normalized,
				"주요 내용", "주요 기능", "어떤 내용", "어떤 기능", "무슨 내용", "무슨 기능", "무엇을 만들",
				"뭘 만들", "만들어야", "this file", "what does", "what is in",
				"主な内容", "主な機能", "どんな内容", "どのような機能", "何を作")) {
			return true;
		}
		return containsAny(normalized, "바탕", "기반", "근거", "based on", "from", "using", "基づいて")
				&& containsAny(normalized, "todo", "할일", "작업", "task", "질문", "question", "정리", "質問", "整理");
	}

	static boolean isTaskSourceRequest(String value) {
		String normalized = normalize(value);
		return containsAny(normalized,
				"todo", "to-do", "할일", "작업", "태스크", "task", "업무", "마감", "미완료", "완료",
				"완성", "끝난", "진행 중", "진행중", "done", "finished", "completed", "remaining",
				"unfinished", "open", "タスク", "todo", "未完了", "完了", "進行中", "残り");
	}

	static boolean isWbsSourceRequest(String value) {
		String normalized = normalize(value);
		return containsAny(normalized, "wbs", "work breakdown", "업무분해", "作業分解");
	}

	static boolean isScheduleSourceRequest(String value) {
		String normalized = normalize(value);
		return containsAny(normalized,
				"일정", "스케줄", "캘린더", "오늘", "내일", "이번 주", "다음 주",
				"schedule", "calendar", "today", "tomorrow", "this week", "next week",
				"予定", "スケジュール", "カレンダー", "今日", "明日", "今週", "来週");
	}

	static boolean isAgentSuggestionSourceRequest(String value) {
		String normalized = normalize(value);
		return containsAny(normalized,
				"ai 후보", "ai후보", "후보", "제안", "suggestion", "candidate", "draft",
				"ai候補", "候補", "提案", "下書き");
	}

	static boolean isUserAccountQuestion(String value) {
		String normalized = normalize(value);
		return containsAny(normalized,
				"내 id", "내 아이디", "내 계정", "내 이메일", "내 프로필", "나는 누구",
				"my id", "my account", "my email", "my profile", "who am i",
				"私のid", "私のアカウント", "私のメール", "私のプロフィール", "私は誰");
	}

	static boolean hasSourceIntent(String value) {
		String normalized = normalize(value);
		return containsAny(normalized,
				"기준", "바탕", "기반", "보고", "근거", "현재", "기존", "미완료", "완료", "목록", "정리",
				"참고", "based on", "from", "using", "current", "existing",
				"基準", "基づいて", "参考", "現在", "既存", "未完了", "完了", "一覧", "整理");
	}

	static boolean hasPreciseDocumentGroundingIntent(String value) {
		String normalized = normalize(value);
		return containsAny(normalized,
				"어디", "몇 페이지", "몇페이지", "몇 번째", "몇번째", "몇 줄", "몇줄", "줄", "라인",
				"원문", "인용", "출처", "근거", "근거 문장", "문장", "찾아", "확인",
				"where", "page", "line", "quote", "citation", "source", "evidence",
				"どこ", "ページ", "行", "引用", "出典", "根拠", "探して", "確認");
	}

	static boolean requiresSemanticDocumentEvidence(String value) {
		String normalized = normalize(value);
		return hasPreciseDocumentGroundingIntent(normalized)
				|| hasRequirementIdentifier(normalized)
				|| containsAny(normalized,
				"내용", "뜻", "요약", "정리", "분석", "설명", "주요", "데이터", "요구사항", "요구 명세",
				"요구명세", "명세", "기능", "무엇", "뭘", "어떤", "무슨", "말하", "포함", "있는가",
				"summary", "summarize", "summarise", "content", "key point", "main point", "explain",
				"data", "requirement", "requirements", "feature", "include", "contain", "means",
				"内容", "意味", "要約", "整理", "分析", "説明", "主要", "要件", "機能", "何", "含む");
	}

	static boolean isDocumentOverviewRequest(String value) {
		String normalized = normalize(value);
		return containsAny(normalized,
				"뜻", "요약", "주요 내용", "주요내용", "중요한 내용", "핵심 내용", "핵심내용",
				"전체 내용", "전체내용", "어떤 내용", "무슨 내용",
				"해결하려는 문제", "주요 사용자",
				"개요", "정리", "핵심", "summary", "summarize", "summarise", "overview", "key point", "main point",
				"意味", "要約", "概要", "主な内容", "重要な内容", "全体内容", "整理");
	}

	static WorkStateIntent workStateIntent(String value) {
		String normalized = normalize(value);
		boolean active = containsAny(normalized,
				"미완료", "진행 중", "진행중", "할일", "todo", "in_progress", "blocked", "review",
				"remaining", "unfinished", "open", "未完了", "進行中", "残り");
		boolean completed = containsAny(normalized,
				"완료된", "완료", "완성된", "완성", "끝난", "끝냄", "done", "finished", "completed", "完了");
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

	static boolean isAnswerabilityStopword(String token) {
		String normalized = normalizeSearchToken(compactResourceText(token));
		return normalized.isBlank()
				|| ANSWERABILITY_STOPWORDS.contains(normalized)
				|| isSearchQueryStopword(normalized);
	}

	static boolean isGenericAnswerabilityKeyword(String token) {
		String normalized = normalizeSearchToken(compactResourceText(token));
		return normalized.isBlank()
				|| isAnswerabilityStopword(normalized)
				|| GENERIC_ANSWERABILITY_KEYWORDS.contains(normalized);
	}

	static boolean isTitleRoutingStopword(String token) {
		String normalized = normalizeSearchToken(compactResourceText(token));
		return normalized.isBlank() || TITLE_ROUTING_STOPWORDS.contains(normalized);
	}

	static boolean isJapaneseLocale(String locale) {
		return locale != null && locale.toLowerCase(Locale.ROOT).startsWith("ja");
	}

	static QueryLanguage queryLanguage(String value) {
		String text = nullToEmpty(value);
		if (text.codePoints().anyMatch(character -> character >= 0xAC00 && character <= 0xD7A3)) {
			return QueryLanguage.KOREAN;
		}
		if (containsJapaneseScript(text)) {
			return QueryLanguage.JAPANESE;
		}
		if (text.codePoints().anyMatch(Character::isLetter)) {
			return QueryLanguage.ENGLISH;
		}
		return QueryLanguage.UNKNOWN;
	}

	static boolean supportsDocumentSearchLanguage(String message, String documentSearchLanguage) {
		String normalized = normalize(message);
		if (hasRequirementIdentifier(message) || normalized.contains("req-")
				|| RESOURCE_FILE_NAME_PATTERN.matcher(nullToEmpty(message)).find()) {
			return true;
		}
		return switch (documentSearchLanguage == null ? "ko" : documentSearchLanguage.toLowerCase(Locale.ROOT)) {
			case "ko", "ko-kr" -> queryLanguage(message) == QueryLanguage.KOREAN;
			case "en", "en-us" -> queryLanguage(message) == QueryLanguage.ENGLISH;
			case "ja", "ja-jp" -> queryLanguage(message) == QueryLanguage.JAPANESE;
			default -> false;
		};
	}

	static String documentQueryLanguage(String message) {
		if (hasRequirementIdentifier(message) || normalize(message).contains("req-")
				|| RESOURCE_FILE_NAME_PATTERN.matcher(nullToEmpty(message)).find()) {
			return null;
		}
		return switch (queryLanguage(message)) {
			case KOREAN -> "ko";
			case ENGLISH -> "en";
			case JAPANESE -> "ja";
			case UNKNOWN -> "unknown";
		};
	}

	static List<ResourceToken> resourceTokens(String value) {
		String compact = compactResourceText(value);
		if (compact.isBlank()) {
			return List.of();
		}
		List<ResourceToken> tokens = new ArrayList<>();
		for (String token : tokenCandidates(compact)) {
			if (token.length() < 2 || isResourceStopword(token)) {
				continue;
			}
			tokens.add(new ResourceToken(token, Math.min(token.length(), 12)));
		}
		return tokens;
	}

	private static List<String> tokenCandidates(String compact) {
		if (!containsJapaneseScript(compact)) {
			return List.of(compact.split(" "));
		}
		String segmented = compact
				.replaceAll("(に基づいて|について|どのように|教えてください|してください|できますか|できるか)", " ")
				.replaceAll("(から|まで)", " ")
				.replaceAll("[のをがはにでとやへも]　?", " ");
		List<String> tokens = new ArrayList<>();
		for (String token : segmented.split(" ")) {
			String normalized = token.trim();
			if (normalized.length() < 2) {
				continue;
			}
			Matcher contentMatcher = JAPANESE_CONTENT_TOKEN_PATTERN.matcher(normalized);
			boolean extracted = false;
			while (contentMatcher.find()) {
				String contentToken = contentMatcher.group();
				if (!tokens.contains(contentToken)) {
					tokens.add(contentToken);
				}
				extracted = true;
			}
			if (!extracted && !tokens.contains(normalized)) {
				tokens.add(normalized);
			}
		}
		return tokens;
	}

	private static boolean containsJapaneseScript(String value) {
		return value.codePoints().anyMatch(AgentQuerySupport::isJapaneseScript);
	}

	private static boolean isJapaneseScript(int character) {
		return (character >= 0x3040 && character <= 0x30ff)
				|| (character >= 0x3400 && character <= 0x4dbf)
				|| (character >= 0x4e00 && character <= 0x9fff)
				|| (character >= 0xf900 && character <= 0xfaff)
				|| (character >= 0x20000 && character <= 0x2fa1f);
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
				.replaceAll("[\\s\\-–—:：]+$", "")
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

	static boolean hasRequirementIdentifier(String value) {
		return !requirementIdentifiers(value).isEmpty();
	}

	static List<String> requirementIdentifiers(String value) {
		String normalized = normalize(value);
		if (normalized.isBlank()) {
			return List.of();
		}
		List<String> identifiers = new ArrayList<>();
		Matcher matcher = REQUIREMENT_IDENTIFIER_PATTERN.matcher(normalized);
		while (matcher.find()) {
			String identifier = matcher.group()
					.replaceAll("[_\\s]+", "-")
					.replaceAll("-+", "-");
			if (!identifiers.contains(identifier)) {
				identifiers.add(identifier);
			}
		}
		return identifiers;
	}

	private static List<String> keywordTokens(
			String value,
			List<String> requirementIdentifiers,
			List<String> quotedPhrases,
			int limit
	) {
		List<String> tokens = new ArrayList<>();
		appendDistinct(tokens, requirementIdentifiers);
		for (String phrase : quotedPhrases) {
			appendDistinct(tokens, compactResourceText(phrase).split(" "));
		}
		for (ResourceToken token : resourceTokens(value)) {
			if (tokens.size() >= limit) {
				break;
			}
			String normalized = normalizeSearchToken(token.value());
			if (normalized.length() >= 2 && !isSearchQueryStopword(normalized) && !tokens.contains(normalized)) {
				tokens.add(normalized);
			}
		}
		return tokens.size() <= limit ? tokens : tokens.subList(0, limit);
	}

	private static List<String> quotedPhrases(String value) {
		String text = nullToEmpty(value);
		if (text.isBlank()) {
			return List.of();
		}
		List<String> phrases = new ArrayList<>();
		Matcher matcher = QUOTED_PHRASE_PATTERN.matcher(text);
		while (matcher.find()) {
			String phrase = matcher.group(1).trim();
			if (phrase.length() >= 2 && !phrases.contains(phrase)) {
				phrases.add(phrase);
			}
		}
		return phrases;
	}

	private static void appendDistinct(List<String> target, List<String> values) {
		for (String value : values) {
			if (value != null && !value.isBlank() && !target.contains(value)) {
				target.add(value);
			}
		}
	}

	private static void appendDistinct(List<String> target, String[] values) {
		for (String value : values) {
			if (value != null && !value.isBlank() && !target.contains(value)) {
				target.add(value);
			}
		}
	}

	private static boolean isResourceStopword(String token) {
		return COMMAND_STOPWORDS.contains(token);
	}

	private static boolean isSearchQueryStopword(String token) {
		return COMMAND_STOPWORDS.contains(token) || SEARCH_QUERY_STOPWORDS.contains(token);
	}

	private static String normalizeSearchToken(String token) {
		String normalized = nullToEmpty(token).trim();
		for (String suffix : List.of(
				"에서는", "에서", "으로는", "으로", "로는", "에게", "한테", "부터", "보다",
				"처럼", "을", "를", "이", "가", "은", "는", "에", "의", "와", "과", "로",
				"には", "では", "から", "まで", "より", "を", "が", "は", "に", "の", "と")) {
			if (normalized.length() > suffix.length() + 1 && normalized.endsWith(suffix)) {
				return normalized.substring(0, normalized.length() - suffix.length());
			}
		}
		return normalized;
	}

	private static String cleanupSuggestionLine(String line) {
		String item = nullToEmpty(line).trim();
		item = item.replaceFirst("^[-*•]\\s*", "");
		item = item.replaceFirst("^\\d+[.)]\\s*", "");
		item = item.replaceFirst("^(TODO|TASK|WBS|할일|작업|タスク|質問)\\s*[:：]\\s*", "");
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

	enum WorkStateIntent {
		ACTIVE,
		COMPLETED,
		ANY
	}

	enum QueryLanguage {
		KOREAN,
		ENGLISH,
		JAPANESE,
		UNKNOWN
	}
}
