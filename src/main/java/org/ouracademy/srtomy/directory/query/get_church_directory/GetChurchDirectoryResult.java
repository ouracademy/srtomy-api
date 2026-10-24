package org.ouracademy.srtomy.directory.query.get_church_directory;

import java.util.List;
import org.ouracademy.srtomy.directory.query.DirectoryNodeResult;

public record GetChurchDirectoryResult(List<DirectoryNodeResult> nodes) {}
