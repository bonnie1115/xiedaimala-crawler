create table links_already_processed
(
    link varchar(1000)
);

create table links_to_be_processed
(
    link varchar(1000)
);

create table news
(
    id          bigint primary key auto_increment,
    title       text,
    url         varchar(1000),
    content     text,
    created_at  timestamp,
    modified_at timestamp

);