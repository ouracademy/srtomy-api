package org.ouracademy.srtomy.directory.query.get_public_page;

import java.util.Optional;

/**
 * The public page of a node. Empty (404) unless the node's visibility is {@code PUBLIC}. The org
 * chart of a church or location leaves out the nodes whose visibility is {@code LEADERS}, with
 * everything below them.
 */
public interface GetPublicPageQueryHandler {
  Optional<GetPublicPageResult> handle(GetPublicPageQuery query);
}
