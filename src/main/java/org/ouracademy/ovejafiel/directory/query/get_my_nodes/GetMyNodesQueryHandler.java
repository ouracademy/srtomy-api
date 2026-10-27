package org.ouracademy.ovejafiel.directory.query.get_my_nodes;

import java.util.List;

public interface GetMyNodesQueryHandler {

  /** Sorted by church and name; empty when the user has no linked person anywhere. */
  List<MyNodeResult> handle(GetMyNodesQuery query);
}
