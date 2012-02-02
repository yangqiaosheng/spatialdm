truncate flickr_europe_count;
insert into flickr_europe_count (id, radius) 
select id, CAST(radius as integer) from flickr_europe_area;