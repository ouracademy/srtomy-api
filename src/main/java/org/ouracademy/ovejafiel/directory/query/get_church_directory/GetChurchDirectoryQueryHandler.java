package org.ouracademy.ovejafiel.directory.query.get_church_directory;

/**
 * The directory of a church: all its nodes (even those whose people are hidden), each with its
 * name, description, schedule and team. Fails with {@code AccessDeniedException} (403) unless the
 * user has access somewhere in the church, and with {@code NoSuchElementException} (404) when the
 * id is not a church.
 */
public interface GetChurchDirectoryQueryHandler {
  GetChurchDirectoryResult handle(GetChurchDirectoryQuery query);
}
