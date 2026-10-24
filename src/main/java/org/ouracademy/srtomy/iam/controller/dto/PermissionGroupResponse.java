package org.ouracademy.srtomy.iam.controller.dto;

import java.util.List;

public record PermissionGroupResponse(String resource, List<String> permissions) {}
