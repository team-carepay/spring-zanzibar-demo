package com.carepay.demo.util;

import com.carepay.demo.library.AuthorEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
public class ReferenceMapper {

    @PersistenceContext
    private EntityManager entityManager;

    public AuthorEntity lookupAuthor(final Long id) {
        return entityManager.getReference(AuthorEntity.class, id);
    }
}