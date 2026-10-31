package org.ouracademy.ovejafiel.whatsapp.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The codes waiting to be sent, the numbers already proven and the wrong tries. Plain SQL: these
 * are small tables with no behaviour of their own.
 */
@Repository
public class WhatsAppLinkRepository {

  public record Code(UUID userId, UUID churchId) {}

  public record Identity(UUID userId, LocalDateTime verifiedAt, String phoneDigits) {}

  private final JdbcClient jdbc;

  WhatsAppLinkRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** One code per user: a new one replaces the old. */
  public void saveCode(UUID userId, UUID churchId, String hash, LocalDateTime expiresAt) {
    jdbc.sql(
            """
            insert into whatsapp_link_code(user_id, church_id, code_hash, expires_at,
                                           create_date, last_modified_date)
            values (:user, :church, :hash, :expires, now(), now())
            on conflict (user_id) do update
              set church_id = :church, code_hash = :hash, expires_at = :expires,
                  last_modified_date = now()
            """)
        .param("user", userId)
        .param("church", churchId)
        .param("hash", hash)
        .param("expires", expiresAt)
        .update();
  }

  /** The code with that hash if it is for that church and has not expired. */
  public Optional<Code> findValidCode(String hash, UUID churchId, LocalDateTime now) {
    return jdbc.sql(
            "select user_id, church_id from whatsapp_link_code"
                + " where code_hash = :hash and church_id = :church and expires_at > :now")
        .param("hash", hash)
        .param("church", churchId)
        .param("now", now)
        .query((rs, i) -> new Code(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)))
        .optional();
  }

  public void deleteCode(UUID userId) {
    jdbc.sql("delete from whatsapp_link_code where user_id = :user").param("user", userId).update();
  }

  /** The number moves to this user if another account had it in that church. */
  public void saveIdentity(UUID userId, UUID churchId, String phoneDigits, LocalDateTime now) {
    jdbc.sql(
            "delete from whatsapp_identity where church_id = :church and phone_digits = :phone"
                + " and user_id <> :user")
        .param("church", churchId)
        .param("phone", phoneDigits)
        .param("user", userId)
        .update();
    jdbc.sql(
            """
            insert into whatsapp_identity(user_id, church_id, phone_digits, verified_at)
            values (:user, :church, :phone, :now)
            on conflict (user_id, church_id) do update
              set phone_digits = :phone, verified_at = :now
            """)
        .param("user", userId)
        .param("church", churchId)
        .param("phone", phoneDigits)
        .param("now", now)
        .update();
  }

  public Optional<UUID> findUserByPhone(UUID churchId, String phoneDigits) {
    return jdbc.sql(
            "select user_id from whatsapp_identity"
                + " where church_id = :church and phone_digits = :phone")
        .param("church", churchId)
        .param("phone", phoneDigits)
        .query(UUID.class)
        .optional();
  }

  public Optional<Identity> findIdentity(UUID userId, UUID churchId) {
    return jdbc.sql(
            "select user_id, verified_at, phone_digits from whatsapp_identity"
                + " where user_id = :user and church_id = :church")
        .param("user", userId)
        .param("church", churchId)
        .query(
            (rs, i) ->
                new Identity(
                    rs.getObject(1, UUID.class),
                    rs.getTimestamp(2).toLocalDateTime(),
                    rs.getString(3)))
        .optional();
  }

  public boolean deleteIdentity(UUID userId, UUID churchId) {
    return jdbc.sql("delete from whatsapp_identity where user_id = :user and church_id = :church")
            .param("user", userId)
            .param("church", churchId)
            .update()
        > 0;
  }

  public void recordFailure(String phoneDigits, LocalDateTime now) {
    jdbc.sql("insert into whatsapp_link_attempt(phone_digits, attempted_at) values (:phone, :now)")
        .param("phone", phoneDigits)
        .param("now", now)
        .update();
  }

  public int countFailuresSince(String phoneDigits, LocalDateTime since) {
    return jdbc.sql(
            "select count(*) from whatsapp_link_attempt"
                + " where phone_digits = :phone and attempted_at > :since")
        .param("phone", phoneDigits)
        .param("since", since)
        .query(Integer.class)
        .single();
  }
}
