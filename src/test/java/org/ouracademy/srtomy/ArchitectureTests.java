package org.ouracademy.srtomy;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
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
import java.util.List;
import java.util.Map;

/**
 * Executable source of truth for the package-by-module structure: module isolation and the
 * Command/Query handler naming conventions.
 *
 * <p>Every rule uses {@code allowEmptyShould(true)} because the migration to modules is
 * incremental: most module packages (and every handler) do not exist yet, and a rule must not fail
 * just because there is nothing to check.
 */
@AnalyzeClasses(packages = "org.ouracademy.srtomy")
class ArchitectureTests {

  private static final String BASE_PACKAGE = "org.ouracademy.srtomy";

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
          "invitation");

  /** Packages of a module that no other module may depend on. */
  private static final List<String> RESTRICTED_SUB_PACKAGES =
      List.of("repository", "gateway", "controller");

  /**
   * Known, temporary violations of module isolation, per module, by fully qualified class name.
   * Each entry must be removed by the migration step named next to it; do not add new ones. Empty
   * now that every module has been migrated.
   */
  private static final Map<String, List<String>> PENDING_MIGRATION_EXCEPTIONS = Map.of();

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
   *   (a) Inheritance: any module may extend org.ouracademy.srtomy.shared.domain.* (DomainObject,
   *       EntityId, ...) or, once it exists, org.ouracademy.srtomy.party.domain.Party (Person and
   *       Organization are Party subtypes). Both live under ..domain.., so they are reachable.
   *   (b) JPA relationships: @ManyToOne/@OneToOne/@OneToMany fields may be typed against another
   *       module's concrete @Entity class, because JPA requires the concrete entity type. Entities
   *       live under <module>.domain.., so this is allowed too. What stays forbidden is reaching
   *       that module's repository/gateway/controller.
   */
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
