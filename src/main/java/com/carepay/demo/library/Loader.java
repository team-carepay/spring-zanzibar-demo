package com.carepay.demo.library;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.PageImpl;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Controller
public class Loader {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final AuthorMapper authorMapper;
    private final BookMapper bookMapper;

    @BatchMapping
    public Mono<Map<Author, Slice<Book>>> books(final List<Author> authors) {
        return Mono.defer(() -> {
            final List<Long> authorIds = authors.stream().map(Author::id).toList();
            final List<BookEntity> books = bookRepository.findByAuthorIds(authorIds);
            final Map<Long, List<Book>> authorMap = books.stream().map(bookMapper::toResponse).collect(Collectors.groupingBy(Book::authorId));
            return Mono.just(authors.stream().collect(Collectors.toMap(a -> a, a -> new PageImpl<>(authorMap.get(a.id())))));
        });
    }

    @BatchMapping
    public Mono<Map<Book, Author>> author(final List<Book> books) {
        return Mono.defer(() -> {
            final Set<Long> authorIds = books.stream().map(Book::authorId).collect(Collectors.toSet());
            final Map<Long, Author> authorMap = authorRepository.findAllById(authorIds).stream().collect(Collectors.toMap(AuthorEntity::getId, authorMapper::toResponse));
            return Mono.just(books.stream().collect(Collectors.toMap(b -> b, b -> authorMap.get(b.authorId()))));
        });
    }
}
