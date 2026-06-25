package com.bubli.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainDependencyArchitectureTest {

	@Test
	void workScheduleDoesNotDependOnOtherDomainRepositoryOrEntity() {
		JavaClasses classes = new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages("com.bubli");

		List<String> violations = classes.stream()
				.filter(javaClass -> javaClass.getPackageName().startsWith("com.bubli.work.schedule"))
				.flatMap(javaClass -> javaClass.getDirectDependenciesFromSelf().stream()
						.filter(dependency -> isOtherDomainPersistenceDependency(
								javaClass,
								dependency.getTargetClass()
						))
						.map(dependency -> javaClass.getName() + " -> " + dependency.getTargetClass().getName()))
				.distinct()
				.sorted()
				.toList();

		assertThat(violations).isEmpty();
	}

	private boolean isOtherDomainPersistenceDependency(JavaClass originClass, JavaClass targetClass) {
		String targetPackage = targetClass.getPackageName();
		if (!targetPackage.startsWith("com.bubli.")) {
			return false;
		}
		if (!targetPackage.contains(".entity") && !targetPackage.contains(".repository")) {
			return false;
		}
		return !domainKey(originClass.getPackageName()).equals(domainKey(targetPackage));
	}

	private String domainKey(String packageName) {
		String[] parts = packageName.replaceFirst("^com\\.bubli\\.", "").split("\\.");
		if (parts.length == 0) {
			return "";
		}
		if (("work".equals(parts[0]) || "personal".equals(parts[0])) && parts.length > 1) {
			return parts[0] + "." + parts[1];
		}
		return parts[0];
	}
}
