package com.carepay.demo.library;

public record AuthorCreatedEvent(AuthorEntity author) {
    public Long getAuthorId() {
        return author.getId();
    }

    public String getUsername() {
        return author.getUser();
    }
}
