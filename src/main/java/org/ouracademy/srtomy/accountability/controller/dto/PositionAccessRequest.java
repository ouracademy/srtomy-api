package org.ouracademy.srtomy.accountability.controller.dto;

import java.util.UUID;

/** The new access level of a position; null makes it a plain label. */
public record PositionAccessRequest(UUID accessRoleId) {}
