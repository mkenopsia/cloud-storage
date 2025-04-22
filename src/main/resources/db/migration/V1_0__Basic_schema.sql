create schema if not exists user_management;

create table if not exists user_management.t_user
(
    id serial primary key,
    c_username varchar(100) not null unique check ( length(trim(c_username)) >= 3 ),
    c_password varchar(100) not null check ( length(trim(c_password)) >= 5 )
);