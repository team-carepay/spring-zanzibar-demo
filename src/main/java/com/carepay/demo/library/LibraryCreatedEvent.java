package com.carepay.demo.library;

import java.util.Optional;

public record LibraryCreatedEvent(LibraryEntity library) {
    public boolean hasParent() {
        return library.getParent() != null;
    }

    public Long getLibraryId() {
        return library.getId();
    }

    public Long getParentId() {
        return Optional.ofNullable(library.getParent()).map(LibraryEntity::getId).orElse(null);
    }

    public String getUser() {
        return library.getUser();
    }
}
