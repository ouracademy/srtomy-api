package org.ouracademy.srtomy.user.query.find_user_by_email;

/** Looks up a user by email; only users whose email the identity provider verified match. */
public record FindUserByEmailQuery(String email) {}
