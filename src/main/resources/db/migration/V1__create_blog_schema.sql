create table if not exists catgories (
    category_id int auto_increment,
    description varchar(255),
    title varchar(255),
    primary key (category_id)
);

create table if not exists users (
    id int auto_increment,
    user_name varchar(100) not null,
    about varchar(500) not null,
    email varchar(255) not null,
    password varchar(255) not null,
    primary key (id),
    constraint uk_users_email unique (email)
);

create table if not exists `role` (
    id int not null,
    name varchar(255),
    primary key (id)
);

create table if not exists posts (
    category_id int,
    post_id int auto_increment,
    user_id int,
    added_date datetime(6),
    post_title varchar(100) not null,
    content varchar(10000),
    image_name varchar(255),
    primary key (post_id),
    constraint fk_posts_category foreign key (category_id) references catgories (category_id),
    constraint fk_posts_user foreign key (user_id) references users (id)
);

create table if not exists comments (
    id int auto_increment,
    post_post_id int,
    content varchar(255),
    primary key (id),
    constraint fk_comments_post foreign key (post_post_id) references posts (post_id)
);

create table if not exists user_role (
    role_id int not null,
    user_id int not null,
    primary key (role_id, user_id),
    constraint fk_user_role_role foreign key (role_id) references `role` (id),
    constraint fk_user_role_user foreign key (user_id) references users (id)
);

insert ignore into `role` (id, name) values (501, 'ROLE_ADMIN');
update `role` set name = 'ROLE_ADMIN' where id = 501;

insert ignore into `role` (id, name) values (502, 'ROLE_NORMAL');
update `role` set name = 'ROLE_NORMAL' where id = 502;
