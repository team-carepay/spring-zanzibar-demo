package com.carepay.demo.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.graphql.data.query.ScrollSubrange;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Pagination {
    private static final int DEFAULT_LIMIT = 20;

    public static ScrollPosition scrollPosition(final ScrollSubrange subrange) {
        return subrange.position().orElse(ScrollPosition.offset());
    }

    public static Limit limit(final ScrollSubrange subrange) {
        return Limit.of(subrange.count().orElse(DEFAULT_LIMIT));
    }
}
