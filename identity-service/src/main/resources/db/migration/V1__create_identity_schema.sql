create table if not exists identity_roles (
    id int not null,
    name varchar(100) not null,
    primary key (id),
    constraint uk_identity_roles_name unique (name)
);

create table if not exists identity_users (
    id int auto_increment,
    user_name varchar(100) not null,
    about varchar(500) not null,
    email varchar(255) not null,
    password varchar(255) not null,
    primary key (id),
    constraint uk_identity_users_email unique (email)
);

create table if not exists identity_user_roles (
    role_id int not null,
    user_id int not null,
    primary key (role_id, user_id),
    constraint fk_identity_user_roles_role foreign key (role_id) references identity_roles (id),
    constraint fk_identity_user_roles_user foreign key (user_id) references identity_users (id) on delete cascade
);

insert into identity_roles (id, name) values (501, 'ROLE_ADMIN')
    on duplicate key update name = values(name);
insert into identity_roles (id, name) values (502, 'ROLE_NORMAL')
    on duplicate key update name = values(name);
