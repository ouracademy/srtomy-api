package org.ouracademy.srtomy.organization.domain;

import jakarta.persistence.Embeddable;

/** Optional public links of an organization; every field may be null. */
@Embeddable
public record SocialLinks(
    String website, String facebookUrl, String instagramUrl, String youtubeUrl, String tiktokUrl) {

  public static final SocialLinks NONE = new SocialLinks(null, null, null, null, null);
}
