package org.ouracademy.srtomy.organization.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;

/**
 * A node of a church's tree: the church itself (root), its locations, and nested groups and
 * ministries. Access roles granted on a node apply to all of its descendants.
 */
@Entity
@Getter
public class Organization extends Party {

  @Enumerated(EnumType.STRING)
  Type type;

  @ManyToOne(fetch = FetchType.LAZY)
  Organization parent;

  @Embedded SocialLinks socialLinks;

  public enum Type {
    CHURCH,
    LOCATION,
    GROUP,
    MINISTRY;

    /** Whether a node of this type may hang from {@code parent} ({@code null} = no parent). */
    public boolean canHaveParent(Type parent) {
      return switch (this) {
        case CHURCH -> parent == null;
        case LOCATION -> parent == CHURCH;
        case GROUP, MINISTRY -> parent == LOCATION || parent == GROUP || parent == MINISTRY;
      };
    }
  }

  protected Organization() {
    // jpa only
  }

  public Organization(String name, Type type) {
    this.name = name;
    this.type = type;
  }

  /**
   * Creates a node under {@code parent}, enforcing the tree shape: church at the root, locations
   * under a church, and groups/ministries under a location or another group/ministry.
   */
  public static Organization createUnder(String name, Type type, Organization parent) {
    var parentType = parent == null ? null : parent.getType();
    if (!type.canHaveParent(parentType)) {
      throw new DomainValidationException(
          "Un nodo de tipo " + type + " no puede estar dentro de " + parentType);
    }
    var organization = new Organization(name, type);
    organization.parent = parent;
    return organization;
  }

  /** Only groups and ministries can be deleted; churches and locations cannot. */
  public boolean isDeletable() {
    return isGroupOrMinistry();
  }

  public boolean isGroupOrMinistry() {
    return type == Type.GROUP || type == Type.MINISTRY;
  }

  /** Switches between group and ministry; the node keeps its place in the tree. */
  public void changeType(Type type) {
    if (!isGroupOrMinistry() || (type != Type.GROUP && type != Type.MINISTRY)) {
      throw new DomainValidationException("Solo se puede cambiar entre grupo y ministerio");
    }
    this.type = type;
  }

  public SocialLinks getSocialLinks() {
    return socialLinks == null ? SocialLinks.NONE : socialLinks;
  }

  public void setSocialLinks(SocialLinks socialLinks) {
    this.socialLinks = socialLinks;
  }
}
