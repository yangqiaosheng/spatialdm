
--Pg
alter table flickr_europe_area_320000
add column area double precision default 0;
update flickr_europe_area_320000 set area = st_area(geom::geography);

alter table flickr_europe_area_160000
add column area double precision default 0;

alter table flickr_europe_area_80000
add column area double precision default 0;

alter table flickr_europe_area_40000
add column area double precision default 0;

alter table flickr_europe_area_20000
add column area double precision default 0;

alter table flickr_europe_area_10000
add column area double precision default 0;

alter table flickr_europe_area_5000
add column area double precision default 0;

alter table flickr_europe_area_2500
add column area double precision default 0;

alter table flickr_europe_area_1250
add column area double precision default 0;

alter table flickr_europe_area_750
add column area double precision default 0;

alter table flickr_europe_area_375
add column area double precision default 0;

--Oracle
alter table flickr_world_area_2560000
add area number;

update flickr_world_area_2560000 set area = SDO_GEOM.SDO_AREA(geom, 5E-7);
commit;
