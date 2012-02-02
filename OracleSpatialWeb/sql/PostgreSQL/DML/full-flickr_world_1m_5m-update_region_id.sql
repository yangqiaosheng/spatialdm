alter table flickr_world_topviewed_1m add primary key 
(photo_id);

set @date = select now();
print @date;


create table flickr_world_topviewed_1m_region_id as
select f.photo_id, a.radius, a.id as region_id from flickr_world_area a, flickr_world_topviewed_1m f
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||f.longitude||' '||f.latitude||')'), a.geom::geometry);

set @date = select now();
print @date;


create index on flickr_world_topviewed_1m_region_id
  USING btree
  (photo_id);
  
--dirty result

delete from flickr_world_topviewed_1m where photo_id in 
(select photo_id from flickr_world_topviewed_1m_region_id group by photo_id having count(*) > 11);

delete from flickr_world_topviewed_1m_region_id where photo_id in 
(select photo_id from flickr_world_topviewed_1m_region_id group by photo_id having count(*) > 11);

set @date = select now();
print @date;

create table flickr_world_topviewed_1m_region_id_seperate as
select photo_id, 
       case when radius = '2560000' then region_id end as region_2560000_id,
       case when radius = '1280000' then region_id end as region_1280000_id,
       case when radius = '640000' then region_id end as region_640000_id,
       case when radius = '320000' then region_id end as region_320000_id,
       case when radius = '160000' then region_id end as region_160000_id,
       case when radius = '80000' then region_id end as region_80000_id,
       case when radius = '40000' then region_id end as region_40000_id,
       case when radius = '20000' then region_id end as region_20000_id,
       case when radius = '10000' then region_id end as region_10000_id,
       case when radius = '5000' then region_id end as region_5000_id,
       case when radius = '2500' then region_id end as region_2500_id,
       case when radius = '1250' then region_id end as region_1250_id,
       case when radius = '625' then region_id end as region_625_id
 from flickr_world_topviewed_1m_region_id;

create index on flickr_world_topviewed_1m_region_id_seperate
  USING btree
  (photo_id);

set @date = select now();
print @date;

create table flickr_world_topviewed_1m_region_id_combine as
select photo_id,
       sum(region_2560000_id) as region_2560000_id,
       sum(region_1280000_id) as region_1280000_id,
       sum(region_640000_id) as region_640000_id,
       sum(region_320000_id) as region_320000_id,
       sum(region_160000_id) as region_160000_id,
       sum(region_80000_id) as region_80000_id,
       sum(region_40000_id) as region_40000_id,
       sum(region_20000_id) as region_20000_id,
       sum(region_10000_id) as region_10000_id,
       sum(region_5000_id) as region_5000_id,
       sum(region_2500_id) as region_2500_id,
       sum(region_1250_id) as region_1250_id,
       sum(region_625_id) as region_625_id
from flickr_world_topviewed_1m_region_id_seperate
group by photo_id;

alter table flickr_world_topviewed_1m_region_id_combine add primary key 
(photo_id);

set @date = select now();
print @date;

create table flickr_world_topviewed_1m_with_region_id as
select f.*, 
       region_2560000_id,
       region_1280000_id,
       region_640000_id,
       region_320000_id,
       region_160000_id,
       region_80000_id,
       region_40000_id,
       region_20000_id,
       region_10000_id,
       region_5000_id,
       region_2500_id,
       region_1250_id,
       region_625_id
from flickr_world_topviewed_1m f, flickr_world_topviewed_1m_region_id_combine r
where f.photo_id = r.photo_id;

alter table flickr_world_topviewed_1m_with_region_id add primary key 
(photo_id);

set @date = select now();
print @date;


create table flickr_world_topviewed_5m_region_id as
select f.photo_id, a.radius, a.id as region_id from flickr_world_area a, flickr_world_topviewed_5m f
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||f.longitude||' '||f.latitude||')'), a.geom::geometry);

set @date = select now();
print @date;


create index on flickr_world_topviewed_5m_region_id
  USING btree
  (photo_id);
  
--dirty result

delete from flickr_world_topviewed_5m where photo_id in 
(select photo_id from flickr_world_topviewed_5m_region_id group by photo_id having count(*) > 11);

delete from flickr_world_topviewed_5m_region_id where photo_id in 
(select photo_id from flickr_world_topviewed_5m_region_id group by photo_id having count(*) > 11);

set @date = select now();
print @date;

create table flickr_world_topviewed_5m_region_id_seperate as
select photo_id, 
       case when radius = '2560000' then region_id end as region_2560000_id,
       case when radius = '1280000' then region_id end as region_1280000_id,
       case when radius = '640000' then region_id end as region_640000_id,
       case when radius = '320000' then region_id end as region_320000_id,
       case when radius = '160000' then region_id end as region_160000_id,
       case when radius = '80000' then region_id end as region_80000_id,
       case when radius = '40000' then region_id end as region_40000_id,
       case when radius = '20000' then region_id end as region_20000_id,
       case when radius = '10000' then region_id end as region_10000_id,
       case when radius = '5000' then region_id end as region_5000_id,
       case when radius = '2500' then region_id end as region_2500_id,
       case when radius = '1250' then region_id end as region_1250_id,
       case when radius = '625' then region_id end as region_625_id
 from flickr_world_topviewed_5m_region_id;

create index on flickr_world_topviewed_5m_region_id_seperate
  USING btree
  (photo_id);

set @date = select now();
print @date;

create table flickr_world_topviewed_5m_region_id_combine as
select photo_id,
       sum(region_2560000_id) as region_2560000_id,
       sum(region_1280000_id) as region_1280000_id,
       sum(region_640000_id) as region_640000_id,
       sum(region_320000_id) as region_320000_id,
       sum(region_160000_id) as region_160000_id,
       sum(region_80000_id) as region_80000_id,
       sum(region_40000_id) as region_40000_id,
       sum(region_20000_id) as region_20000_id,
       sum(region_10000_id) as region_10000_id,
       sum(region_5000_id) as region_5000_id,
       sum(region_2500_id) as region_2500_id,
       sum(region_1250_id) as region_1250_id,
       sum(region_625_id) as region_625_id
from flickr_world_topviewed_5m_region_id_seperate
group by photo_id;

alter table flickr_world_topviewed_5m_region_id_combine add primary key 
(photo_id);

set @date = select now();
print @date;

create table flickr_world_topviewed_5m_with_region_id as
select f.*, 
       region_2560000_id,
       region_1280000_id,
       region_640000_id,
       region_320000_id,
       region_160000_id,
       region_80000_id,
       region_40000_id,
       region_20000_id,
       region_10000_id,
       region_5000_id,
       region_2500_id,
       region_1250_id,
       region_625_id
from flickr_world_topviewed_5m f, flickr_world_topviewed_5m_region_id_combine r
where f.photo_id = r.photo_id;

alter table flickr_world_topviewed_5m_with_region_id add primary key 
(photo_id);

set @date = select now();
print @date;