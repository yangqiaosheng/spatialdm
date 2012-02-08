truncate table flickr_world_count;
insert into flickr_world_count (id, radius)
select id, CAST(radius as integer) from flickr_world_area;
commit;