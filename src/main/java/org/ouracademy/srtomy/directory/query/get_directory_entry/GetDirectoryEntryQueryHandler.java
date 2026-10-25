package org.ouracademy.srtomy.directory.query.get_directory_entry;

/**
 * A node of the church directory. Its list of people follows its visibility: {@code PUBLIC} and
 * {@code CHURCH} show it to anyone in the church, {@code MEMBERS} to the members of the node and
 * whoever has {@code members.read} there, {@code LEADERS} only to whoever has {@code members.read}.
 * Known minors are never listed. Fails with {@code AccessDeniedException} (403) unless the user has
 * access somewhere in the church, and with {@code NoSuchElementException} (404) when the node does
 * not exist.
 */
public interface GetDirectoryEntryQueryHandler {
  GetDirectoryEntryResult handle(GetDirectoryEntryQuery query);
}
