package org.ouracademy.ovejafiel.event.controller.dto;

import java.util.UUID;

/** The new event and how many sub-events were copied with it. */
public record CopyEventResponse(UUID id, int subEvents) {}
