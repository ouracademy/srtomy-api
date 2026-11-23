package org.ouracademy.ovejafiel;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.util.List;
import java.util.Map;

/**
 * Executable source of truth for docs/code-style.md: module isolation, the layers inside a module
 * (domain, command/query, repository/gateway, controller) and the Command/Query naming conventions.
 *
 * <p>Every rule uses {@code allowEmptyShould(true)} because the migration to modules is
 * incremental: most module packages (and every handler) do not exist yet, and a rule must not fail
 * just because there is nothing to check.
 */
@AnalyzeClasses(packages = "org.ouracademy.ovejafiel")
class ArchitectureTests {

  private static final String BASE_PACKAGE = "org.ouracademy.ovejafiel";

  private static final String SPRING_BOOT_TEST =
      "org.springframework.boot.test.context.SpringBootTest";

  /**
   * Feature modules whose internals ({@code repository}, {@code gateway}, {@code controller}) are
   * private to the module. Adding a module only requires adding its name here.
   */
  private static final List<String> MODULES =
      List.of(
          "party",
          "person",
          "organization",
          "accountability",
          "event",
          "attendance",
          "complaint",
          "user",
          "iam",
          "notification",
          "invitation",
          "directory",
          "outbox",
          "whatsapp");

  /** Packages of a module that no other module may depend on. */
  private static final List<String> RESTRICTED_SUB_PACKAGES =
      List.of("repository", "gateway", "controller");

  /**
   * Known, temporary violations of module isolation, per module, by fully qualified class name.
   * Each entry must be removed by the migration step named next to it; do not add new ones. Empty
   * now that every module has been migrated.
   */
  private static final Map<String, List<String>> PENDING_MIGRATION_EXCEPTIONS = Map.of();

  /**
   * Entities that still use a raw id instead of their own {@code EntityId} record
   * (docs/code-style.md, "Entity ids"). Changing them touches their mapping and migrations; see
   * item 41 of docs/tech-debt-and-future-work.md. Do not add new ones.
   */
  private static final List<String> ENTITIES_WITH_RAW_IDS =
      List.of(
          "org.ouracademy.ovejafiel.accountability.domain.MemberStateEvent",
          "org.ouracademy.ovejafiel.event.domain.EventNotice",
          "org.ouracademy.ovejafiel.event.domain.SubEventCheckIn");

  /**
   * The one class of {@code shared} allowed to know the modules: Hibernate needs every {@code
   * EntityId} record registered in a single {@code TypeContributor}.
   */
  private static final String ENTITY_ID_REGISTRY =
      BASE_PACKAGE + ".shared.domain.EntityIdTypeContributor";

  private static final DescribedPredicate<JavaClass> PRODUCTION_CODE =
      DescribedPredicate.describe(
          "are production code",
          javaClass ->
              javaClass
                  .getSource()
                  .map(source -> !source.getUri().toString().contains("/test-classes/"))
                  .orElse(true));

  private static final DescribedPredicate<JavaClass> HANDLER_INTERFACE =
      JavaClass.Predicates.simpleNameEndingWith("CommandHandler")
          .or(JavaClass.Predicates.simpleNameEndingWith("QueryHandler"));

  /*
   * Module isolation: no class outside <module>.. may depend on <module>.repository..,
   * <module>.gateway.. or <module>.controller.. Modules talk to each other through command/query
   * handlers instead.
   *
   * <module>.domain.. is intentionally NOT in the restricted list, which is what makes the two
   * documented exceptions legal without any whitelist:
   *   (a) Inheritance: any module may extend org.ouracademy.ovejafiel.shared.domain.* (DomainObject,
   *       EntityId, ...) or, once it exists, org.ouracademy.ovejafiel.party.domain.Party (Person and
   *       Organization are Party subtypes). Both live under ..domain.., so they are reachable.
   *   (b) JPA relationships: @ManyToOne/@OneToOne/@OneToMany fields may be typed against another
   *       module's concrete @Entity class, because JPA requires the concrete entity type. Entities
   *       live under <module>.domain.., so this is allowed too. What stays forbidden is reaching
   *       that module's repository/gateway/controller.
   */
  /*
   * Work on a schedule is Cloud Scheduler's job, calling an internal endpoint (see
   * docs/code-style.md, "Trabajo programado y sistemas externos"). An in-process scheduler would not
   * run when Cloud Run has no instance up, and would run once per instance when it has several.
   */
  @ArchTest
  static final ArchRule no_in_process_scheduling =
      CompositeArchRule.of(
              methods()
                  .should()
                  .notBeAnnotatedWith(org.springframework.scheduling.annotation.Scheduled.class)
                  .allowEmptyShould(true))
          .and(
              noClasses()
                  .should()
                  .beAnnotatedWith(org.springframework.scheduling.annotation.EnableScheduling.class)
                  .allowEmptyShould(true))
          .because("scheduled work goes through Cloud Scheduler calling /v1/internal/**");

