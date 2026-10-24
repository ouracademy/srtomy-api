package org.ouracademy.srtomy.directory.controller.dto;

import java.util.List;
import org.ouracademy.srtomy.directory.query.get_directory_entry.GetDirectoryEntryResult;

/** {@code members} is empty and {@code membersVisible} false when the caller may not see them. */
public record DirectoryEntryResponse(
    DirectoryNodeResponse node,
    boolean membersVisible,
    List<String> members,
    List<DirectoryNodeResponse> children) {

  public static DirectoryEntryResponse from(GetDirectoryEntryResult result) {
    return new DirectoryEntryResponse(
        DirectoryNodeResponse.from(result.node()),
        result.membersVisible(),
        result.members(),
        result.children().stream().map(DirectoryNodeResponse::from).toList());
  }
}
