package com.bubli.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {

	private static final String BASE_PACKAGE = "com.bubli";
	private static final Set<String> CROSS_DOMAIN_ALLOWED_LAYERS = Set.of("dto", "type");
	private static final Set<String> INTERNAL_LAYERS = Set.of(
			"controller",
			"service",
			"repository",
			"entity",
			"dto",
			"type",
			"job",
			"suggestion",
			"prompt",
			"rag",
			"model",
			"error"
	);

	private final JavaClasses classes = new ClassFileImporter()
			.withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages(BASE_PACKAGE);

	@Test
	void domains_should_access_other_domains_only_through_public_contracts() {
		List<String> violations = new ArrayList<>();

		for (JavaClass sourceClass : classes) {
			Domain sourceDomain = Domain.from(sourceClass.getPackageName());
			if (sourceDomain == null || sourceDomain.isGlobal()) {
				continue;
			}

			for (Dependency dependency : sourceClass.getDirectDependenciesFromSelf()) {
				JavaClass targetClass = dependency.getTargetClass();
				Domain targetDomain = Domain.from(targetClass.getPackageName());
				if (targetDomain == null || targetDomain.isGlobal() || sourceDomain.equals(targetDomain)) {
					continue;
				}

				String targetLayer = targetDomain.layerOf(targetClass.getPackageName());
				if (CROSS_DOMAIN_ALLOWED_LAYERS.contains(targetLayer)) {
					continue;
				}
				if ("service".equals(targetLayer) && targetClass.getSimpleName().endsWith("PublicService")) {
					continue;
				}

				violations.add(message(sourceClass, targetClass, dependency));
			}
		}

		assertThat(violations).isEmpty();
	}

	@Test
	void layers_should_not_depend_on_upper_or_unrelated_layers() {
		List<String> violations = new ArrayList<>();

		for (JavaClass sourceClass : classes) {
			String sourceLayer = layerOf(sourceClass);
			if (sourceLayer.isBlank()) {
				continue;
			}

			for (Dependency dependency : sourceClass.getDirectDependenciesFromSelf()) {
				JavaClass targetClass = dependency.getTargetClass();
				if (!targetClass.getPackageName().startsWith(BASE_PACKAGE + ".")) {
					continue;
				}

				String targetLayer = layerOf(targetClass);
				if (targetLayer.isBlank()) {
					continue;
				}

				if ("controller".equals(sourceLayer) && Set.of("repository", "entity").contains(targetLayer)) {
					violations.add(message(sourceClass, targetClass, dependency));
				}
				if ("service".equals(sourceLayer) && "controller".equals(targetLayer)) {
					violations.add(message(sourceClass, targetClass, dependency));
				}
				if ("repository".equals(sourceLayer) && !Set.of("entity", "type").contains(targetLayer)) {
					violations.add(message(sourceClass, targetClass, dependency));
				}
				if ("entity".equals(sourceLayer) && Set.of("controller", "service", "repository", "dto").contains(targetLayer)) {
					violations.add(message(sourceClass, targetClass, dependency));
				}
			}
		}

		assertThat(violations).isEmpty();
	}

	@Test
	void services_should_not_accept_or_depend_on_request_dtos() {
		List<String> violations = new ArrayList<>();

		for (JavaClass sourceClass : classes) {
			if (!"service".equals(layerOf(sourceClass))) {
				continue;
			}

			for (Dependency dependency : sourceClass.getDirectDependenciesFromSelf()) {
				JavaClass targetClass = dependency.getTargetClass();
				if (targetClass.getPackageName().startsWith(BASE_PACKAGE + ".")
						&& targetClass.getSimpleName().endsWith("Request")
						&& !targetClass.getPackageName().contains(".entity")) {
					violations.add(message(sourceClass, targetClass, dependency));
				}
			}
		}

		assertThat(violations).isEmpty();
	}

	@Test
	void global_should_not_depend_on_domain_packages() {
		List<String> violations = new ArrayList<>();

		for (JavaClass sourceClass : classes) {
			Domain sourceDomain = Domain.from(sourceClass.getPackageName());
			if (sourceDomain == null || !sourceDomain.isGlobal()) {
				continue;
			}

			for (Dependency dependency : sourceClass.getDirectDependenciesFromSelf()) {
				JavaClass targetClass = dependency.getTargetClass();
				Domain targetDomain = Domain.from(targetClass.getPackageName());
				if (targetDomain != null && !targetDomain.isGlobal()) {
					violations.add(message(sourceClass, targetClass, dependency));
				}
			}
		}

		assertThat(violations).isEmpty();
	}

	@Test
	void agent_should_not_access_confirmed_data_owners_directly() {
		List<String> violations = new ArrayList<>();

		for (JavaClass sourceClass : classes) {
			Domain sourceDomain = Domain.from(sourceClass.getPackageName());
			if (sourceDomain == null || !"agent".equals(sourceDomain.name())) {
				continue;
			}

			for (Dependency dependency : sourceClass.getDirectDependenciesFromSelf()) {
				JavaClass targetClass = dependency.getTargetClass();
				Domain targetDomain = Domain.from(targetClass.getPackageName());
				if (targetDomain == null || targetDomain.isGlobal() || "agent".equals(targetDomain.name())) {
					continue;
				}

				String targetLayer = targetDomain.layerOf(targetClass.getPackageName());
				if (Set.of("repository", "entity").contains(targetLayer)) {
					violations.add(message(sourceClass, targetClass, dependency));
				}
			}
		}

		assertThat(violations).isEmpty();
	}

	private static String layerOf(JavaClass javaClass) {
		Domain domain = Domain.from(javaClass.getPackageName());
		if (domain == null) {
			return "";
		}
		return domain.layerOf(javaClass.getPackageName());
	}

	private static String message(JavaClass sourceClass, JavaClass targetClass, Dependency dependency) {
		return "%s depends on %s via %s".formatted(
				sourceClass.getName(),
				targetClass.getName(),
				dependency.getDescription()
		);
	}

	private record Domain(String name) {

		static Domain from(String packageName) {
			if (!packageName.startsWith(BASE_PACKAGE + ".")) {
				return null;
			}

			String rest = packageName.substring((BASE_PACKAGE + ".").length());
			String[] parts = rest.split("\\.");
			if (parts.length == 0) {
				return null;
			}
			if ("global".equals(parts[0])) {
				return new Domain("global");
			}
			if (("personal".equals(parts[0]) || "work".equals(parts[0])) && parts.length >= 2) {
				return new Domain(parts[0] + "." + parts[1]);
			}
			return new Domain(parts[0]);
		}

		boolean isGlobal() {
			return "global".equals(name);
		}

		String layerOf(String packageName) {
			String rest = packageName.substring((BASE_PACKAGE + ".").length());
			String[] parts = rest.split("\\.");
			if (parts.length < 2) {
				return "";
			}
			if (name.contains(".")) {
				return parts.length >= 3 && INTERNAL_LAYERS.contains(parts[2]) ? parts[2] : "";
			}
			return INTERNAL_LAYERS.contains(parts[1]) ? parts[1] : "";
		}
	}
}
