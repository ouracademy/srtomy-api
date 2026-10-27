package org.ouracademy.ovejafiel.iam.query.authorize;

import org.ouracademy.ovejafiel.iam.domain.Permission;

/** Proof that the permission was granted. */
public record AuthorizeResult(Permission permission) {}
