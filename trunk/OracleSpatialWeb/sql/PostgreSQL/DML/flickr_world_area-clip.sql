create table flickr_world_area_clip_2560000 as 
select id,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area_2560000 where ST_IsValid(geom);
alter table flickr_world_area_clip_2560000 add primary key (id);
CREATE INDEX ON flickr_world_area_clip_2560000 USING gist(geom);

create table flickr_world_area_clip_1280000 as 
select id,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area_1280000 where ST_IsValid(geom);
alter table flickr_world_area_clip_1280000 add primary key (id);
CREATE INDEX ON flickr_world_area_clip_1280000 USING gist(geom);

create table flickr_world_area_clip_640000 as 
select id,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area_640000 where ST_IsValid(geom);
alter table flickr_world_area_clip_640000 add primary key (id);
CREATE INDEX ON flickr_world_area_clip_640000 USING gist(geom);

create table flickr_world_area_clip_320000 as 
select id,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area_320000 where ST_IsValid(geom);
alter table flickr_world_area_clip_320000 add primary key (id);
CREATE INDEX ON flickr_world_area_clip_320000 USING gist(geom);

create table flickr_world_area_clip_160000 as 
select id,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area_160000 where ST_IsValid(geom);
alter table flickr_world_area_clip_160000 add primary key (id);
CREATE INDEX ON flickr_world_area_clip_160000 USING gist(geom);

create table flickr_world_area_clip_80000 as 
select id,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area_80000 where ST_IsValid(geom);
alter table flickr_world_area_clip_80000 add primary key (id);
CREATE INDEX ON flickr_world_area_clip_80000 USING gist(geom);

create table flickr_world_area_clip_40000 as 
select id,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area_40000 where ST_IsValid(geom);
alter table flickr_world_area_clip_40000 add primary key (id);
CREATE INDEX ON flickr_world_area_clip_40000 USING gist(geom);

create table flickr_world_area_clip_20000 as 
select id,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area_20000 where ST_IsValid(geom);
alter table flickr_world_area_clip_20000 add primary key (id);
CREATE INDEX ON flickr_world_area_clip_20000 USING gist(geom);

create table flickr_world_area_clip_10000 as 
select id,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area_10000 where ST_IsValid(geom);
alter table flickr_world_area_clip_10000 add primary key (id);
CREATE INDEX ON flickr_world_area_clip_10000 USING gist(geom);

create table flickr_world_area_clip_5000 as 
select id,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area_5000 where ST_IsValid(geom);
alter table flickr_world_area_clip_5000 add primary key (id);
CREATE INDEX ON flickr_world_area_clip_5000 USING gist(geom);

create table flickr_world_area_clip_2500 as 
select id,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area_2500 where ST_IsValid(geom);
alter table flickr_world_area_clip_2500 add primary key (id);
CREATE INDEX ON flickr_world_area_clip_2500 USING gist(geom);

create table flickr_world_area_clip_1250 as 
select id,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area_1250 where ST_IsValid(geom);
alter table flickr_world_area_clip_1250 add primary key (id);
CREATE INDEX ON flickr_world_area_clip_1250 USING gist(geom);

create table flickr_world_area_clip_625 as 
select id,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area_625 where ST_IsValid(geom);
alter table flickr_world_area_clip_625 add primary key (id);
CREATE INDEX ON flickr_world_area_clip_625 USING gist(geom);

create table flickr_world_area_clip as 
select id,radius,name,center_x,center_y,area,ST_Intersection(geom,'SRID=4326;POLYGON((-180 -85,180 -85,180 85,-180 85,-180 -85))') as geom 
from flickr_world_area where ST_IsValid(geom);
alter table flickr_world_area_clip add primary key (id);
CREATE INDEX ON flickr_world_area_clip USING gist(geom);

drop table flickr_world_area;
drop table flickr_world_area_2560000;
drop table flickr_world_area_1280000;
drop table flickr_world_area_640000;
drop table flickr_world_area_320000;
drop table flickr_world_area_160000;
drop table flickr_world_area_80000;
drop table flickr_world_area_40000;
drop table flickr_world_area_20000;
drop table flickr_world_area_10000;
drop table flickr_world_area_5000;
drop table flickr_world_area_2500;
drop table flickr_world_area_1250;
drop table flickr_world_area_625;

alter table flickr_world_area_clip rename to flickr_world_area;
alter table flickr_world_area_clip_2560000 rename to flickr_world_area_2560000;
alter table flickr_world_area_clip_1280000 rename to flickr_world_area_1280000;
alter table flickr_world_area_clip_640000 rename to flickr_world_area_640000;
alter table flickr_world_area_clip_320000 rename to flickr_world_area_320000;
alter table flickr_world_area_clip_160000 rename to flickr_world_area_160000;
alter table flickr_world_area_clip_80000 rename to flickr_world_area_80000;
alter table flickr_world_area_clip_40000 rename to flickr_world_area_40000;
alter table flickr_world_area_clip_20000 rename to flickr_world_area_20000;
alter table flickr_world_area_clip_10000 rename to flickr_world_area_10000;
alter table flickr_world_area_clip_5000 rename to flickr_world_area_5000;
alter table flickr_world_area_clip_2500 rename to flickr_world_area_2500;
alter table flickr_world_area_clip_1250 rename to flickr_world_area_1250;
alter table flickr_world_area_clip_625 rename to flickr_world_area_625;



