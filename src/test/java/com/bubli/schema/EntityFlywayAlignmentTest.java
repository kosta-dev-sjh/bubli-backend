package com.bubli.schema;

import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.agent.type.AiDocumentStatus;
import com.bubli.agent.type.AiDocumentType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static java.util.Map.entry;

class EntityFlywayAlignmentTest {

	private static final Path SOURCE_ROOT = Path.of("src/main/java/com/bubli");
	private static final Path MIGRATION_ROOT = Path.of("src/main/resources/db/migration");
	private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
			"CREATE TABLE (\\w+) \\((.*?)\\n\\);",
			Pattern.DOTALL
	);
	private static final Pattern ALTER_TABLE_ADD_COLUMN_PATTERN = Pattern.compile(
			"ALTER\\s+TABLE\\s+(\\w+)\\s+ADD\\s+COLUMN\\s+(\\w+)\\s+([^;]+);",
			Pattern.CASE_INSENSITIVE
	);
	private static final Pattern ALTER_TABLE_PATTERN = Pattern.compile(
			"ALTER\\s+TABLE\\s+(\\w+)\\s+(.*?);",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL
	);
	private static final Pattern CREATE_INDEX_PATTERN = Pattern.compile(
			"CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(\\w+)\\s+ON\\s+(\\w+)\\s+\\(([^)]+)\\);",
			Pattern.CASE_INSENSITIVE
	);
	private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("@Table\\(name = \"([^\"]+)\"");
	private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("@Column\\(name = \"([^\"]+)\"");
	private static final Pattern FIELD_PATTERN = Pattern.compile("private\\s+[^;=]+\\s+(\\w+)\\s*(?:=[^;]+)?;");

	@Test
	void entityTablesAndColumnsExistInFlywayMigrations() throws IOException {
		Map<String, Set<String>> schema = parseSchema(migrationSql());
		List<String> missing = new ArrayList<>();

		for (Path entityFile : entityFiles()) {
			String source = Files.readString(entityFile);
			String tableName = tableName(source);
			if (tableName == null) {
				continue;
			}
			if (!schema.containsKey(tableName)) {
				missing.add("table " + tableName + " for " + entityFile.getFileName());
				continue;
			}
			for (String columnName : columnNames(source)) {
				if (!schema.get(tableName).contains(columnName)) {
					missing.add(tableName + "." + columnName + " for " + entityFile.getFileName());
				}
			}
		}

		assertThat(missing).isEmpty();
	}

	@Test
	void agentTablesMatchCurrentDataDictionaryColumns() throws IOException {
		Map<String, Set<String>> schema = parseSchema(migrationSql());

		assertTableColumns(schema, "ai_documents", Set.of(
				"id",
				"resource_id",
				"room_id",
				"document_type",
				"detected_confidence",
				"status",
				"created_at",
				"updated_at"
		));
		assertTableColumns(schema, "agent_jobs", Set.of(
				"id",
				"requested_by_user_id",
				"room_id",
				"resource_id",
				"job_type",
				"status",
				"retry_count",
				"error_code",
				"error_message",
				"started_at",
				"finished_at",
				"created_at",
				"updated_at"
		));
		assertTableColumns(schema, "agent_job_events", Set.of(
				"id",
				"job_id",
				"event_type",
				"message",
				"created_at"
		));
		assertTableColumns(schema, "agent_model_call_logs", Set.of(
				"id",
				"job_id",
				"prompt_version",
				"schema_version",
				"model_name",
				"latency_ms",
				"input_tokens",
				"output_tokens",
				"error_code",
				"created_at"
		));
		assertTableColumns(schema, "agent_suggestions", Set.of(
				"id",
				"user_id",
				"room_id",
				"job_id",
				"resource_id",
				"suggestion_type",
				"payload_json",
				"evidence_json",
				"status",
				"created_at",
				"updated_at"
		));
	}

	@Test
	void agentTablesMatchCurrentDataDictionaryColumnTypes() throws IOException {
		Map<String, Map<String, String>> schema = parseColumnTypes(migrationSql());

		assertColumnTypes(schema, "ai_documents", Map.ofEntries(
				entry("id", "UUID"),
				entry("resource_id", "UUID"),
				entry("room_id", "UUID"),
				entry("document_type", "VARCHAR(40)"),
				entry("detected_confidence", "NUMERIC(5, 4)"),
				entry("status", "VARCHAR(30)"),
				entry("created_at", "TIMESTAMPTZ"),
				entry("updated_at", "TIMESTAMPTZ")
		));
		assertColumnTypes(schema, "agent_jobs", Map.ofEntries(
				entry("id", "UUID"),
				entry("requested_by_user_id", "UUID"),
				entry("room_id", "UUID"),
				entry("resource_id", "UUID"),
				entry("job_type", "VARCHAR(40)"),
				entry("status", "VARCHAR(30)"),
				entry("retry_count", "INTEGER"),
				entry("error_code", "VARCHAR(80)"),
				entry("error_message", "TEXT"),
				entry("started_at", "TIMESTAMPTZ"),
				entry("finished_at", "TIMESTAMPTZ"),
				entry("created_at", "TIMESTAMPTZ"),
				entry("updated_at", "TIMESTAMPTZ")
		));
		assertColumnTypes(schema, "agent_job_events", Map.ofEntries(
				entry("id", "UUID"),
				entry("job_id", "UUID"),
				entry("event_type", "VARCHAR(60)"),
				entry("message", "TEXT"),
				entry("created_at", "TIMESTAMPTZ")
		));
		assertColumnTypes(schema, "agent_model_call_logs", Map.ofEntries(
				entry("id", "UUID"),
				entry("job_id", "UUID"),
				entry("prompt_version", "VARCHAR(40)"),
				entry("schema_version", "VARCHAR(40)"),
				entry("model_name", "VARCHAR(100)"),
				entry("latency_ms", "BIGINT"),
				entry("input_tokens", "INTEGER"),
				entry("output_tokens", "INTEGER"),
				entry("error_code", "VARCHAR(80)"),
				entry("created_at", "TIMESTAMPTZ")
		));
		assertColumnTypes(schema, "agent_suggestions", Map.ofEntries(
				entry("id", "UUID"),
				entry("user_id", "UUID"),
				entry("room_id", "UUID"),
				entry("job_id", "UUID"),
				entry("resource_id", "UUID"),
				entry("suggestion_type", "VARCHAR(40)"),
				entry("payload_json", "JSONB"),
				entry("evidence_json", "JSONB"),
				entry("status", "VARCHAR(30)"),
				entry("created_at", "TIMESTAMPTZ"),
				entry("updated_at", "TIMESTAMPTZ")
		));
	}

	@Test
	void agentTablesMatchCurrentDataDictionaryForeignKeys() throws IOException {
		Map<String, Set<String>> schema = parseForeignKeys(migrationSql());

		assertForeignKeys(schema, "ai_documents", Set.of(
				"FOREIGN KEY (resource_id) REFERENCES resources(id)",
				"FOREIGN KEY (room_id) REFERENCES project_rooms(id)"
		));
		assertForeignKeys(schema, "agent_jobs", Set.of(
				"FOREIGN KEY (requested_by_user_id) REFERENCES users(id)",
				"FOREIGN KEY (room_id) REFERENCES project_rooms(id)",
				"FOREIGN KEY (resource_id) REFERENCES resources(id)"
		));
		assertForeignKeys(schema, "agent_job_events", Set.of(
				"FOREIGN KEY (job_id) REFERENCES agent_jobs(id)"
		));
		assertForeignKeys(schema, "agent_model_call_logs", Set.of(
				"FOREIGN KEY (job_id) REFERENCES agent_jobs(id)"
		));
		assertForeignKeys(schema, "agent_suggestions", Set.of(
				"FOREIGN KEY (user_id) REFERENCES users(id)",
				"FOREIGN KEY (room_id) REFERENCES project_rooms(id)",
				"FOREIGN KEY (job_id) REFERENCES agent_jobs(id)",
				"FOREIGN KEY (resource_id) REFERENCES resources(id)"
		));
	}

	@Test
	void coreLookupIndexesMatchCurrentDataDictionaryAccessPatterns() throws IOException {
		Map<String, String> indexes = parseIndexes(migrationSql());

		assertThat(indexes).containsAllEntriesOf(Map.ofEntries(
				entry("idx_room_members_user_status", "room_members(user_id,status)"),
				entry("idx_room_members_room_status", "room_members(room_id,status)"),
				entry("idx_invitations_room_status", "invitations(room_id,status)"),
				entry("idx_resources_owner_status", "resources(owner_id,status)"),
				entry("idx_resources_room_status", "resources(room_id,status)"),
				entry("idx_resource_comments_resource_created", "resource_comments(resource_id,created_at)"),
				entry("idx_agent_jobs_requested_status", "agent_jobs(requested_by_user_id,status)"),
				entry("idx_agent_jobs_room_status", "agent_jobs(room_id,status)"),
				entry("idx_agent_suggestions_user_status", "agent_suggestions(user_id,status)"),
				entry("idx_agent_suggestions_room_status", "agent_suggestions(room_id,status)"),
				entry("idx_ai_documents_room_status", "ai_documents(room_id,status)"),
				entry("idx_tasks_owner_status_due", "tasks(owner_user_id,status,due_at)"),
				entry("idx_tasks_room_status_due", "tasks(room_id,status,due_at)"),
				entry("idx_schedules_owner_starts", "schedules(owner_user_id,starts_at)"),
				entry("idx_schedules_room_starts", "schedules(room_id,starts_at)"),
				entry("idx_time_logs_user_status", "time_logs(user_id,status)"),
				entry("idx_time_logs_room_status", "time_logs(room_id,status)"),
				entry("idx_chat_room_members_user_status", "chat_room_members(user_id,status)"),
				entry("idx_chat_messages_chat_room_created", "chat_messages(chat_room_id,created_at)")
		));
	}

	@Test
	void agentEnumsContainCurrentDataDictionaryValues() {
		assertThat(enumNames(AiDocumentStatus.class))
				.containsExactlyInAnyOrder("READY", "ANALYZING", "ANALYZED", "FAILED");
		assertThat(enumNames(AiDocumentType.class))
				.contains("CONTRACT", "REQUIREMENT", "MEETING_NOTE", "REFERENCE");
		assertThat(enumNames(AgentJobStatus.class))
				.containsExactlyInAnyOrder("PENDING", "RUNNING", "SUCCEEDED", "FAILED", "CANCELED");
		assertThat(enumNames(AgentJobType.class))
				.contains(
						"ANALYZE_RESOURCE",
						"GENERATE_REQUIREMENTS",
						"GENERATE_WBS",
						"GENERATE_TASKS",
						"REVIEW_CONTRACT_DOCUMENTS",
						"GENERATE_QUESTIONS",
						"DAILY_SUMMARY"
				);
		assertThat(enumNames(AgentSuggestionStatus.class))
				.containsExactlyInAnyOrder("DRAFT", "APPROVED", "HELD", "REJECTED");
		assertThat(enumNames(AgentSuggestionType.class))
				.contains(
						"REQUIREMENT",
						"TODO",
						"WBS",
						"TASK",
						"SCHEDULE",
						"QUESTION",
						"CONTRACT_FIELD",
						"CONTRACT_REVIEW",
						"REVIEW_ITEM",
						"DOCUMENT_DRAFT",
						"DAILY_SUMMARY",
						"MEMO"
				);
	}

	private String migrationSql() throws IOException {
		try (Stream<Path> paths = Files.list(MIGRATION_ROOT)) {
			List<Path> migrations = paths
					.filter(path -> path.getFileName().toString().endsWith(".sql"))
					.sorted()
					.toList();
			StringBuilder sql = new StringBuilder();
			for (Path migration : migrations) {
				sql.append(Files.readString(migration)).append("\n");
			}
			return sql.toString();
		}
	}

	private Map<String, Set<String>> parseSchema(String sql) {
		Map<String, Set<String>> schema = new HashMap<>();
		Matcher tableMatcher = CREATE_TABLE_PATTERN.matcher(sql);
		while (tableMatcher.find()) {
			String tableName = tableMatcher.group(1);
			Set<String> columns = new HashSet<>();
			for (String rawLine : tableMatcher.group(2).split("\\n")) {
				String line = rawLine.strip().replaceFirst(",$", "");
				if (line.isBlank() || line.startsWith("CONSTRAINT") || line.startsWith("PRIMARY KEY")) {
					continue;
				}
				columns.add(line.split("\\s+")[0].replace("\"", ""));
			}
			schema.put(tableName, columns);
		}
		Matcher addColumnMatcher = ALTER_TABLE_ADD_COLUMN_PATTERN.matcher(sql);
		while (addColumnMatcher.find()) {
			String tableName = addColumnMatcher.group(1);
			String columnName = addColumnMatcher.group(2).replace("\"", "");
			schema.computeIfAbsent(tableName, ignored -> new HashSet<>()).add(columnName);
		}
		return schema;
	}

	private Map<String, Map<String, String>> parseColumnTypes(String sql) {
		Map<String, Map<String, String>> schema = new HashMap<>();
		Matcher tableMatcher = CREATE_TABLE_PATTERN.matcher(sql);
		while (tableMatcher.find()) {
			String tableName = tableMatcher.group(1);
			Map<String, String> columnTypes = new HashMap<>();
			for (String rawLine : tableMatcher.group(2).split("\\n")) {
				String line = rawLine.strip().replaceFirst(",$", "");
				if (line.isBlank() || line.startsWith("CONSTRAINT") || line.startsWith("PRIMARY KEY")) {
					continue;
				}
				String columnName = line.split("\\s+")[0].replace("\"", "");
				columnTypes.put(columnName, columnType(line.substring(line.indexOf(columnName) + columnName.length())));
			}
			schema.put(tableName, columnTypes);
		}
		Matcher addColumnMatcher = ALTER_TABLE_ADD_COLUMN_PATTERN.matcher(sql);
		while (addColumnMatcher.find()) {
			String tableName = addColumnMatcher.group(1);
			String columnName = addColumnMatcher.group(2).replace("\"", "");
			String columnDefinition = addColumnMatcher.group(3);
			schema.computeIfAbsent(tableName, ignored -> new HashMap<>())
					.put(columnName, columnType(columnDefinition));
		}
		return schema;
	}

	private Map<String, Set<String>> parseForeignKeys(String sql) {
		Map<String, Set<String>> schema = new HashMap<>();
		Matcher tableMatcher = CREATE_TABLE_PATTERN.matcher(sql);
		while (tableMatcher.find()) {
			String tableName = tableMatcher.group(1);
			Set<String> foreignKeys = new HashSet<>();
			for (String rawLine : tableMatcher.group(2).split("\\n")) {
				String line = rawLine.strip().replaceFirst(",$", "");
				if (!line.startsWith("CONSTRAINT") || !line.contains(" FOREIGN KEY ")) {
					continue;
				}
				foreignKeys.add(line.replaceFirst("^CONSTRAINT\\s+\\w+\\s+", ""));
			}
			schema.put(tableName, foreignKeys);
		}
		Matcher alterMatcher = ALTER_TABLE_PATTERN.matcher(sql);
		while (alterMatcher.find()) {
			String tableName = alterMatcher.group(1);
			for (String rawLine : alterMatcher.group(2).split("\\n")) {
				String line = rawLine.strip().replaceFirst("[,;]$", "");
				if (!line.startsWith("ADD CONSTRAINT") || !line.contains(" FOREIGN KEY ")) {
					continue;
				}
				schema.computeIfAbsent(tableName, ignored -> new HashSet<>())
						.add(line.replaceFirst("^ADD\\s+CONSTRAINT\\s+\\w+\\s+", ""));
			}
		}
		return schema;
	}

	private Map<String, String> parseIndexes(String sql) {
		Map<String, String> indexes = new HashMap<>();
		Matcher indexMatcher = CREATE_INDEX_PATTERN.matcher(sql);
		while (indexMatcher.find()) {
			String indexName = indexMatcher.group(1);
			String tableName = indexMatcher.group(2);
			String columns = indexMatcher.group(3).replaceAll("\\s+", "");
			indexes.put(indexName, tableName + "(" + columns + ")");
		}
		return indexes;
	}

	private void assertTableColumns(Map<String, Set<String>> schema, String tableName, Set<String> expectedColumns) {
		assertThat(schema)
				.as("schema contains table %s", tableName)
				.containsKey(tableName);
		assertThat(schema.get(tableName))
				.as("%s columns", tableName)
				.isEqualTo(expectedColumns);
	}

	private void assertColumnTypes(
			Map<String, Map<String, String>> schema,
			String tableName,
			Map<String, String> expectedColumnTypes
	) {
		assertThat(schema)
				.as("schema contains table %s", tableName)
				.containsKey(tableName);
		assertThat(schema.get(tableName))
				.as("%s column types", tableName)
				.containsAllEntriesOf(expectedColumnTypes);
	}

	private void assertForeignKeys(Map<String, Set<String>> schema, String tableName, Set<String> expectedForeignKeys) {
		assertThat(schema)
				.as("schema contains table %s", tableName)
				.containsKey(tableName);
		assertThat(schema.get(tableName))
				.as("%s foreign keys", tableName)
				.isEqualTo(expectedForeignKeys);
	}

	private String columnType(String columnDefinition) {
		String definition = columnDefinition.strip();
		Matcher constraintMatcher = Pattern.compile(
				"\\s+(NOT\\s+NULL|PRIMARY\\s+KEY|UNIQUE|DEFAULT|REFERENCES|CHECK|CONSTRAINT)\\b",
				Pattern.CASE_INSENSITIVE
		).matcher(definition);
		if (constraintMatcher.find()) {
			return definition.substring(0, constraintMatcher.start()).strip();
		}
		return definition;
	}

	private <E extends Enum<E>> Set<String> enumNames(Class<E> enumType) {
		return Arrays.stream(enumType.getEnumConstants())
				.map(Enum::name)
				.collect(java.util.stream.Collectors.toSet());
	}

	private List<Path> entityFiles() throws IOException {
		try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
			return paths
					.filter(path -> path.toString().contains("/entity/"))
					.filter(path -> path.toString().endsWith(".java"))
					.sorted()
					.toList();
		}
	}

	private String tableName(String source) {
		if (!source.contains("@Entity")) {
			return null;
		}
		Matcher matcher = TABLE_NAME_PATTERN.matcher(source);
		return matcher.find() ? matcher.group(1) : null;
	}

	private List<String> columnNames(String source) {
		List<String> columns = new ArrayList<>();
		List<String> annotations = new ArrayList<>();
		for (String line : source.split("\\R")) {
			String trimmed = line.strip();
			if (trimmed.startsWith("@")) {
				annotations.add(trimmed);
				continue;
			}
			Matcher fieldMatcher = FIELD_PATTERN.matcher(trimmed);
			if (fieldMatcher.find()) {
				if (!annotations.isEmpty()) {
					String annotationText = String.join("\n", annotations);
					if (!annotationText.contains("@EmbeddedId") && !annotationText.contains("@Transient")) {
						columnName(annotationText, fieldMatcher.group(1)).ifPresent(columns::add);
					}
				}
				annotations.clear();
				continue;
			}
			if (!trimmed.isBlank()) {
				annotations.clear();
			}
		}
		return columns;
	}

	private java.util.Optional<String> columnName(String annotations, String fieldName) {
		Matcher explicitNameMatcher = COLUMN_NAME_PATTERN.matcher(annotations);
		if (explicitNameMatcher.find()) {
			return java.util.Optional.of(explicitNameMatcher.group(1));
		}
		if (annotations.contains("@Column") || annotations.contains("@Id") || annotations.contains("@Enumerated")) {
			return java.util.Optional.of(camelToSnake(fieldName));
		}
		return java.util.Optional.empty();
	}

	private String camelToSnake(String value) {
		return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
	}
}
