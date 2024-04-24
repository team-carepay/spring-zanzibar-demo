package com.carepay.demo.library;

import java.util.List;
import java.util.Optional;

import com.carepay.demo.exception.NotFoundException;
import com.carepay.demo.security.SpiceDBClient;
import jakarta.persistence.Column;
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

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
public class LibraryEntity extends AbstractAggregateRoot<LibraryEntity> {
    public static final String NAMESPACE = "library";
    public static final String RELATION_ADMIN = "admin";
    public static final String RELATION_PARENT = "parent";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(value = AccessLevel.PRIVATE)
    private Long id;

    @Column(unique = true)
    private String name;

    private String user;

    @ManyToOne
    private LibraryEntity parent;

    //region methods
    @PrePersist
    void onLibraryCreated() {
        registerEvent(new LibraryCreatedEvent(this));
    }
    //endregion
}





@Repository
interface LibraryRepository extends JpaRepository<LibraryEntity, Long> {
    Optional<LibraryEntity> findByName(String name);
}


@RestController
@RequestMapping(path = "libraries")
@RequiredArgsConstructor
@Transactional
class LibraryController {
    private final LibraryRepository libraryRepository;
    private final LibraryMapper libraryMapper;

    //region GraphQL
    @QueryMapping
    @PreAuthorize("hasAnyAuthority('admin','author','member')")
    public List<LibraryEntity> libraries() {
        return libraryRepository.findAll(Sort.by("name"));
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('admin')")
    public Library createLibrary(@Argument CreateLibraryRequest createLibraryRequest) {
        return Optional.of(createLibraryRequest)
                .map(libraryMapper::fromCreateLibraryRequest)
                .map(libraryRepository::save)
                .map(libraryMapper::toResponse)
                .orElseThrow();
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('admin') or hasPermission(#updateLibraryRequest.id(), 'library', 'manage')")
    public Library updateLibrary(@Argument UpdateLibraryRequest updateLibraryRequest) {
        return libraryRepository.findById(updateLibraryRequest.id())
                .map(library -> libraryMapper.updateLibrary(library, updateLibraryRequest))
                .map(libraryMapper::toResponse)
                .orElseThrow(NotFoundException::new);
    }

    @MutationMapping
    @PreAuthorize("hasAnyAuthority('admin') or hasPermission(#libraryId, 'library', 'manage')")
    public void deleteLibrary(@Argument Long libraryId) {
        libraryRepository.deleteById(libraryId);
    }
    //endregion

    //region REST
    @GetMapping
    @PreAuthorize("hasAnyAuthority('member','author','admin')")
    public Page<LibraryEntity> listLibraries(Pageable pageable) {
        return libraryRepository.findAll(pageable);
    }

    @GetMapping(path = "{libraryId}")
    @PreAuthorize("hasPermission(#libraryId, 'library', 'view')")
    public LibraryEntity getLibrary(@PathVariable("libraryId") long libraryId) {
        return libraryRepository.findById(libraryId).orElseThrow(NotFoundException::new);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin')")
    public Library addLibrary(@RequestBody final CreateLibraryRequest createLibraryRequest) {
        return Optional.of(createLibraryRequest)
                .map(libraryMapper::fromCreateLibraryRequest)
                .map(libraryRepository::save)
                .map(libraryMapper::toResponse)
                .orElseThrow();
    }

    @PutMapping("{libraryId}")
    @PreAuthorize("hasAnyAuthority('admin') or hasPermission(#libraryId, 'library', 'manage')")
    public Library updateLibrary(@PathVariable("libraryId") long libraryId, @RequestBody final UpdateLibraryRequest updateLibraryRequest) {
        return libraryRepository.findById(libraryId)
                .map(library -> libraryMapper.updateLibrary(library, updateLibraryRequest))
                .map(libraryMapper::toResponse)
                .orElseThrow(NotFoundException::new);
    }
    //endregion
}


@Component
@RequiredArgsConstructor
class LibraryEventListener {
    private final SpiceDBClient spiceDBClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLibraryCreatedEvent(final LibraryCreatedEvent libraryCreatedEvent) {
        if (libraryCreatedEvent.hasParent()) {
            spiceDBClient.addRelation(LibraryEntity.NAMESPACE, libraryCreatedEvent.getLibraryId(), LibraryEntity.RELATION_PARENT, LibraryEntity.NAMESPACE, libraryCreatedEvent.getParentId());
        }
        if (libraryCreatedEvent.getUser() != null) {
            spiceDBClient.addRelation(LibraryEntity.NAMESPACE, libraryCreatedEvent.getLibraryId(), LibraryEntity.RELATION_ADMIN, User.NAMESPACE, libraryCreatedEvent.getUser());
        }
    }
}


record CreateLibraryRequest(String name, String user, Long parentId) { }
record UpdateLibraryRequest(Long id, String name, String user) { }
record Library(Long id, String name, String user) { }


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
abstract class LibraryMapper {

    @Autowired
    private LibraryRepository libraryRepository;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", source = "parentId") // uses lookupLibrary
    abstract LibraryEntity fromCreateLibraryRequest(CreateLibraryRequest createLibraryRequest);

    @Mapping(target = "parent", ignore = true)
    abstract LibraryEntity updateLibrary(@MappingTarget LibraryEntity library, UpdateLibraryRequest updateLibraryRequest);

    abstract Library toResponse(LibraryEntity library);

    LibraryEntity lookupLibrary(final Long id) {
        return libraryRepository.findById(id).orElseThrow();
    }
}