  @ArchTest
  static final ArchRule modules_only_expose_their_domain =
      CompositeArchRule.of(
          MODULES.stream().map(ArchitectureTests::moduleInternalsArePrivate).toList());

  @ArchTest
  static final ArchRule no_use_case_or_interactor_naming =
      noClasses()
          .should()
          .haveNameMatching(".*UseCase.*|.*Interactor.*")
          .because("application logic is named Command/Query + Handler, not UseCase/Interactor")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule handlers_declare_a_single_handle_method =
      classes()
          .that(HANDLER_INTERFACE)
          .should(declareExactlyOnePublicMethodNamedHandle())
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule handle_methods_return_a_result =
      methods()
          .that()
          .haveName("handle")
          .and()
          .areDeclaredInClassesThat(HANDLER_INTERFACE)
          .should(returnATypeNamedResultThatIsNotAnEntity())
          .allowEmptyShould(true);

  /*
   * Layers inside a module (docs/code-style.md, "Estructura interna de un módulo"). The domain is
   * the core and knows nothing else; command/query handlers do not know HTTP; repositories and
   * gateways only talk to the database or the outside; controllers reach data only through
   * handlers.
   */
  @ArchTest
  static final ArchRule domain_depends_on_no_other_layer =
      noClasses()
          .that(PRODUCTION_CODE)
          .and()
          .resideInAPackage(BASE_PACKAGE + ".*.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..command..", "..query..", "..repository..", "..gateway..", "..controller..")
          .because("the domain is the core of a module; the other layers depend on it");

  @ArchTest
  static final ArchRule handlers_do_not_know_http =
      noClasses()
          .that(PRODUCTION_CODE)
          .and()
          .resideInAnyPackage("..command..", "..query..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..controller..")
          .because(
              "HTTP requests and responses stay in the controller; handlers take a Command/Query");

  @ArchTest
  static final ArchRule repositories_only_talk_to_the_database =
      noClasses()
          .that(PRODUCTION_CODE)
          .and()
          .resideInAPackage("..repository..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..command..", "..query..", "..controller..", "..gateway..")
          .because("handlers use repositories, never the other way round");

  @ArchTest
  static final ArchRule controllers_go_through_handlers =
      noClasses()
          .that(PRODUCTION_CODE)
          .and()
          .resideInAPackage("..controller..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..repository..", "..gateway..")
          .because("repositories and gateways are used by the handlers of their own module");

