package org.ouracademy.srtomy.iam.query.authorize;

import org.ouracademy.srtomy.iam.domain.Permission;

/** Proof that the permission was granted. */
public record AuthorizeResult(Permission permission) {}
