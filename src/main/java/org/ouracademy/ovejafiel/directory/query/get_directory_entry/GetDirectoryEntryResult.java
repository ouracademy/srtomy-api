package org.ouracademy.ovejafiel.directory.query.get_directory_entry;

import java.util.List;
import org.ouracademy.ovejafiel.directory.query.DirectoryNodeResult;

/**
 * A node of the directory with its children. {@code members} is empty and {@code membersVisible}
 * false when the node's visibility hides its people from the caller.
 */
public record GetDirectoryEntryResult(
    DirectoryNodeResult node,
    boolean membersVisible,
    List<String> members,
    List<DirectoryNodeResult> children) {}
