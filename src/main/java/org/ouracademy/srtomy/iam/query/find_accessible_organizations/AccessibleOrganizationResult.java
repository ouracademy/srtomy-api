package org.ouracademy.srtomy.iam.query.find_accessible_organizations;

import java.util.List;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * A node of the user's trees. {@code accessible} is false for ancestors that are only shown as a
 * breadcrumb; {@code roles} are the role names bound directly on this node.
 */
public record AccessibleOrganizationResult(
    PartyId id,
    String name,
    Organization.Type type,
    PartyId parentId,
    boolean accessible,
    List<String> roles) {}