  /*
   * shared is the base every module imports (docs/code-style.md, "Lo compartido"), so it must not
   * import them back. The only exception is the Hibernate registry of entity ids.
   */
  @ArchTest
  static final ArchRule shared_does_not_depend_on_modules =
      noClasses()
          .that(PRODUCTION_CODE)
          .and()
          .resideInAPackage(BASE_PACKAGE + ".shared..")
          .and()
          .doNotHaveFullyQualifiedName(ENTITY_ID_REGISTRY)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              MODULES.stream().map(m -> BASE_PACKAGE + "." + m + "..").toArray(String[]::new))
          .because("modules depend on shared, not the other way round");

  /*
   * Calls to other systems are encapsulated in a module's gateway/ (docs/code-style.md), and reach
   * them through the outbox when they come from a command.
   */
  @ArchTest
  static final ArchRule http_clients_only_in_gateways =
      noClasses()
          .that(PRODUCTION_CODE)
          .and()
          .resideOutsideOfPackage("..gateway..")
          .should()
          .dependOnClassesThat()
          .haveFullyQualifiedName("org.springframework.web.client.RestClient")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("org.springframework.web.client.RestTemplate")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("org.springframework.web.reactive.function.client.WebClient")
          .orShould()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.net.http.HttpClient")
          .because("calls to external systems live in a module's gateway/");

  /* Where each kind of class lives. */
  @ArchTest
  static final ArchRule controllers_live_in_controller_packages =
      classes()
          .that(PRODUCTION_CODE)
          .and()
          .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
          .should()
          .resideInAPackage("..controller..")
          .andShould()
          .haveSimpleNameEndingWith("Controller");

  @ArchTest
  static final ArchRule spring_data_repositories_live_in_repository_packages =
      classes()
          .that()
          .areInterfaces()
          .and()
          .areAssignableTo("org.springframework.data.repository.Repository")
          .should()
          .resideInAPackage("..repository..");

  @ArchTest
  static final ArchRule entities_live_in_domain_packages =
      classes()
          .that()
          .areAnnotatedWith("jakarta.persistence.Entity")
          .should()
          .resideInAPackage("..domain..");

  /*
   * "Los *HandlerImpl siguen siendo package-private": a module (or another action) depends on the
   * handler interface, never on its implementation.
   */
  @ArchTest
  static final ArchRule handler_implementations_are_package_private =
      classes()
          .that(PRODUCTION_CODE)
          .and()
          .haveSimpleNameEndingWith("HandlerImpl")
          .should()
          .notBePublic()
          .allowEmptyShould(true);

  /* One folder per action, named after the Command/Query in snake_case. */
  @ArchTest
  static final ArchRule commands_live_in_their_action_package =
      classes()
          .that(PRODUCTION_CODE)
          .and()
          .haveSimpleNameEndingWith("Command")
          .and()
          .resideInAPackage("..command..")
          .and()
          .areNotInterfaces()
          .should(liveInTheirActionPackage("command"))
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule queries_live_in_their_action_package =
      classes()
          .that(PRODUCTION_CODE)
          .and()
          .haveSimpleNameEndingWith("Query")
          .and()
          .resideInAPackage("..query..")
          .and()
          .areNotInterfaces()
          .should(liveInTheirActionPackage("query"))
          .allowEmptyShould(true);

  /* Every entity has its own typed id (docs/code-style.md, "Entity ids"). */
  @ArchTest
  static final ArchRule entity_ids_are_typed =
      fields()
          .that()
          .areAnnotatedWith("jakarta.persistence.Id")
          .and(
              DescribedPredicate.describe(
                  "are not in " + ENTITIES_WITH_RAW_IDS,
                  (JavaField field) -> !ENTITIES_WITH_RAW_IDS.contains(field.getOwner().getName())))
          .should()
          .haveRawType(
              DescribedPredicate.describe(
                  "a record implementing EntityId",
                  type -> type.isAssignableTo(BASE_PACKAGE + ".shared.domain.EntityId")))
          .allowEmptyShould(true);

  /* Spring beans get their collaborators through the constructor. */
  @ArchTest
  static final ArchRule no_autowired_fields =
      noFields()
          .that()
          .areDeclaredInClassesThat(PRODUCTION_CODE)
          .should()
          .beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
          .orShould()
          .beAnnotatedWith("jakarta.inject.Inject")
          .because("dependencies are injected through the constructor")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule no_standard_streams =
      noClasses()
          .that(PRODUCTION_CODE)
          .should(GeneralCodingRules.ACCESS_STANDARD_STREAMS)
          .because("use the logger: Cloud Logging only structures what goes through it");

  /*
   * The suffix of a test class is its level of the test pyramid (constitution, principle II), and
   * CI picks the classes of each stage by it: *Tests first, then *IntegrationTests, then
   * *SmokeTests. Only the last two may load the full context, and both must.
   */
  @ArchTest
  static final ArchRule full_context_tests_say_so_in_their_name =
      CompositeArchRule.of(
              classes()
                  .that()
                  .areAnnotatedWith(SPRING_BOOT_TEST)
                  .should()
                  .haveSimpleNameEndingWith("IntegrationTests")
                  .orShould()
                  .haveSimpleNameEndingWith("SmokeTests")
                  .allowEmptyShould(true))
          .and(
              classes()
                  .that()
                  .haveSimpleNameEndingWith("IntegrationTests")
                  .or()
                  .haveSimpleNameEndingWith("SmokeTests")
                  .should()
                  .beAnnotatedWith(SPRING_BOOT_TEST)
                  .allowEmptyShould(true))
          .because(
              "CI runs each level of the test pyramid in its own stage, chosen by the suffix"
                  + " (see the test-pyramid skill)");

  @ArchTest
  static final ArchRule no_java_util_logging =
      GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

  private static ArchRule moduleInternalsArePrivate(String module) {
    String modulePackage = BASE_PACKAGE + "." + module;
    String[] restrictedPackages =
        RESTRICTED_SUB_PACKAGES.stream()
            .map(sub -> modulePackage + "." + sub + "..")
            .toArray(String[]::new);
    List<String> exceptions = PENDING_MIGRATION_EXCEPTIONS.getOrDefault(module, List.of());
    DescribedPredicate<JavaClass> pendingMigration =
        DescribedPredicate.describe(
            "pending migration " + exceptions,
            javaClass -> exceptions.contains(javaClass.getName()));
    return noClasses()
        .that()
        .resideOutsideOfPackage(modulePackage + "..")
        .and(DescribedPredicate.not(pendingMigration))
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(restrictedPackages)
        .because(
            "other modules must use the "
                + module
                + " module's command/query handlers, not its repository/gateway/controller")
        .allowEmptyShould(true);
  }

  /**
   * {@code CreateChurchCommand} must live in {@code ..command.create_church}. Brand names written
   * as one word in package names ({@code whatsapp}) are lowered before splitting the words.
   */
  private static ArchCondition<JavaClass> liveInTheirActionPackage(String kind) {
    String suffix = kind.equals("command") ? "Command" : "Query";
    return new ArchCondition<>("live in " + kind + "/<action in snake_case>/") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        String action =
            javaClass
                .getSimpleName()
                .substring(0, javaClass.getSimpleName().length() - suffix.length())
                .replace("WhatsApp", "Whatsapp");
        String expected =
            "." + kind + "." + action.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
        boolean satisfied = javaClass.getPackageName().endsWith(expected);
        String message =
            String.format(
                "%s should live in a package ending with %s", javaClass.getName(), expected);
        events.add(new SimpleConditionEvent(javaClass, satisfied, message));
      }
    };
  }

  private static ArchCondition<JavaClass> declareExactlyOnePublicMethodNamedHandle() {
    return new ArchCondition<>("declare exactly one public method, named 'handle'") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        // getMethods() only returns the class's own declared methods (no Object/inherited ones).
        // Default and static methods are not part of the handler's contract.
        List<JavaMethod> contractMethods =
            javaClass.getMethods().stream()
                .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                .filter(method -> method.getModifiers().contains(JavaModifier.ABSTRACT))
                .filter(method -> !method.getModifiers().contains(JavaModifier.STATIC))
                .toList();
        boolean satisfied =
            contractMethods.size() == 1 && contractMethods.getFirst().getName().equals("handle");
        String message =
            String.format(
                "%s declares %d public method(s) %s; expected exactly one, named 'handle'",
                javaClass.getName(),
                contractMethods.size(),
                contractMethods.stream().map(JavaMethod::getName).toList());
        events.add(new SimpleConditionEvent(javaClass, satisfied, message));
      }
    };
  }

  private static ArchCondition<JavaMethod> returnATypeNamedResultThatIsNotAnEntity() {
    return new ArchCondition<>(
        "return a type whose simple name ends with 'Result' (not an @Entity)") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        JavaClass returnType = unwrapContainer(method.getReturnType());
        boolean namedResult = returnType.getSimpleName().endsWith("Result");
        boolean isEntity = returnType.isAnnotatedWith("jakarta.persistence.Entity");
        String message =
            String.format(
                "%s returns %s; handle() must return a *Result type that is not an @Entity",
                method.getFullName(), returnType.getName());
        events.add(new SimpleConditionEvent(method, namedResult && !isEntity, message));
      }
    };
  }

  /**
   * Query handlers may wrap their result in an {@link java.util.Optional} or a {@link
   * java.util.Collection} (e.g. {@code Optional<GetPartyByIdResult>}, {@code
   * List<AccountabilityResult>}); the rule then applies to the element type.
   */
  private static JavaClass unwrapContainer(JavaType type) {
    JavaClass raw = type.toErasure();
    boolean isContainer =
        raw.isEquivalentTo(java.util.Optional.class)
            || raw.isAssignableTo(java.util.Collection.class);
    if (isContainer
        && type instanceof JavaParameterizedType parameterized
        && parameterized.getActualTypeArguments().size() == 1) {
      return parameterized.getActualTypeArguments().getFirst().toErasure();
    }
    return raw;
  }
}
