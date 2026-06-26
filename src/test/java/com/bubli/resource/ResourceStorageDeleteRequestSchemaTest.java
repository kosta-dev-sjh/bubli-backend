package com.bubli.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceStorageDeleteRequestSchemaTest {

	@Test
	void flywaySchemaContainsDeadLetterLookupIndex() throws IOException {
		String migration = Files.readString(Path.of("src/main/resources/db/migration/V1__init_schema.sql"));

		assertThat(migration)
				.contains("CREATE INDEX idx_resource_storage_delete_requests_status_updated_at")
				.contains("ON resource_storage_delete_requests (status, updated_at)");
	}
}
