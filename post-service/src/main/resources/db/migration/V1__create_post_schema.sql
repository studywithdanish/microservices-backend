create table if not exists service_posts (
    post_id int auto_increment,
    post_title varchar(100) not null,
    content varchar(10000) not null,
    image_name varchar(255) not null,
    added_date datetime(6) not null,
    author_id int not null,
    category_id int not null,
    category_title varchar(255) not null,
    category_description varchar(255),
    primary key (post_id)
);

create index idx_service_posts_author on service_posts (author_id);
create index idx_service_posts_category on service_posts (category_id);
create index idx_service_posts_added_date on service_posts (added_date);
