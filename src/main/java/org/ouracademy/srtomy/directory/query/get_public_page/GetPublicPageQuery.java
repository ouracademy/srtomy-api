package org.ouracademy.srtomy.directory.query.get_public_page;

/** The public page whose address is {@code slug}; nobody has to be signed in. */
public record GetPublicPageQuery(String slug) {}
