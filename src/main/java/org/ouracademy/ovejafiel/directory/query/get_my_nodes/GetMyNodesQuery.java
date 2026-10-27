package org.ouracademy.ovejafiel.directory.query.get_my_nodes;

/** The nodes the person linked to the user identified by {@code subject} belongs to or leads. */
public record GetMyNodesQuery(String subject) {}
