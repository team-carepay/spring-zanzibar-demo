package com.carepay.demo.library;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class Initializer {

    private final LibraryRepository libraryRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    void init() {
        if (libraryRepository.findByName("Global Corp").isEmpty()) {
            var globalCorp = libraryRepository.save(LibraryEntity.builder().name("Global Corp").user("global_admin").build());
            var americaComics = libraryRepository.save(LibraryEntity.builder().name("America Comics").parent(globalCorp).build());
            var euroBooks = libraryRepository.save(LibraryEntity.builder().name("Euro Books").parent(globalCorp).build());
            var nlBoek = libraryRepository.save(LibraryEntity.builder().name("NL Boek").user("nl_admin").parent(euroBooks).build());

            var tolkien = authorRepository.save(AuthorEntity.builder().firstName("john").lastName("tolkien").user("tolkien").build());
            bookRepository.save(BookEntity.builder().author(tolkien).title("Lord of the Rings").library(nlBoek).build());
            bookRepository.save(BookEntity.builder().author(tolkien).title("The Hobbit").library(americaComics).build());

            var janeAusten = authorRepository.save(AuthorEntity.builder().firstName("jane").lastName("austen").user("austen").build());
            bookRepository.save(BookEntity.builder().author(janeAusten).title("Price and Prejudice").library(nlBoek).build());
            bookRepository.save(BookEntity.builder().author(janeAusten).title("Sense and Sensibility").library(americaComics).build());
        }
    }
}
