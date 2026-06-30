package com.bubli.agent.model;

import com.bubli.agent.contract.v1.AgentAnalysisResult;
import com.bubli.agent.contract.v1.Suggestion;
import com.bubli.agent.contract.v1.SuggestionType;
import com.bubli.agent.validation.AgentAnalysisResultValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AgentAnalysisFixtureRegressionTest {

	private static jakarta.validation.ValidatorFactory validatorFactory;
	private static AgentAnalysisResultJsonParser parser;

	@BeforeAll
	static void setUp() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		Validator validator = validatorFactory.getValidator();
		parser = new AgentAnalysisResultJsonParser(
				new ObjectMapper(),
				new AgentAnalysisResultValidator(validator)
		);
	}

	@AfterAll
	static void tearDown() {
		validatorFactory.close();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("scenarioFixtures")
	void scenarioFixturesMatchAnalysisV1Contract(ScenarioFixture fixture) throws Exception {
		AgentAnalysisResult result = parser.parse(readFixture(fixture.filename()));

		assertThat(result.schemaVersion()).isEqualTo(AgentAnalysisResult.SCHEMA_VERSION);
		assertThat(result.model().promptVersion()).isNotBlank();
		assertThat(result.analysis().summary()).isNotBlank();
		assertThat(result.analysis().keywords()).isNotNull();
		assertThat(result.analysis().risks()).isNotNull();
		assertThat(result.analysis().checklist()).isNotNull();
		assertThat(result.suggestions())
				.extracting(Suggestion::type)
				.containsExactlyElementsOf(fixture.expectedTypes());

		for (Suggestion suggestion : result.suggestions()) {
			assertThat(suggestion.title()).as("%s title".formatted(fixture.filename())).isNotBlank();
			assertThat(suggestion.description()).as("%s description".formatted(fixture.filename())).isNotBlank();
			assertThat(suggestion.sourceText()).as("%s sourceText".formatted(fixture.filename())).isNotBlank();
			assertThat(suggestion.confidence()).as("%s confidence".formatted(fixture.filename()))
					.isNotNull()
					.isBetween(0.0, 1.0);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("scenarioFixtures")
	void scenarioFixturesCoverRequiredSuggestionTypeFields(ScenarioFixture fixture) throws Exception {
		AgentAnalysisResult result = parser.parse(readFixture(fixture.filename()));

		for (Suggestion suggestion : result.suggestions()) {
			if (suggestion.type() == SuggestionType.CONTRACT_FIELD) {
				assertThat(suggestion.fieldKey()).isNotBlank();
				assertThat(suggestion.value()).isNotBlank();
			}
			if (suggestion.type() == SuggestionType.DAILY_SUMMARY) {
				assertThat(suggestion.description())
						.contains("done", "remaining", "tomorrowFocus", "risks", "evidence");
			}
		}
	}

	@ParameterizedTest
	@MethodSource("allSuggestionTypes")
	void scenarioFixtureSetCoversEverySuggestionType(SuggestionType suggestionType) {
		Set<SuggestionType> coveredTypes = scenarioFixtures()
				.flatMap(fixture -> fixture.expectedTypes().stream())
				.collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(SuggestionType.class)));

		assertThat(coveredTypes).contains(suggestionType);
	}

	private static Stream<ScenarioFixture> scenarioFixtures() {
		return Stream.of(
				new ScenarioFixture(
						"analysis-v1-contract-review.json",
						List.of(SuggestionType.REVIEW_ITEM, SuggestionType.CONTRACT_FIELD)
				),
				new ScenarioFixture(
						"analysis-v1-requirements.json",
						List.of(SuggestionType.REQUIREMENT, SuggestionType.WBS)
				),
				new ScenarioFixture(
						"analysis-v1-meeting-tasks.json",
						List.of(SuggestionType.TASK, SuggestionType.QUESTION)
				),
				new ScenarioFixture(
						"analysis-v1-document-draft.json",
						List.of(SuggestionType.DOCUMENT_DRAFT)
				),
				new ScenarioFixture(
						"analysis-v1-daily-summary.json",
						List.of(SuggestionType.DAILY_SUMMARY)
				)
		);
	}

	private static Stream<SuggestionType> allSuggestionTypes() {
		return Stream.of(SuggestionType.values());
	}

	private static String readFixture(String filename) throws IOException {
		String path = "/fixtures/agent/" + filename;
		try (InputStream inputStream = AgentAnalysisFixtureRegressionTest.class.getResourceAsStream(path)) {
			if (inputStream == null) {
				throw new IOException("Fixture not found: " + path);
			}
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private record ScenarioFixture(
			String filename,
			List<SuggestionType> expectedTypes
	) {

		@Override
		public String toString() {
			return filename;
		}
	}
}
