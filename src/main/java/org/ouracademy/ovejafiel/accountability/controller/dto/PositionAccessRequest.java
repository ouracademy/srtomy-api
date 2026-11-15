package org.ouracademy.ovejafiel.accountability.controller.dto;

import java.util.List;
import java.util.UUID;

/**
 * What the position gives from now on; both empty or missing makes it a plain label.
 *
 * @param roleIds Ids of access levels, from the roles of the church. Optional.
 * @param permissions Loose permission values, such as {@code members.read}; the full list is in
 *     {@code GET /v1/iam/permissions}. Optional.
 */
public record PositionAccessRequest(List<UUID> roleIds, List<String> permissions) {}
