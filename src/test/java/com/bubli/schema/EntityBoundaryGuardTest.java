package com.bubli.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EntityBoundaryGuardTest {

	private static final Path SOURCE_ROOT = Path.of("src/main/java/com/bubli");
	private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("@Table\\(name = \"([^\"]+)\"");

	@Test
	void backendDoesNotIntroduceSharedBaseEntityOrGlobalEntityPackage() throws IOException {
		List<String> violations = new ArrayList<>();

		for (Path sourceFile : javaFiles()) {
			if (sourceFile.toString().contains("/global/entity/")) {
				violations.add("global/entity Java source must not be introduced: " + sourceFile);
			}
			if (sourceFile.getFileName().toString().equals("BaseTimeEntity.java")) {
				violations.add("BaseTimeEntity must not be introduced: " + sourceFile);
			}
		}

		assertThat(violations).isEmpty();
	}

	@Test
	void serverJpaEntitiesDoNotMapTauriLocalTables() throws IOException {
		List<String> violations = new ArrayList<>();

		for (Path sourceFile : javaFiles()) {
			String source = Files.readString(sourceFile);
			if (!source.contains("@Entity")) {
				continue;
			}
			Matcher matcher = TABLE_NAME_PATTERN.matcher(source);
			if (matcher.find() && matcher.group(1).startsWith("local_")) {
				violations.add(sourceFile + " maps server JPA entity to " + matcher.group(1));
			}
		}

		assertThat(violations).isEmpty();
	}

	private List<Path> javaFiles() throws IOException {
		try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
			return paths
					.filter(path -> path.toString().endsWith(".java"))
					.sorted()
					.toList();
		}
	}
}
