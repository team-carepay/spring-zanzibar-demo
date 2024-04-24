package com.carepay.demo.library;

import org.springframework.modulith.events.Externalized;

@Externalized("book-created::#{getBookId()}")
public record BookCreatedEvent(BookEntity book) {
    public Long getBookId() {
        return book.getId();
    }

    public Long getAuthorId() {
        return book.getAuthor().getId();
    }

    public Long getLibraryId() {
        return book.getLibrary().getId();
    }
}
