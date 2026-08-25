create table if not exists service_categories (
    category_id int auto_increment,
    title varchar(255) not null,
    description varchar(255) not null,
    primary key (category_id)
);

create table if not exists service_comments (
    id int auto_increment,
    content varchar(255) not null,
    post_id int not null,
    author_id int,
    primary key (id),
    index idx_service_comments_post_id (post_id)
);
