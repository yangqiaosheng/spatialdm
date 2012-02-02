--快， 利用临时表和索引的group by功能，避免多表连接

set @date = select now();
print @date;
create table flickr_europe_46m_region_id as
select f.photo_id, a.radius, a.id as region_id from flickr_europe_area a, flickr_europe_46m f
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||f.longitude||' '||f.latitude||')'), a.geom::geometry);

set @date = select now();
print @date;

create index on flickr_europe_46m_region_id
  USING btree
  (photo_id);

set @date = select now();
print @date;
--dirty result

delete from flickr_europe_46m where photo_id in
(select photo_id from flickr_europe_46m_region_id group by photo_id having count(*) > 11);

delete from flickr_europe_46m_region_id where photo_id in
(select photo_id from flickr_europe_46m_region_id group by photo_id having count(*) > 11);

set @date = select now();
print @date;

create table flickr_europe_46m_region_id_seperate as
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
 from flickr_europe_46m_region_id;

create index on flickr_europe_46m_region_id_seperate
  USING btree
  (photo_id);
set @date = select now();
print @date;
 
create table flickr_europe_46m_region_id_combine as
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
from flickr_europe_46m_region_id_seperate
group by photo_id;

alter table flickr_europe_46m_region_id_combine add primary key 
(photo_id);
set @date = select now();
print @date;

create table flickr_europe as
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
from flickr_europe_46m f, flickr_europe_46m_region_id_combine r
where f.photo_id = r.photo_id;
set @date = select now();
print @date;

alter table flickr_europe add primary key 
(photo_id);

CREATE INDEX
  ON flickr_europe
  USING btree
  (region_375_id, taken_date DESC);
CREATE INDEX
  ON flickr_europe
  USING btree
  (region_750_id, taken_date DESC);
CREATE INDEX
  ON flickr_europe
  USING btree
  (region_1250_id, taken_date DESC);
CREATE INDEX
  ON flickr_europe
  USING btree
  (region_2500_id, taken_date DESC);
CREATE INDEX
  ON flickr_europe
  USING btree
  (region_5000_id, taken_date DESC);
CREATE INDEX
  ON flickr_europe
  USING btree
  (region_10000_id, taken_date DESC);
CREATE INDEX
  ON flickr_europe
  USING btree
  (region_20000_id, taken_date DESC);
CREATE INDEX
  ON flickr_europe
  USING btree
  (region_40000_id, taken_date DESC);
CREATE INDEX
  ON flickr_europe
  USING btree
  (region_80000_id, taken_date DESC);
CREATE INDEX
  ON flickr_europe
  USING btree
  (region_160000_id, taken_date DESC);
CREATE INDEX
  ON flickr_europe
  USING btree
  (region_320000_id, taken_date DESC);
------------------------------------------------------

set @date = select now();
print @date;

create table flickr_europe_taken_date_hour as
select region_320000_id,
       region_160000_id,
       region_80000_id,
       region_40000_id,
       region_20000_id,
       region_10000_id,
       region_5000_id,
       region_2500_id,
       region_1250_id,
       region_750_id,
       region_375_id,
       to_char(TAKEN_DATE, 'yyyy-mm-dd@hh24') as date_str
from flickr_europe;

set @date = select now();
print @date;

CREATE INDEX
  ON flickr_europe_taken_date_hour
  USING btree
  (region_375_id, date_str);
CREATE INDEX
  ON flickr_europe_taken_date_hour
  USING btree
  (region_750_id, date_str);
CREATE INDEX
  ON flickr_europe_taken_date_hour
  USING btree
  (region_1250_id, date_str);
CREATE INDEX
  ON flickr_europe_taken_date_hour
  USING btree
  (region_2500_id, date_str);
CREATE INDEX
  ON flickr_europe_taken_date_hour
  USING btree
  (region_5000_id, date_str);
CREATE INDEX
  ON flickr_europe_taken_date_hour
  USING btree
  (region_10000_id, date_str);
CREATE INDEX
  ON flickr_europe_taken_date_hour
  USING btree
  (region_20000_id, date_str);
CREATE INDEX
  ON flickr_europe_taken_date_hour
  USING btree
  (region_40000_id, date_str);
CREATE INDEX
  ON flickr_europe_taken_date_hour
  USING btree
  (region_80000_id, date_str);
CREATE INDEX
  ON flickr_europe_taken_date_hour
  USING btree
  (region_160000_id, date_str);
CREATE INDEX
  ON flickr_europe_taken_date_hour
  USING btree
  (region_320000_id, date_str);
  
set @date = select now();
print @date;
