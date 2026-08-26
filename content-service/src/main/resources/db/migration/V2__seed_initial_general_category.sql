insert into service_categories (title, description)
select 'General', 'General engineering and platform discussions'
where not exists (
    select 1 from service_categories
);
