--快， 利用临时表和索引的group by功能，避免多表连接


alter table flickr_europe_without_region_id add primary key 
(photo_id);

create table flickr_europe_region_id as
select f.photo_id, a.radius, a.id as region_id from flickr_europe_area a, flickr_europe f
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||f.longitude||' '||f.latitude||')'), a.geom::geometry);

create index on flickr_europe_region_id
  USING btree
  (photo_id);
  
--dirty result
select photo_id from flickr_europe_region_id group by photo_id having count(*) > 11;

delete from flickr_europe where photo_id in
(select photo_id from flickr_europe_region_id group by photo_id having count(*) > 11);

delete from flickr_europe_region_id where photo_id in
(select photo_id from flickr_europe_region_id group by photo_id having count(*) > 11);


create table flickr_europe_region_id_seperate as
select photo_id, 
       case when radius = '320000' then region_id end as region_320000_id,
       case when radius = '160000' then region_id end as region_160000_id,
       case when radius = '80000' then region_id end as region_80000_id,
       case when radius = '40000' then region_id end as region_40000_id,
       case when radius = '20000' then region_id end as region_20000_id,
       case when radius = '10000' then region_id end as region_10000_id,
       case when radius = '5000' then region_id end as region_5000_id,
       case when radius = '2500' then region_id end as region_2500_id,
       case when radius = '1250' then region_id end as region_1250_id,
       case when radius = '750' then region_id end as region_750_id,
       case when radius = '375' then region_id end as region_375_id
 from flickr_europe_region_id;

create index on flickr_europe_region_id_seperate
  USING btree
  (photo_id);
  
create table flickr_europe_region_id_combine as
select photo_id,
       sum(region_320000_id) as region_320000_id,
       sum(region_160000_id) as region_160000_id,
       sum(region_80000_id) as region_80000_id,
       sum(region_40000_id) as region_40000_id,
       sum(region_20000_id) as region_20000_id,
       sum(region_10000_id) as region_10000_id,
       sum(region_5000_id) as region_5000_id,
       sum(region_2500_id) as region_2500_id,
       sum(region_1250_id) as region_1250_id,
       sum(region_750_id) as region_750_id,
       sum(region_375_id) as region_375_id
from flickr_europe_region_id_seperate
group by photo_id;

alter table flickr_europe_region_id_combine add primary key 
(photo_id);


create table flickr_europe_with_region_id as
select f.*, 
       region_320000_id,
       region_160000_id,
       region_80000_id,
       region_40000_id,
       region_20000_id,
       region_10000_id,
       region_5000_id,
       region_2500_id,
       region_1250_id,
       region_750_id,
       region_375_id 
from flickr_europe_without_region_id f, flickr_europe_region_id_combine r
where f.photo_id = r.photo_id;

alter table flickr_europe_with_region_id add primary key 
(photo_id);
--------------------------------------------------------------------------------------------------
--慢, 尽管photo_id上必须有主键索引
--太多表的链接join操作，数据量大时，性能急剧下降

alter table flickr_europe_without_region_id add primary key 
(photo_id);

create table flickr_europe_with_region_id as
select t.*, 
       a1.id as region_320000_id,
       a2.id as region_160000_id,
       a3.id as region_80000_id,
       a4.id as region_40000_id,
       a5.id as region_20000_id,
       a6.id as region_10000_id,
       a7.id as region_5000_id,
       a8.id as region_2500_id,
       a9.id as region_1250_id,
       a10.id as region_750_id,
       a11.id as region_375_id
from flickr_europe_without_region_id t, 
     flickr_europe_area_320000 a1,
     flickr_europe_area_160000 a2,
     flickr_europe_area_80000 a3,
     flickr_europe_area_40000 a4,
     flickr_europe_area_20000 a5,
     flickr_europe_area_10000 a6,
     flickr_europe_area_5000 a7,
     flickr_europe_area_2500 a8,
     flickr_europe_area_1250 a9,
     flickr_europe_area_750 a10,
     flickr_europe_area_375 a11
where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||t.longitude||' '||t.latitude||')'), a1.geom::geometry) and 
      ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||t.longitude||' '||t.latitude||')'), a2.geom::geometry) and
      ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||t.longitude||' '||t.latitude||')'), a3.geom::geometry) and
      ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||t.longitude||' '||t.latitude||')'), a4.geom::geometry) and
      ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||t.longitude||' '||t.latitude||')'), a5.geom::geometry) and
      ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||t.longitude||' '||t.latitude||')'), a6.geom::geometry) and
      ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||t.longitude||' '||t.latitude||')'), a7.geom::geometry) and
      ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||t.longitude||' '||t.latitude||')'), a8.geom::geometry) and
      ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||t.longitude||' '||t.latitude||')'), a9.geom::geometry) and
      ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||t.longitude||' '||t.latitude||')'), a10.geom::geometry) and
      ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||t.longitude||' '||t.latitude||')'), a11.geom::geometry);
	  
	  
alter table flickr_europe_with_region_id add primary key 
(photo_id);


