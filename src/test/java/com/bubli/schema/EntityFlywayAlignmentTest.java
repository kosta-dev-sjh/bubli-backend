package com.bubli.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
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

class EntityFlywayAlignmentTest {

	private static final Path SOURCE_ROOT = Path.of("src/main/java/com/bubli");
	private static final Path MIGRATION = Path.of("src/main/resources/db/migration/V1__init_schema.sql");
	private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
			"CREATE TABLE (\\w+) \\((.*?)\\n\\);",
			Pattern.DOTALL
	);
	private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("@Table\\(name = \"([^\"]+)\"");
	private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("@Column\\(name = \"([^\"]+)\"");
	private static final Pattern FIELD_PATTERN = Pattern.compile("private\\s+[^;=]+\\s+(\\w+)\\s*(?:=[^;]+)?;");

	@Test
	void entityTablesAndColumnsExistInFlywayV1() throws IOException {
		Map<String, Set<String>> schema = parseSchema(Files.readString(MIGRATION));
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
		Map<String, Set<String>> schema = parseSchema(Files.readString(MIGRATION));

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
		return schema;
	}

	private void assertTableColumns(Map<String, Set<String>> schema, String tableName, Set<String> expectedColumns) {
		assertThat(schema)
				.as("schema contains table %s", tableName)
				.containsKey(tableName);
		assertThat(schema.get(tableName))
				.as("%s columns", tableName)
				.isEqualTo(expectedColumns);
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
