drop table flickr_world_area;

create table flickr_world_area as
select id, name, 2560000 as radius, center_x, center_y, area, geom from flickr_world_area_625 t where 1=0;


insert into flickr_world_area 
select id, name, 2560000 as radius, center_x, center_y, area, geom from flickr_world_area_2560000;
commit;

insert into flickr_world_area 
select id, name, 1280000 as radius, center_x, center_y, area, geom from flickr_world_area_1280000;
commit;

insert into flickr_world_area 
select id, name, 640000 as radius, center_x, center_y, area, geom from flickr_world_area_640000;
commit;

insert into flickr_world_area 
select id, name, 320000 as radius, center_x, center_y, area, geom from flickr_world_area_320000;
commit;

insert into flickr_world_area 
select id, name, 160000 as radius, center_x, center_y, area, geom from flickr_world_area_160000;
commit;

insert into flickr_world_area 
select id, name, 80000 as radius, center_x, center_y, area, geom from flickr_world_area_80000;
commit;

insert into flickr_world_area 
select id, name, 40000 as radius, center_x, center_y, area, geom from flickr_world_area_40000;
commit;

insert into flickr_world_area 
select id, name, 20000 as radius, center_x, center_y, area, geom from flickr_world_area_20000;
commit;

insert into flickr_world_area 
select id, name, 10000 as radius, center_x, center_y, area, geom from flickr_world_area_10000;
commit;

insert into flickr_world_area
select id, name, 5000 as radius, center_x, center_y, area, geom from flickr_world_area_5000;
commit;

insert into flickr_world_area
select id, name, 2500 as radius, center_x, center_y, area, geom from flickr_world_area_2500;
commit;

insert into flickr_world_area
select id, name, 1250 as radius, center_x, center_y, area, geom from flickr_world_area_1250;
commit;

insert into flickr_world_area
select id, name, 625 as radius, center_x, center_y, area, geom from flickr_world_area_625;
commit;

--PostGIS
alter table flickr_world_area add primary key (id);

CREATE INDEX ON flickr_world_area USING gist(geom);

--ORACLE

alter table flickr_world_area 
add CONSTRAINT flickr_world_area_pk primary key (id);

create index flickr_world_area_sdx on flickr_world_area(geom)
      indextype is MDSYS.SPATIAL_INDEX;
commit;


