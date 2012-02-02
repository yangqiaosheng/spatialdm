

--Pg
update flickr_world_area_2560000 set id = null;

alter table flickr_world_area
alter column id type integer;

alter table flickr_world_area_2560000
alter column id type integer;

alter table flickr_world_area_2560000 add primary key (id);

--Oracle

update flickr_world_area_2560000 set id = null;
commit;

alter table flickr_world_area_2560000
modify id number;


update flickr_world_area_2560000 set id = to_number(name);
commit;

update flickr_world_area_1280000 set id = to_number(name) + (select max(id) from flickr_world_area_2560000 t);
commit;

update flickr_world_area_640000 set id = to_number(name) + (select max(id) from flickr_world_area_1280000 t);
commit;

update flickr_world_area_320000 set id = to_number(name) + (select max(id) from flickr_world_area_640000 t);
commit;

update flickr_world_area_160000 set id = to_number(name) + (select max(id) from flickr_world_area_320000 t);
commit;

update flickr_world_area_80000 set id = to_number(name) + (select max(id) from flickr_world_area_160000 t);
commit;

update flickr_world_area_40000 set id = to_number(name) + (select max(id) from flickr_world_area_80000 t);
commit;

update flickr_world_area_20000 set id = to_number(name) + (select max(id) from flickr_world_area_40000 t);
commit;

update flickr_world_area_10000 set id = to_number(name) + (select max(id) from flickr_world_area_20000 t);
commit;

update flickr_world_area_5000 set id = to_number(name) + (select max(id) from flickr_world_area_10000 t);
commit;

update flickr_world_area_2500 set id = to_number(name) + (select max(id) from flickr_world_area_5000 t);
commit;

update flickr_world_area_1250 set id = to_number(name) + (select max(id) from flickr_world_area_2500 t);
commit;

update flickr_world_area_625 set id = to_number(name) + (select max(id) from flickr_world_area_1250 t);
commit;

alter table flickr_world_area_2560000 
add CONSTRAINT flickr_world_area_2560000_pk primary key (id);

create index flickr_world_area_2560000_sdx on flickr_world_area_2560000(geom)
      indextype is MDSYS.SPATIAL_INDEX;
commit;
