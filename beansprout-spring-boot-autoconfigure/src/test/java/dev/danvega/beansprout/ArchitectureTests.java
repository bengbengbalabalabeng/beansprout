package dev.danvega.beansprout;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Controller;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * The shape of the library. Read this before you add a class. Agents may not edit this file.
 * Every rule allows an empty match so the rules hold before the code they guard exists.
 */
@AnalyzeClasses(packages = "dev.danvega.beansprout", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

	// --- Core stays plain -------------------------------------------------------------------

	@ArchTest
	static final ArchRule coreKnowsNothingAboutTheWeb = noClasses()
		.that().resideInAnyPackage("dev.danvega.beansprout.content..", "dev.danvega.beansprout.view..")
		.should().dependOnClassesThat().resideInAnyPackage(
				"dev.danvega.beansprout.autoconfigure..", "dev.danvega.beansprout.web..",
				"dev.danvega.beansprout.engine..", "org.springframework.web..", "org.springframework.boot..",
				"jakarta.servlet..")
		.allowEmptyShould(true)
		.because("posts and the view contract are reused by the MCP server and every engine");

	@ArchTest
	static final ArchRule packagesAreFreeOfCycles = slices().matching("dev.danvega.beansprout.(*)..")
		.should().beFreeOfCycles()
		.allowEmptyShould(true);

	// --- Each template engine lives in its own package --------------------------------------

	@ArchTest
	static final ArchRule onlyTheJtePackageTouchesJte = noClasses()
		.that().resideOutsideOfPackage("dev.danvega.beansprout.engine.jte..")
		.should().dependOnClassesThat().resideInAPackage("gg.jte..")
		.allowEmptyShould(true)
		.because("an app without JTE must still start");

	@ArchTest
	static final ArchRule onlyTheThymeleafPackageTouchesThymeleaf = noClasses()
		.that().resideOutsideOfPackage("dev.danvega.beansprout.engine.thymeleaf..")
		.should().dependOnClassesThat().resideInAPackage("org.thymeleaf..")
		.allowEmptyShould(true)
		.because("an app without Thymeleaf must still start");

	@ArchTest
	static final ArchRule onlyTheFreemarkerPackageTouchesFreemarker = noClasses()
		.that().resideOutsideOfPackage("dev.danvega.beansprout.engine.freemarker..")
		.should().dependOnClassesThat().resideInAPackage("freemarker..")
		.allowEmptyShould(true)
		.because("an app without FreeMarker must still start");

	// --- Spring Boot conventions -------------------------------------------------------------

	@ArchTest
	static final ArchRule autoConfigurationsLiveInAutoconfigure = classes()
		.that().areAnnotatedWith(AutoConfiguration.class)
		.should().resideInAPackage("dev.danvega.beansprout.autoconfigure..")
		.andShould().haveSimpleNameEndingWith("AutoConfiguration")
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule propertiesLiveInAutoconfigure = classes()
		.that().areAnnotatedWith(ConfigurationProperties.class)
		.should().resideInAPackage("dev.danvega.beansprout.autoconfigure..")
		.andShould().haveSimpleNameEndingWith("Properties")
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule controllersLiveInWeb = classes()
		.that().areAnnotatedWith(Controller.class).or().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
		.should().resideInAPackage("dev.danvega.beansprout.web..")
		.allowEmptyShould(true);

	// --- General hygiene ---------------------------------------------------------------------

	@ArchTest
	static final ArchRule noStandardStreams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.allowEmptyShould(true);

	@ArchTest
	static final ArchRule noJavaUtilLogging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.allowEmptyShould(true);

	@ArchTest
	static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION.allowEmptyShould(true);

	@ArchTest
	static final ArchRule noGenericExceptions = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.allowEmptyShould(true);

}
