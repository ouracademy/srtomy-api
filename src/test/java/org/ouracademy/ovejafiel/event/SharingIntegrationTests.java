package org.ouracademy.ovejafiel.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.notification.SentEmails;
import org.ouracademy.ovejafiel.notification.TestEmailConfig;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommand;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommandHandler;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommand;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Sharing end to end over the real schema: who can open an event through the tree, a share, the
 * church or the link, and the sign-ups through the link of a course.
 */
@SpringBootTest(classes = OvejafielApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({TestSecurityConfig.class, TestEmailConfig.class})
class SharingIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @Autowired LinkUserToPersonCommandHandler linkUserToPersonCommandHandler;
  @Autowired SentEmails sentEmails;
  @Autowired JdbcTemplate jdbc;

  RestTestClient client;
  String alice;
  String bob;
  String carol;
  String dan;
  String churchId;
  String location;
  String group;
  String eventId;
  String courseId;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/v1").build();
    sentEmails.reset();
    alice = signUp("alice", true);
    bob = signUp("bob", true);
    carol = signUp("carol", true);
    dan = signUp("dan", false);
    churchId =
        read(body(
                alice,
                HttpMethod.POST,
                "/organizations",
                """
                    {"name":"Iglesia","location":{"name":"Sede"},
                     "positions":[{"name":"Pastor","roleIds":["%s"]}],
                     "creator":{"firstName":"Ana","lastName":"Diaz","positionName":"Pastor"}}
                    """
                    .formatted(PredefinedRoles.ADMIN.value())))
            .get("id")
            .asText();
    location = json(alice, "/organizations/" + churchId + "/childs").get(0).get("id").asText();
    group = createGroup("Jóvenes");
    eventId = createEvent("Retiro", false);
    courseId = createEvent("Curso de discipulado", true);
  }

  @Test
  void aNewEventIsRestrictedWithAnUnguessableLink() {
    var sharing = json(alice, "/events/" + eventId + "/sharing");

    assertThat(sharing.get("generalAccess").asText()).isEqualTo("RESTRICTED");
    assertThat(sharing.has("linkRole")).isFalse();
    assertThat(sharing.get("slug").asText()).hasSize(16);
    assertThat(sharing.get("shares")).isEmpty();
    assertThat(sharing.get("isACourse").asBoolean()).isFalse();
    assertThat(json(alice, "/events/" + courseId + "/sharing").get("slug").asText())
        .isNotEqualTo(sharing.get("slug").asText());
  }

  @Test
  void onlyWhoManagesTheEventSeesAndChangesItsSharing() {
    holdAccess(bob, group, PredefinedRoles.COLLABORATOR.value());

    // Bob can read the event through the tree, but sharing is for managers.
    assertThat(status(bob, HttpMethod.GET, "/events/" + eventId)).isEqualTo(200);
    assertThat(status(bob, HttpMethod.GET, "/events/" + eventId + "/sharing")).isEqualTo(403);
    assertThat(
            status(
                bob,
                HttpMethod.PUT,
                "/events/" + eventId + "/sharing",
                sharingBody("ANYONE_WITH_LINK")))
        .isEqualTo(403);
    assertThat(
            status(
                bob,
                HttpMethod.POST,
                "/events/" + eventId + "/shares",
                "{\"email\":\"x@example.org\",\"role\":\"VIEWER\"}"))
        .isEqualTo(403);
    assertThat(status(bob, HttpMethod.GET, "/events/" + eventId + "/registrations")).isEqualTo(403);
    assertThat(status(carol, HttpMethod.GET, "/events/" + eventId)).isEqualTo(403);
  }

  @Test
  void sharingWithAnEmailGivesAccessToWhoseVerifiedEmailItIs() {
    assertThat(status(carol, HttpMethod.GET, "/events/" + eventId)).isEqualTo(403);

    var shared =
        read(
            body(
                alice,
                HttpMethod.POST,
                "/events/" + eventId + "/shares",
                "{\"email\":\"%s\",\"role\":\"VIEWER\"}".formatted(email(carol).toUpperCase())));

    assertThat(shared.get("emailSent").asBoolean()).isTrue();
    assertThat(status(carol, HttpMethod.GET, "/events/" + eventId)).isEqualTo(200);
    // A viewer cannot manage, and neither can they delete.
    assertThat(status(carol, HttpMethod.GET, "/events/" + eventId + "/sharing")).isEqualTo(403);
    assertThat(
            status(carol, HttpMethod.DELETE, "/events/" + eventId, "{\"option\":\"THIS_EVENT\"}"))
        .isEqualTo(403);
    // Someone else with the same event is not affected.
    assertThat(status(bob, HttpMethod.GET, "/events/" + eventId)).isEqualTo(403);

    var shares = json(alice, "/events/" + eventId + "/sharing").get("shares");
    assertThat(shares).hasSize(1);
    assertThat(shares.get(0).get("email").asText()).isEqualTo(email(carol));
    var shareId = shares.get(0).get("id").asText();

    assertThat(status(alice, HttpMethod.DELETE, "/events/" + eventId + "/shares/" + shareId))
        .isEqualTo(204);
    assertThat(status(carol, HttpMethod.GET, "/events/" + eventId)).isEqualTo(403);
  }

  @Test
  void aShareDoesNotApplyToAnUnverifiedEmail() {
    body(
        alice,
        HttpMethod.POST,
        "/events/" + eventId + "/shares",
        "{\"email\":\"%s\",\"role\":\"VIEWER\"}".formatted(email(dan)));

    assertThat(status(dan, HttpMethod.GET, "/events/" + eventId)).isEqualTo(403);
  }

  @Test
  void sharingAgainChangesTheRoleInsteadOfDuplicating() {
    for (var role : new String[] {"VIEWER", "EDITOR"}) {
      body(
          alice,
          HttpMethod.POST,
          "/events/" + eventId + "/shares",
          "{\"email\":\"%s\",\"role\":\"%s\"}".formatted(email(carol), role));
    }

    var shares = json(alice, "/events/" + eventId + "/sharing").get("shares");
    assertThat(shares).hasSize(1);
    assertThat(shares.get(0).get("role").asText()).isEqualTo("EDITOR");
  }

  @Test
  void anEventSharedWithMeShowsUpInMyList() {
    body(
        alice,
        HttpMethod.POST,
        "/events/" + eventId + "/shares",
        "{\"email\":\"%s\",\"role\":\"EDITOR\"}".formatted(email(carol)));
    var slug = json(alice, "/events/" + eventId + "/sharing").get("slug").asText();

    var list = json(carol, "/me/shared-events");

    assertThat(list).hasSize(1);
    assertThat(list.get(0).get("title").asText()).isEqualTo("Retiro");
    assertThat(list.get(0).get("role").asText()).isEqualTo("EDITOR");
    assertThat(list.get(0).get("slug").asText()).isEqualTo(slug);
    assertThat(json(bob, "/me/shared-events")).isEmpty();
    // The same shared page opens for them with the link.
    assertThat(sharedPage(carol, slug).get("title").asText()).isEqualTo("Retiro");
  }

  @Test
  void generalAccessChurchOpensTheEventToWhoBelongsToTheChurch() {
    var other = createGroup("Otro");
    holdAccess(bob, other, PredefinedRoles.MEMBER.value());
    assertThat(status(bob, HttpMethod.GET, "/events/" + eventId)).isEqualTo(403);

    setSharing(eventId, "CHURCH");

    assertThat(status(bob, HttpMethod.GET, "/events/" + eventId)).isEqualTo(200);
    // Not someone outside the church, and not without a session.
    assertThat(status(carol, HttpMethod.GET, "/events/" + eventId)).isEqualTo(403);
    var slug = json(alice, "/events/" + eventId + "/sharing").get("slug").asText();
    assertThat(sharedPage(bob, slug).get("title").asText()).isEqualTo("Retiro");
    client.get().uri("/public/events/" + slug).exchange().expectStatus().isNotFound();
    assertThat(statusOfPublic(carol, slug)).isEqualTo(404);
  }

  @Test
  void generalAccessAnyoneOpensThePublicPageWithoutASession() {
    var slug = json(alice, "/events/" + eventId + "/sharing").get("slug").asText();
    client.get().uri("/public/events/" + slug).exchange().expectStatus().isNotFound();

    setSharing(eventId, "ANYONE_WITH_LINK");

    var page = publicPage(slug);
    assertThat(page.get("title").asText()).isEqualTo("Retiro");
    assertThat(page.get("organizationName").asText()).isEqualTo("Jóvenes");
    assertThat(page.get("canRegister").asBoolean()).isFalse();
    // The page only has what a visitor needs.
    assertThat(page.toString()).doesNotContain("shares", "slug");
    // But the event itself is still protected.
    assertThat(status(carol, HttpMethod.GET, "/events/" + eventId)).isEqualTo(403);
  }

  @Test
  void registrationIsSeparateFromSharingAndAnyEventCanHaveIt() {
    holdAccess(bob, group, PredefinedRoles.COLLABORATOR.value());

    // A plain event (not a course) takes sign-ups too; only managers set it up.
    assertThat(json(alice, "/events/" + eventId + "/registration").get("enabled").asBoolean())
        .isFalse();
    openRegistration(eventId, 20);
    var setup = json(alice, "/events/" + eventId + "/registration");
    assertThat(setup.get("enabled").asBoolean()).isTrue();
    assertThat(setup.get("tickets").get(0).get("quantity").asInt()).isEqualTo(20);
    assertThat(status(bob, HttpMethod.GET, "/events/" + eventId + "/registration")).isEqualTo(403);
    assertThat(
            status(
                bob,
                HttpMethod.PUT,
                "/events/" + eventId + "/registration",
                "{\"requiresAccount\":false," + "\"tickets\":[{\"name\":\"X\",\"priceCents\":0}]}"))
        .isEqualTo(403);
  }

  @Test
  void eachThingYouDoWithTheRegistrationHasItsOwnPermission() {
    openRegistration(courseId, 5);
    var slug = json(alice, "/events/" + courseId + "/sharing").get("slug").asText();
    setSharing(courseId, "ANYONE_WITH_LINK");
    var ticket = ticketOf(courseId);
    var signUp = register(slug, ticket, "Ana", "Paz", "ana@example.org", null);
    var signUpId = json(alice, "/events/" + courseId + "/registrations").get(0).get("id").asText();
    var path = "/events/" + courseId + "/registration";

    // Someone who only sees the sign-ups: the others are refused.
    var reader = role("registrations.read");
    holdAccess(bob, group, reader);
    assertThat(status(bob, HttpMethod.GET, path)).isEqualTo(200);
    assertThat(status(bob, HttpMethod.GET, path + "s")).isEqualTo(200);
    assertThat(status(bob, HttpMethod.PUT, path + "/open", "{\"open\":false}")).isEqualTo(403);
    assertThat(status(bob, HttpMethod.DELETE, path)).isEqualTo(403);
    assertThat(status(bob, HttpMethod.DELETE, path + "s/" + signUpId)).isEqualTo(403);

    // Closing and reopening, and nothing else.
    var closer = role("registrations.close");
    holdAccess(carol, group, closer);
    assertThat(status(carol, HttpMethod.PUT, path + "/open", "{\"open\":false}")).isEqualTo(200);
    assertThat(status(carol, HttpMethod.PUT, path + "/open", "{\"open\":true}")).isEqualTo(200);
    assertThat(status(carol, HttpMethod.GET, path + "s")).isEqualTo(403);
    assertThat(status(carol, HttpMethod.DELETE, path + "s/" + signUpId)).isEqualTo(403);

    assertThat(signUp.get("status").asText()).isEqualTo("CONFIRMED");
  }

  @Test
  void aRegistrationNeedsAtLeastOneTicketAndDistinctNames() {
    var path = "/events/" + eventId + "/registration";
    assertThat(status(alice, HttpMethod.PUT, path, "{\"requiresAccount\":false,\"tickets\":[]}"))
        .isEqualTo(400);
    assertThat(
            status(
                alice,
                HttpMethod.PUT,
                path,
                "{\"requiresAccount\":false,\"tickets\":["
                    + "{\"name\":\"A\",\"priceCents\":0},{\"name\":\"a\",\"priceCents\":0}]}"))
        .isEqualTo(400);
  }

  @Test
  void anyoneSignsUpThroughTheLinkWithATicket() {
    var slug = json(alice, "/events/" + courseId + "/sharing").get("slug").asText();
    setSharing(courseId, "ANYONE_WITH_LINK");
    var ticket = openRegistration(courseId, 10);
    var page = publicPage(slug);
    assertThat(page.get("canRegister").asBoolean()).isTrue();
    assertThat(page.get("registration").get("tickets").get(0).get("remaining").asInt())
        .isEqualTo(10);

    var first = register(slug, ticket, "Lucía", "Mora", "Lucia@Example.org", "999");
    assertThat(first.get("alreadyRegistered").asBoolean()).isFalse();
    assertThat(first.get("status").asText()).isEqualTo("CONFIRMED");
    assertThat(first.get("cancelCode").asText()).isNotBlank();
    assertThat(first.get("ticketCode").asText()).isNotEqualTo(first.get("cancelCode").asText());
    var again = register(slug, ticket, "Lucía", "Mora", "lucia@example.org", null);
    assertThat(again.get("alreadyRegistered").asBoolean()).isTrue();
    assertThat(again.get("cancelCode").asText()).isEqualTo(first.get("cancelCode").asText());

    var registrations = json(alice, "/events/" + courseId + "/registrations");
    assertThat(registrations).hasSize(1);
    assertThat(registrations.get(0).get("email").asText()).isEqualTo("lucia@example.org");
    assertThat(registrations.get(0).get("ticketName").asText()).isEqualTo("General");
    assertThat(registrations.get(0).get("status").asText()).isEqualTo("CONFIRMED");
    assertThat(registrations.get(0).get("hasAccount").asBoolean()).isFalse();
    assertThat(json(alice, "/events/" + courseId + "/sharing").get("registrations").asInt())
        .isEqualTo(1);
    assertThat(publicPage(slug).get("registration").get("tickets").get(0).get("remaining").asInt())
        .isEqualTo(9);
    // The public page never shows who signed up.
    assertThat(publicPage(slug).toString()).doesNotContain("lucia@example.org");

    var id = registrations.get(0).get("id").asText();
    assertThat(status(alice, HttpMethod.DELETE, "/events/" + courseId + "/registrations/" + id))
        .isEqualTo(204);
    assertThat(json(alice, "/events/" + courseId + "/registrations")).isEmpty();
    // Removed, the seat is free again and the same email can sign up.
    assertThat(
            register(slug, ticket, "Lucía", "Mora", "lucia@example.org", null)
                .get("status")
                .asText())
        .isEqualTo("CONFIRMED");
  }

  @Test
  void signingUpNeedsAnOpenLinkAndAnOpenRegistration() {
    var slug = json(alice, "/events/" + courseId + "/sharing").get("slug").asText();
    var ticket = openRegistration(courseId, 5);

    // Restricted: whoever cannot open the event cannot sign up.
    assertThat(registerStatus(slug, ticket, "a@example.org")).isEqualTo(403);
    setSharing(courseId, "ANYONE_WITH_LINK");
    assertThat(registerStatus(slug, ticket, "a@example.org")).isEqualTo(201);
    // Closed registration.
    setRegistration(courseId, false, false, 5);
    assertThat(publicPage(slug).get("canRegister").asBoolean()).isFalse();
    assertThat(registerStatus(slug, ticket, "b@example.org")).isEqualTo(400);
    // An event without registration, and an unknown link.
    assertThat(
            registerStatus(
                json(alice, "/events/" + eventId + "/sharing").get("slug").asText(),
                ticket,
                "c@example.org"))
        .isEqualTo(403);
    assertThat(registerStatus("unknown-link", ticket, "a@example.org")).isEqualTo(404);
  }

  @Test
  void aRegistrationThatNeedsAnAccountRefusesAnonymousVisitors() {
    var slug = json(alice, "/events/" + courseId + "/sharing").get("slug").asText();
    setSharing(courseId, "ANYONE_WITH_LINK");
    var ticket = setRegistration(courseId, true, true, 5);

    assertThat(registerStatus(slug, ticket, "anon@example.org")).isEqualTo(403);
    assertThat(publicPage(slug).get("canRegister").asBoolean()).isFalse();
    var created =
        read(
            bodyAs(
                bob,
                "/public/events/" + slug + "/registrations",
                registrationBody(ticket, email(bob))));
    assertThat(created.get("status").asText()).isEqualTo("CONFIRMED");
    assertThat(
            json(alice, "/events/" + courseId + "/registrations")
                .get(0)
                .get("hasAccount")
                .asBoolean())
        .isTrue();
  }

  @Test
  void aSignUpNeedsATicketANameAndAValidEmail() {
    var slug = json(alice, "/events/" + courseId + "/sharing").get("slug").asText();
    setSharing(courseId, "ANYONE_WITH_LINK");
    var ticket = openRegistration(courseId, 5);

    assertThat(
            statusPublicPost(
                slug,
                "{\"ticketTypeId\":\""
                    + ticket
                    + "\",\"firstName\":\"\",\"lastName\":\"Mora\",\"email\":\"a@example.org\"}"))
        .isEqualTo(400);
    assertThat(
            statusPublicPost(
                slug,
                "{\"ticketTypeId\":\""
                    + ticket
                    + "\",\"firstName\":\"Lu\",\"lastName\":\"Mora\",\"email\":\"not-an-email\"}"))
        .isEqualTo(400);
    assertThat(
            statusPublicPost(
                slug, "{\"firstName\":\"Lu\",\"lastName\":\"Mora\",\"email\":\"a@example.org\"}"))
        .isEqualTo(400);
    assertThat(
            statusPublicPost(
                slug,
                "{\"ticketTypeId\":\"%s\",\"firstName\":\"Lu\",\"lastName\":\"Mora\",\"email\":\"a@example.org\"}"
                    .formatted(java.util.UUID.randomUUID())))
        .isEqualTo(400);
  }

  @Test
  void aChurchLinkOnlyLetsTheChurchSignUp() {
    var other = createGroup("Otro");
    holdAccess(bob, other, PredefinedRoles.MEMBER.value());
    var slug = json(alice, "/events/" + courseId + "/sharing").get("slug").asText();
    setSharing(courseId, "CHURCH");
    var ticket = openRegistration(courseId, 5);

    assertThat(registerStatus(slug, ticket, "anon@example.org")).isEqualTo(403);
    assertThat(
            statusAs(
                carol,
                HttpMethod.POST,
                "/public/events/" + slug + "/registrations",
                registrationBody(ticket, "carol@example.org")))
        .isEqualTo(403);
    var created =
        read(
            bodyAs(
                bob,
                "/public/events/" + slug + "/registrations",
                registrationBody(ticket, email(bob))));
    assertThat(created.get("alreadyRegistered").asBoolean()).isFalse();
    assertThat(
            json(alice, "/events/" + courseId + "/registrations")
                .get(0)
                .get("hasAccount")
                .asBoolean())
        .isTrue();
    assertThat(sharedPage(bob, slug).get("registrationStatus").asText()).isEqualTo("CONFIRMED");
  }

  @Test
  void aFullTicketRefusesTheNextOneUntilASeatIsGivenUp() {
    var slug = json(alice, "/events/" + courseId + "/sharing").get("slug").asText();
    setSharing(courseId, "ANYONE_WITH_LINK");
    var ticket = openRegistration(courseId, 1);

    var first = register(slug, ticket, "Ana", "Paz", "ana@example.org", null);
    // There is no waitlist: when the seats run out nobody else can sign up.
    assertThat(registerStatus(slug, ticket, "luis@example.org")).isEqualTo(400);
    assertThat(publicPage(slug).get("registration").get("soldOut").asBoolean()).isTrue();
    var setup = json(alice, "/events/" + courseId + "/registration");
    assertThat(setup.get("confirmed").asInt()).isEqualTo(1);
    assertThat(setup.has("waitlisted")).isFalse();

    // The first gives up the seat with the code they kept: it is free for the next one.
    assertThat(
            client
                .delete()
                .uri("/public/registrations/" + first.get("cancelCode").asText())
                .exchange()
                .returnResult(String.class)
                .getStatus()
                .value())
        .isEqualTo(204);
    assertThat(publicPage(slug).get("registration").get("soldOut").asBoolean()).isFalse();
    assertThat(registerStatus(slug, ticket, "luis@example.org")).isEqualTo(201);
    var states = new java.util.HashMap<String, String>();
    json(alice, "/events/" + courseId + "/registrations")
        .forEach(r -> states.put(r.get("email").asText(), r.get("status").asText()));
    assertThat(states)
        .containsOnlyKeys("luis@example.org")
        .containsEntry("luis@example.org", "CONFIRMED");
  }

  @Test
  void simultaneousSignUpsNeverExceedTheSeats() throws Exception {
    var slug = json(alice, "/events/" + courseId + "/sharing").get("slug").asText();
    setSharing(courseId, "ANYONE_WITH_LINK");
    var ticket = openRegistration(courseId, 2);

    var pool = java.util.concurrent.Executors.newFixedThreadPool(6);
    try {
      var results = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
      for (var i = 0; i < 6; i++) {
        var email = "p%d@example.org".formatted(i);
        results.add(pool.submit(() -> registerStatus(slug, ticket, email)));
      }
      var created = 0;
      for (var result : results) {
        if (result.get() == 201) {
          created++;
        }
      }
      assertThat(created).isEqualTo(2);
    } finally {
      pool.shutdownNow();
    }
    assertThat(json(alice, "/events/" + courseId + "/registration").get("confirmed").asInt())
        .isEqualTo(2);
  }

  @Test
  void aTicketWithSignUpsCannotBeRemovedAndTheRegistrationMustBeClosedInstead() {
    var slug = json(alice, "/events/" + courseId + "/sharing").get("slug").asText();
    setSharing(courseId, "ANYONE_WITH_LINK");
    var ticket = openRegistration(courseId, 5);
    register(slug, ticket, "Ana", "Paz", "ana@example.org", null);

    assertThat(
            status(
                alice,
                HttpMethod.PUT,
                "/events/" + courseId + "/registration",
                "{\"requiresAccount\":false,\"tickets\":["
                    + "{\"name\":\"Nuevo\",\"priceCents\":0}]}"))
        .isEqualTo(400);
    assertThat(status(alice, HttpMethod.DELETE, "/events/" + courseId + "/registration"))
        .isEqualTo(400);

    var id = json(alice, "/events/" + courseId + "/registrations").get(0).get("id").asText();
    body(alice, HttpMethod.DELETE, "/events/" + courseId + "/registrations/" + id, null);
    assertThat(status(alice, HttpMethod.DELETE, "/events/" + courseId + "/registration"))
        .isEqualTo(204);
    assertThat(json(alice, "/events/" + courseId + "/registration").get("enabled").asBoolean())
        .isFalse();
  }

  @Test
  void anUnknownCancelCodeIsNotFound() {
    assertThat(
            client
                .delete()
                .uri("/public/registrations/not-a-code")
                .exchange()
                .returnResult(String.class)
                .getStatus()
                .value())
        .isEqualTo(404);
  }

  @Test
  void renewingTheLinkRevokesTheOldOne() {
    var oldSlug = json(alice, "/events/" + eventId + "/sharing").get("slug").asText();
    setSharing(eventId, "ANYONE_WITH_LINK");
    publicPage(oldSlug);

    var renewed =
        read(body(alice, HttpMethod.POST, "/events/" + eventId + "/sharing/link/renew", null));

    var newSlug = renewed.get("slug").asText();
    assertThat(newSlug).isNotEqualTo(oldSlug).hasSize(16);
    client.get().uri("/public/events/" + oldSlug).exchange().expectStatus().isNotFound();
    assertThat(publicPage(newSlug).get("title").asText()).isEqualTo("Retiro");
  }

  // --- helpers

  void setSharing(String id, String generalAccess) {
    body(alice, HttpMethod.PUT, "/events/" + id + "/sharing", sharingBody(generalAccess));
  }

  static String sharingBody(String generalAccess) {
    return "{\"generalAccess\":\"%s\"}".formatted(generalAccess);
  }

  /** Opens the registration of the event with one ticket "General" and returns its id. */
  String openRegistration(String id, Integer quantity) {
    return setRegistration(id, true, false, quantity);
  }

  /** Sets the registration with one ticket "General", open or closed, and returns the ticket id. */
  String setRegistration(String id, boolean open, boolean requiresAccount, Integer quantity) {
    var tickets = json(alice, "/events/" + id + "/registration").get("tickets");
    var existing = tickets.isEmpty() ? "" : "\"id\":\"" + tickets.get(0).get("id").asText() + "\",";
    var ticket =
        read(body(
                alice,
                HttpMethod.PUT,
                "/events/" + id + "/registration",
                ("{\"requiresAccount\":%s,"
                        + "\"tickets\":[{%s\"name\":\"General\",\"priceCents\":0,"
                        + "\"currency\":\"PEN\",\"quantity\":%s}]}")
                    .formatted(requiresAccount, existing, quantity)))
            .get("tickets")
            .get(0)
            .get("id")
            .asText();
    body(
        alice,
        HttpMethod.PUT,
        "/events/" + id + "/registration/open",
        "{\"open\":%s}".formatted(open));
    return ticket;
  }

  String ticketOf(String id) {
    return json(alice, "/events/" + id + "/registration").get("tickets").get(0).get("id").asText();
  }

  String registrationBody(String ticketId, String email) {
    return "{\"ticketTypeId\":\"%s\",\"firstName\":\"Nombre\",\"lastName\":\"Apellido\",\"email\":\"%s\"}"
        .formatted(ticketId, email);
  }

  JsonNode register(
      String slug, String ticketId, String first, String last, String email, String phone) {
    return read(
        client
            .post()
            .uri("/public/events/" + slug + "/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                "{\"ticketTypeId\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"email\":\"%s\"%s}"
                    .formatted(
                        ticketId,
                        first,
                        last,
                        email,
                        phone == null ? "" : ",\"phoneNumber\":\"" + phone + "\""))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody());
  }

  int registerStatus(String slug, String ticketId, String email) {
    return statusPublicPost(slug, registrationBody(ticketId, email));
  }

  int statusPublicPost(String slug, String json) {
    return client
        .post()
        .uri("/public/events/" + slug + "/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .body(json)
        .exchange()
        .returnResult(String.class)
        .getStatus()
        .value();
  }

  JsonNode publicPage(String slug) {
    return read(
        client
            .get()
            .uri("/public/events/" + slug)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody());
  }

  JsonNode sharedPage(String subject, String slug) {
    return json(subject, "/public/events/" + slug);
  }

  int statusOfPublic(String subject, String slug) {
    return status(subject, HttpMethod.GET, "/public/events/" + slug);
  }

  String createGroup(String name) {
    return read(body(
            alice,
            HttpMethod.POST,
            "/organizations/locations/childs",
            "{\"name\":\"%s\",\"type\":\"GROUP\",\"parentId\":\"%s\"}".formatted(name, location)))
        .get("id")
        .asText();
  }

  String createEvent(String name, boolean course) {
    return read(body(
            alice,
            HttpMethod.POST,
            "/events",
            ("{\"name\":\"%s\",\"startTime\":\"2030-01-05T10:00:00-05:00\","
                    + "\"endTime\":\"2030-01-05T12:00:00-05:00\",\"organizationId\":\"%s\","
                    + "\"isACourse\":%s}")
                .formatted(name, group, course)))
        .asText();
  }

  /** Gives {@code subject} a position with {@code roleId} on {@code nodeId}, through a person. */
  final java.util.List<UUID> createdRoles = new java.util.ArrayList<>();

  /** The roles of a test are left out of the lists of the next ones. */
  @org.junit.jupiter.api.AfterEach
  void dropRoles() {
    createdRoles.forEach(
        id -> jdbc.update("UPDATE iam_role SET deleted_time = now() WHERE id = ?", id));
    createdRoles.clear();
  }

  /** A role with just these permissions (and the basics to see the node and its events). */
  UUID role(String... permissions) {
    var id = UUID.randomUUID();
    createdRoles.add(id);
    var all = new java.util.ArrayList<>(java.util.List.of("org.read", "events.read"));
    all.addAll(java.util.List.of(permissions));
    jdbc.update(
        "INSERT INTO iam_role (id, name, description, permissions, create_date, last_modified_date)"
            + " VALUES (?, ?, '', ?, now(), now())",
        id,
        "Rol " + id,
        all.toArray(new String[0]));
    return id;
  }

  void holdAccess(String subject, String nodeId, Object roleId) {
    var position =
        read(body(
                alice,
                HttpMethod.POST,
                "/organizations/" + churchId + "/positions",
                "{\"name\":\"Cargo %s\",\"roleIds\":[\"%s\"]}"
                    .formatted(UUID.randomUUID(), roleId)))
            .get("id")
            .asText();
    var personId =
        read(body(
                alice,
                HttpMethod.POST,
                "/people",
                ("{\"firstName\":\"Persona\",\"lastName\":\"Cargo\",\"organizationId\":\"%s\","
                        + "\"positionIds\":[\"%s\"]}")
                    .formatted(nodeId, position)))
            .asText();
    var user =
        syncCurrentUserCommandHandler
            .handle(new SyncCurrentUserCommand(subject, email(subject), true))
            .user();
    linkUserToPersonCommandHandler.handle(
        new LinkUserToPersonCommand(
            user.id(),
            new PartyId(UUID.fromString(personId)),
            new PartyId(UUID.fromString(churchId))));
  }

  String signUp(String name, boolean verified) {
    var subject = "auth0|" + name + "-" + UUID.randomUUID();
    syncCurrentUserCommandHandler.handle(
        new SyncCurrentUserCommand(subject, email(subject), verified));
    return subject;
  }

  static String email(String subject) {
    return subject.replace("auth0|", "") + "@example.org";
  }

  int status(String subject, HttpMethod method, String uri) {
    return status(subject, method, uri, null);
  }

  int status(String subject, HttpMethod method, String uri, String body) {
    return statusAs(subject, method, uri, body);
  }

  int statusAs(String subject, HttpMethod method, String uri, String body) {
    var request = client.method(method).uri(uri).headers(h -> h.setBearerAuth(token(subject)));
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).body(body);
    }
    return request.exchange().returnResult(String.class).getStatus().value();
  }

  String bodyAs(String subject, String uri, String body) {
    return body(subject, HttpMethod.POST, uri, body);
  }

  String body(String subject, HttpMethod method, String uri, String body) {
    var request = client.method(method).uri(uri).headers(h -> h.setBearerAuth(token(subject)));
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).body(body);
    }
    return request
        .exchange()
        .expectStatus()
        .is2xxSuccessful()
        .expectBody(String.class)
        .returnResult()
        .getResponseBody();
  }

  JsonNode json(String subject, String uri) {
    return read(
        client
            .get()
            .uri(uri)
            .headers(h -> h.setBearerAuth(token(subject)))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody());
  }

  static JsonNode read(String body) {
    try {
      return JSON.readTree(body);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  String token(String subject) {
    return TestSecurityConfig.validToken(jwtEncoder, subject);
  }
}
