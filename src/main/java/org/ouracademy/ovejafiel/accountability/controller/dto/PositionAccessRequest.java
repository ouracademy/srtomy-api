package org.ouracademy.ovejafiel.accountability.controller.dto;

import java.util.List;
import java.util.UUID;

/** What the position gives from now on; both empty or missing makes it a plain label. */
public record PositionAccessRequest(List<UUID> roleIds, List<String> permissions) {}
