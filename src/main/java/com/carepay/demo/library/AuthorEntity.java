package com.carepay.demo.library;

import java.util.List;
import java.util.Optional;

import com.carepay.demo.exception.NotFoundException;
import com.carepay.demo.security.ZanzibarClient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AuthorEntity extends AbstractAggregateRoot<AuthorEntity> {
    public static final String NAMESPACE = "author";
    public static final String RELATION_USER = "user";

    @Id
    @Setter(value = AccessLevel.PRIVATE)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(unique = true)
    String user;

    String firstName;

    String lastName;

    String bio;

    @OneToMany(mappedBy = "author")
    List<BookEntity> books;

    //region methods
    void addBook(final BookEntity book) {
        book.setAuthor(this);
        books.add(book);
    }

    @PrePersist
    void onAuthorCreated() {
        registerEvent(new AuthorCreatedEvent(this));
    }
    //endregion
}









@Repository
interface AuthorRepository extends JpaRepository<AuthorEntity, Long> {
    Optional<AuthorEntity> findByUser(String name);
}







@RestController
@RequestMapping(path = "authors")
@RequiredArgsConstructor
@Transactional
class AuthorController {
    public static final Sort LAST_NAME_SORT = Sort.by("lastName").ascending();
    private final AuthorRepository authorRepository;
    private final AuthorMapper authorMapper;

    //region GraphQL
    @QueryMapping
    @PreAuthorize("hasAnyAuthority('admin','author','member')")
    public List<Author> authors() {
        return authorRepository.findAll(LAST_NAME_SORT).stream()
                .map(authorMapper::toResponse)
                .toList();
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('admin')")
    public Author createAuthor(@Argument CreateAuthorRequest createAuthorRequest) {
        return Optional.of(createAuthorRequest)
                        .map(authorMapper::fromCreateAuthorRequest)
                        .map(authorRepository::save)
                        .map(authorMapper::toResponse)
                        .orElseThrow();
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('admin') or hasPermission(#updateAuthorRequest.id, 'author', 'manage')")
    public Author updateAuthor(@Argument UpdateAuthorRequest updateAuthorRequest) {
        return authorRepository.findById(updateAuthorRequest.id())
                .map(author -> authorMapper.updateAuthor(author, updateAuthorRequest))
                .map(authorMapper::toResponse)
                .orElseThrow(NotFoundException::new);
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('admin') or hasPermission(#authorId, 'author', 'manage')")
    public void deleteAuthor(@Argument Long authorId) {
        authorRepository.deleteById(authorId);
    }
    //endregion

    //region REST
    @GetMapping
    @PreAuthorize("hasAnyAuthority('member','author','admin')")
    public Page<Author> listAuthors(Pageable pageable) {
        return authorRepository.findAll(pageable)
                .map(authorMapper::toResponse);
    }

    @GetMapping(path = "{authorId}")
    @PreAuthorize("hasPermission(#authorId, 'author', 'view')")
    public Author getAuthor(@PathVariable("authorId") long authorId) {
        return authorRepository.findById(authorId)
                .map(authorMapper::toResponse)
                .orElseThrow(NotFoundException::new);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin')")
    public Author addAuthor(@RequestBody final CreateAuthorRequest request) {
        return Optional.of(request)
                .map(authorMapper::fromCreateAuthorRequest)
                .map(authorRepository::save)
                .map(authorMapper::toResponse)
                .orElseThrow();
    }

    @PutMapping("{authorId}")
    @PreAuthorize("hasAnyAuthority('admin') or hasPermission(#authorId, 'author', 'manage')")
    public Author updateAuthor(@PathVariable("authorId") long authorId, @RequestBody final UpdateAuthorRequest request) {
        return authorRepository.findById(authorId)
                .map(author -> authorMapper.updateAuthor(author, request))
                .map(authorMapper::toResponse)
                .orElseThrow(NotFoundException::new);    }
    //endregion
}








@Component
@RequiredArgsConstructor
class AuthorEventListener {
    private final ZanzibarClient zanzibarClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuthorCreatedEvent(final AuthorCreatedEvent authorCreatedEvent) {
        zanzibarClient.addRelation(AuthorEntity.NAMESPACE, authorCreatedEvent.getAuthorId(), AuthorEntity.RELATION_USER, User.NAMESPACE, authorCreatedEvent.getUsername());
    }
}







record CreateAuthorRequest(String firstName, String lastName, String user, String bio) {}
record UpdateAuthorRequest(Long id, String firstName, String lastName, String user, String bio) {}
record Author(Long id, String firstName, String lastName, String user, String bio) {}







@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
abstract class AuthorMapper {
    @Autowired
    private AuthorRepository authorRepository;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "books", ignore = true)
    abstract AuthorEntity fromCreateAuthorRequest(CreateAuthorRequest createAuthorRequest);

    @Mapping(target = "books", ignore = true)
    abstract AuthorEntity updateAuthor(@MappingTarget AuthorEntity author, UpdateAuthorRequest updateAuthorRequest);

    abstract Author toResponse(AuthorEntity author);

    AuthorEntity lookupAuthor(Long authorId) {
        return authorRepository.findById(authorId).orElseThrow();
    }
}
