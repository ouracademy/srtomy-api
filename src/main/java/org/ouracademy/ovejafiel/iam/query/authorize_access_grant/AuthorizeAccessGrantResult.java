package org.ouracademy.ovejafiel.iam.query.authorize_access_grant;

import java.util.List;

/** The effective permissions that were authorized (distinct, sorted). */
public record AuthorizeAccessGrantResult(List<String> permissions) {}
