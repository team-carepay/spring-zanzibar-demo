create table if not exists event_publication
(
    id               binary(16) not null,
    listener_id      varchar(512) not null,
    event_type       varchar(512) not null,
    serialized_event varchar(4000) not null,
    publication_date timestamp(6) not null,
    completion_date  timestamp(6) default null null,
    primary key (id)
);

create table  if not exists author_entity
(
    id         bigint auto_increment primary key,
    bio        varchar(255) null,
    first_name varchar(255) null,
    last_name  varchar(255) null,
    user       varchar(255) null,
    constraint UK_hfcnejna0tcp0nvvgbe1muuq0 unique (user)
);

create table  if not exists library_entity
(
    id        bigint auto_increment primary key,
    name      varchar(255) null,
    user      varchar(255) null,
    parent_id bigint       null,
    constraint UK_oovnydoels4uh7xlndis8hxsi unique (name),
    constraint FKcbpofi60wl4cc8cdg8uq4om9e foreign key (parent_id) references library_entity (id)
);


create table  if not exists book_entity
(
    id          bigint auto_increment primary key,
    description varchar(255) null,
    title       varchar(255) null,
    author_id   bigint       null,
    library_id  bigint       null,
    constraint FK3e3nppyc2tarykc693kx2cmim foreign key (author_id) references author_entity (id),
    constraint FK5eywwtf7ojmwmdk9lbxk5w97c foreign key (library_id) references library_entity (id)
);

