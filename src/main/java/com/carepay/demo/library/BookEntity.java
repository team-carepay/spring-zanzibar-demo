package com.carepay.demo.library;

import java.util.List;
import java.util.Optional;

import com.carepay.demo.exception.NotFoundException;
import com.carepay.demo.security.ZanzibarClient;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.carepay.demo.util.Pagination.limit;
import static com.carepay.demo.util.Pagination.scrollPosition;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
class BookEntity extends AbstractAggregateRoot<BookEntity> {

    public static final String NAMESPACE = "book";
    public static final String RELATION_AUTHOR = "author";
    public static final String RELATION_LIBRARY = "library";

    @Id
    @Setter(value = AccessLevel.PRIVATE)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    @ManyToOne
    private AuthorEntity author;

    @ManyToOne
    private LibraryEntity library;

    //region methods
    @PrePersist
    void onBookCreated() {
        registerEvent(new BookCreatedEvent(this));
    }
    //endregion
}








@Repository
interface BookRepository extends JpaRepository<BookEntity, Long> {
    Optional<BookEntity> findByTitle(String title);
    Window<BookEntity> findAllBy(ScrollPosition position, Limit limit, Sort sort);

    @Query("from BookEntity b where b.author.id in (:authorIds)")
    List<BookEntity> findByAuthorIds(List<Long> authorIds);

    Window<BookEntity> findAllByIdIn(List<Long> bookIds, ScrollPosition scrollPosition, Limit limit, Sort sort);
}




@RestController
@RequestMapping(path = "books")
@RequiredArgsConstructor
@Transactional
class BookController {
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final ZanzibarClient zanzibarClient;

    private static final Sort TITLE_SORT = Sort.by("title").ascending();

    //region GraphQL
    @QueryMapping
    @PreAuthorize("hasAnyAuthority('admin')")
    public Window<Book> allbooks(final ScrollSubrange subrange) {
        return bookRepository.findAllBy(scrollPosition(subrange), limit(subrange), TITLE_SORT)
                .map(bookMapper::toResponse);
    }

    @QueryMapping
    @PreAuthorize("hasAnyAuthority('author')")
    public Window<Book> authorbooks(final ScrollSubrange subrange) {
        var allowedBookIds = zanzibarClient.getAllowedResources("book", "view")
                .stream().map(Long::parseLong).toList();
        return bookRepository.findAllByIdIn(allowedBookIds, scrollPosition(subrange), limit(subrange), TITLE_SORT)
                .map(bookMapper::toResponse);
    }


    @MutationMapping
    @PreAuthorize("hasAnyAuthority('admin')")
    public Book createBook(@Argument CreateBookRequest createBookRequest) {
        return Optional.of(createBookRequest)
                .map(bookMapper::fromCreateBookRequest)
                .map(bookRepository::save)
                .map(bookMapper::toResponse)
                .orElseThrow();
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('admin') or hasPermission(#updateBookRequest.id(), 'book', 'update')")
    public Book updateBook(@Argument UpdateBookRequest updateBookRequest) {
        return bookRepository.findById(updateBookRequest.id())
                .map(book -> bookMapper.updateBook(book, updateBookRequest))
                .map(bookMapper::toResponse)
                .orElseThrow(NotFoundException::new);
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('admin') or hasPermission(#bookId, 'book', 'delete')")
    public void deleteBook(@Argument Long bookId) {
        bookRepository.deleteById(bookId);
    }
    //endregion

    //region REST
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin','author','member') or hasPermission(#bookId, 'book', 'view')")
    public Page<Book> listBooks(Pageable pageable) {
        return bookRepository.findAll(pageable)
                .map(bookMapper::toResponse);
    }

    @GetMapping(path = "{bookId}")
    @PreAuthorize("hasAnyAuthority('admin') or hasPermission(#bookId, 'book', 'view')")
    public BookEntity getBook(@PathVariable("bookId") long bookId) {
        return bookRepository.findById(bookId).orElseThrow(NotFoundException::new);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin')")
    public Book addBook(@RequestBody CreateBookRequest createBookRequest) {
        return Optional.of(createBookRequest)
                .map(bookMapper::fromCreateBookRequest)
                .map(bookRepository::save)
                .map(bookMapper::toResponse)
                .orElseThrow();    }
    //endregion
}









@Component
@RequiredArgsConstructor
class BookEventListener {
    private final ZanzibarClient zanzibarClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookCreatedEvent(final BookCreatedEvent bookCreatedEvent) {
        zanzibarClient.addRelation(BookEntity.NAMESPACE, bookCreatedEvent.getBookId(), BookEntity.RELATION_AUTHOR, AuthorEntity.NAMESPACE, bookCreatedEvent.getAuthorId());
        zanzibarClient.addRelation(BookEntity.NAMESPACE, bookCreatedEvent.getBookId(), BookEntity.RELATION_LIBRARY, LibraryEntity.NAMESPACE, bookCreatedEvent.getLibraryId());
    }
}








record CreateBookRequest(String title, String description, Long authorId) {}
record UpdateBookRequest(Long id, String title, String description) {}
record Book(Long id, String title, String description, Long authorId, Long libraryId) {}










@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = AuthorMapper.class)
interface BookMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", source = "authorId") // uses lookupAuthor from AuthorMapper
    @Mapping(target = "library", ignore = true)
    BookEntity fromCreateBookRequest(CreateBookRequest createBookRequest);

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "library", ignore = true)
    BookEntity updateBook(@MappingTarget BookEntity book, UpdateBookRequest updateBookRequest);

    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "libraryId", source = "library.id")
    Book toResponse(BookEntity book);
}
