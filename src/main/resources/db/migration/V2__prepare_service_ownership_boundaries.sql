alter table comments
    add column author_id int null;

create index idx_comments_author_id on comments (author_id);
create index idx_comments_post_id on comments (post_post_id);

alter table comments
    drop foreign key fk_comments_post;

alter table comments
    add constraint fk_comments_post
        foreign key (post_post_id) references posts (post_id)
        on delete cascade;

alter table posts
    drop foreign key fk_posts_user;
