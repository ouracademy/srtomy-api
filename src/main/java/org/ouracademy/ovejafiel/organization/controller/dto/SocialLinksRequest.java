package org.ouracademy.ovejafiel.organization.controller.dto;

import jakarta.validation.constraints.Pattern;
import org.ouracademy.ovejafiel.organization.domain.SocialLinks;

/**
 * Optional links; each one, when present, must be an http(s) URL.
 *
 * @param website Address of the website.
 * @param facebookUrl Address of the Facebook page.
 * @param instagramUrl Address of the Instagram profile.
 * @param youtubeUrl Address of the YouTube channel.
 * @param tiktokUrl Address of the TikTok profile.
 */
public record SocialLinksRequest(
    @Pattern(regexp = URL, message = MESSAGE) String website,
    @Pattern(regexp = URL, message = MESSAGE) String facebookUrl,
    @Pattern(regexp = URL, message = MESSAGE) String instagramUrl,
    @Pattern(regexp = URL, message = MESSAGE) String youtubeUrl,
    @Pattern(regexp = URL, message = MESSAGE) String tiktokUrl) {

  static final String URL = "^(\\s*|https?://\\S+)$";
  static final String MESSAGE = "Debe ser un enlace que empiece con http:// o https://";

  public SocialLinks toDomain() {
    return new SocialLinks(
        blankToNull(website),
        blankToNull(facebookUrl),
        blankToNull(instagramUrl),
        blankToNull(youtubeUrl),
        blankToNull(tiktokUrl));
  }

  public static SocialLinks toDomain(SocialLinksRequest request) {
    return request == null ? SocialLinks.NONE : request.toDomain();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
