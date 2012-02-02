set @date = select now();
print @date;

create index on flickr_europe_40m
  USING btree
  (viewed desc);

set @date = select now();
print @date;

create table flickr_europe_topviewed_5m as
select * from flickr_europe_40m 
order by viewed desc 
limit 5000000;

set @date = select now();
print @date;

alter table flickr_europe_topviewed_5m 
drop column region_320000_id;
alter table flickr_europe_topviewed_5m 
drop column region_160000_id;
alter table flickr_europe_topviewed_5m 
drop column region_80000_id;
alter table flickr_europe_topviewed_5m 
drop column region_40000_id;
alter table flickr_europe_topviewed_5m 
drop column region_20000_id;
alter table flickr_europe_topviewed_5m 
drop column region_10000_id;
alter table flickr_europe_topviewed_5m 
drop column region_5000_id;
alter table flickr_europe_topviewed_5m 
drop column region_2500_id;
alter table flickr_europe_topviewed_5m 
drop column region_1250_id;
alter table flickr_europe_topviewed_5m 
drop column region_750_id;
alter table flickr_europe_topviewed_5m 
drop column region_375_id;
alter table flickr_europe_topviewed_5m 
drop column region_checked;

alter table flickr_europe_topviewed_5m add primary key 
(photo_id);

set @date = select now();
print @date;

create table flickr_europe_topviewed_5m_region_id as
select f.photo_id, a.radius, a.id as region_id from flickr_europe_area a, flickr_europe_topviewed_5m f
	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||f.longitude||' '||f.latitude||')'), a.geom::geometry);

set @date = select now();
print @date;


create index on flickr_europe_topviewed_5m_region_id
  USING btree
  (photo_id);
  
--dirty result

delete from flickr_europe_topviewed_5m where photo_id in 
(select photo_id from flickr_europe_topviewed_5m_region_id group by photo_id having count(*) > 11);

delete from flickr_europe_topviewed_5m_region_id where photo_id in 
(select photo_id from flickr_europe_topviewed_5m_region_id group by photo_id having count(*) > 11);

set @date = select now();
print @date;

create table flickr_europe_topviewed_5m_region_id_seperate as
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
 from flickr_europe_topviewed_5m_region_id;

create index on flickr_europe_topviewed_5m_region_id_seperate
  USING btree
  (photo_id);

set @date = select now();
print @date;

create table flickr_europe_topviewed_5m_region_id_combine as
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
from flickr_europe_topviewed_5m_region_id_seperate
group by photo_id;

alter table flickr_europe_topviewed_5m_region_id_combine add primary key 
(photo_id);

set @date = select now();
print @date;

create table flickr_europe_topviewed_5m_with_region_id as
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
from flickr_europe_topviewed_5m f, flickr_europe_topviewed_5m_region_id_combine r
where f.photo_id = r.photo_id;

alter table flickr_europe_topviewed_5m_with_region_id add primary key 
(photo_id);

set @date = select now();
print @date;


-----------------------------------------------------------
---output:


[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-17 01:17:56.535+02")
[QUERY    ] create index on flickr_europe_40m
              USING btree
              (viewed desc)
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-17 01:23:57.584+02")
[QUERY    ] create table flickr_europe_topviewed_5m as
            select * from flickr_europe_40m 
            order by viewed desc 
            limit 5000000
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-17 01:34:42.63+02")
[QUERY    ] alter table flickr_europe_topviewed_5m 
            drop column region_320000_id
[QUERY    ] alter table flickr_europe_topviewed_5m 
            drop column region_160000_id
[QUERY    ] alter table flickr_europe_topviewed_5m 
            drop column region_80000_id
[QUERY    ] alter table flickr_europe_topviewed_5m 
            drop column region_40000_id
[QUERY    ] alter table flickr_europe_topviewed_5m 
            drop column region_20000_id
[QUERY    ] alter table flickr_europe_topviewed_5m 
            drop column region_10000_id
[QUERY    ] alter table flickr_europe_topviewed_5m 
            drop column region_5000_id
[WARNING  ] alter table flickr_europe_topviewed_5m 
            drop column region_2500_id
            错误:  关系 "flickr_europe_topviewed_5m" 的 "region_2500_id" 字段不存在
[WARNING  ] alter table flickr_europe_topviewed_5m 
            drop column region_1250_id
            错误:  关系 "flickr_europe_topviewed_5m" 的 "region_1250_id" 字段不存在
[WARNING  ] alter table flickr_europe_topviewed_5m 
            drop column region_750_id
            错误:  关系 "flickr_europe_topviewed_5m" 的 "region_750_id" 字段不存在
[WARNING  ] alter table flickr_europe_topviewed_5m 
            drop column region_375_id
            错误:  关系 "flickr_europe_topviewed_5m" 的 "region_375_id" 字段不存在
[QUERY    ] alter table flickr_europe_topviewed_5m 
            drop column region_checked
[QUERY    ] alter table flickr_europe_topviewed_5m add primary key 
            (photo_id)
            注意:  ALTER TABLE / ADD PRIMARY KEY 将要为表 "flickr_europe_topviewed_5m" 创建隐含索引 "flickr_europe_topviewed_5m_pkey"
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-17 01:37:30.455+02")
[QUERY    ] create table flickr_europe_topviewed_5m_region_id as
            select f.photo_id, a.radius, a.id as region_id from flickr_europe_area a, flickr_europe_topviewed_5m f
            	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||f.longitude||' '||f.latitude||')'), a.geom::geometry)
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-17 05:26:18.479+02")
[QUERY    ] create index on flickr_europe_topviewed_5m_region_id
              USING btree
              (photo_id)
[WARNING  ] delete from flickr_europe_topviewed_5m where photo_id = 
            (select photo_id from flickr_europe_topviewed_5m_region_id group by photo_id having count(*) > 11)
            错误:  作为一个表达式使用的子查询返回了多列
[WARNING  ] delete from flickr_europe_topviewed_5m_region_id where photo_id = 
            (select photo_id from flickr_europe_topviewed_5m_region_id group by photo_id having count(*) > 11)
            错误:  作为一个表达式使用的子查询返回了多列
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-17 05:41:36.104+02")
[QUERY    ] create table flickr_europe_topviewed_5m_region_id_seperate as
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
             from flickr_europe_topviewed_5m_region_id
[QUERY    ] create index on flickr_europe_topviewed_5m_region_id_seperate
              USING btree
              (photo_id)
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-17 05:55:59.035+02")
[QUERY    ] create table flickr_europe_topviewed_5m_region_id_combine as
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
            from flickr_europe_topviewed_5m_region_id_seperate
            group by photo_id
[QUERY    ] alter table flickr_europe_topviewed_5m_region_id_combine add primary key 
            (photo_id)
            注意:  ALTER TABLE / ADD PRIMARY KEY 将要为表 "flickr_europe_topviewed_5m_region_id_combine" 创建隐含索引 "flickr_europe_topviewed_5m_region_id_combine_pkey"
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-17 05:59:26.219+02")
[QUERY    ] create table flickr_europe_topviewed_5m_with_region_id as
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
            from flickr_europe_topviewed_5m f, flickr_europe_topviewed_5m_region_id_combine r
            where f.photo_id = r.photo_id
[QUERY    ] alter table flickr_europe_topviewed_5m_with_region_id add primary key 
            (photo_id)
            注意:  ALTER TABLE / ADD PRIMARY KEY 将要为表 "flickr_europe_topviewed_5m_with_region_id" 创建隐含索引 "flickr_europe_topviewed_5m_with_region_id_pkey"
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-17 06:09:11.126+02")


-----------------------------------------------------------------------------------------------------------
--topviewed_15m

[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-17 16:31:46.823+02")
[QUERY    ] create index on flickr_europe
              USING btree
              (viewed desc)
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-17 16:42:33.862+02")
[QUERY    ] create table flickr_europe_topviewed_15m as
            select * from flickr_europe
            order by viewed desc 
            limit 15000000
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-17 17:02:27.439+02")
[QUERY    ] alter table flickr_europe_topviewed_15m add primary key 
            (photo_id)
            HINWEIS:  ALTER TABLE / ADD PRIMARY KEY erstellt implizit einen Index »flickr_europe_topviewed_15m_pkey« für Tabelle »flickr_europe_topviewed_15m«
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-17 17:11:12.509+02")
[QUERY    ] create table flickr_europe_topviewed_15m_region_id as
            select f.photo_id, a.radius, a.id as region_id from flickr_europe_area a, flickr_europe_topviewed_15m f
            	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||f.longitude||' '||f.latitude||')'), a.geom::geometry)
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-18 03:02:55.288+02")
[QUERY    ] create index on flickr_europe_topviewed_15m_region_id
              USING btree
              (photo_id)
[QUERY    ] delete from flickr_europe_topviewed_15m where photo_id in 
            (select photo_id from flickr_europe_topviewed_15m_region_id group by photo_id having count(*) > 11)
[QUERY    ] delete from flickr_europe_topviewed_15m_region_id where photo_id in 
            (select photo_id from flickr_europe_topviewed_15m_region_id group by photo_id having count(*) > 11)
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-18 04:02:24.852+02")
[QUERY    ] create table flickr_europe_topviewed_15m_region_id_seperate as
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
             from flickr_europe_topviewed_15m_region_id
[QUERY    ] create index on flickr_europe_topviewed_15m_region_id_seperate
              USING btree
              (photo_id)
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-18 04:55:37.046+02")
[WARNING  ] create table flickr_europe_topviewed_15m_region_id_combine as
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
            from flickr_europe_topviewed_15m_region_id_seperate
            group by photo_id
            FEHLER:  Speicher aufgebraucht
            DETAIL:  Fehler bei Anfrage mit Größe 4056.
[WARNING  ] alter table flickr_europe_topviewed_15m_region_id_combine add primary key 
            (photo_id)
            FEHLER:  Relation »flickr_europe_topviewed_15m_region_id_combine« existiert nicht
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-18 05:02:15.317+02")


----------------------------------------------------------------------------------------------------
---46Million Europe

[QUERY    ] create table flickr_europe_region_id as
            select f.photo_id, a.radius, a.id as region_id from flickr_europe_area a, flickr_europe_46m f
            	where ST_Intersects(ST_GeomFromEWKT('SRID=4326;POINT('||f.longitude||' '||f.latitude||')'), a.geom::geometry)
[WARNING  ] create index on flickr_europe_region_id
              USING btree
              (photo_id)
            ERROR:  could not write block 1576388 of temporary file: No space left on device
            HINT:  Perhaps out of disk space?
[WARNING  ] delete from flickr_europe where photo_id in
            (select photo_id from flickr_europe_region_id group by photo_id having count(*) > 11)
            ERROR:  relation "flickr_europe" does not exist
            LINE 1: delete from flickr_europe where photo_id in
                                ^
[WARNING  ] delete from flickr_europe_region_id where photo_id in
            (select photo_id from flickr_europe_region_id group by photo_id having count(*) > 11)
            ERROR:  out of memory
            DETAIL:  Failed on request of size 27.
[WARNING  ] create table flickr_europe_region_id_seperate as
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
             from flickr_europe_region_id
            ERROR:  could not extend file "pg_tblspc/29773/PG_9.0_201008051/29801/33461.12": No space left on device
            HINT:  Check free disk space.
[WARNING  ] create index on flickr_europe_region_id_seperate
              USING btree
              (photo_id)
            ERROR:  relation "flickr_europe_region_id_seperate" does not exist
[WARNING  ] create table flickr_europe_region_id_combine as
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
            group by photo_id
            ERROR:  relation "flickr_europe_region_id_seperate" does not exist
            LINE 14: from flickr_europe_region_id_seperate
                          ^
[WARNING  ] alter table flickr_europe_region_id_combine add primary key 
            (photo_id)
            ERROR:  relation "flickr_europe_region_id_combine" does not exist
[WARNING  ] create table flickr_europe as
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
            from flickr_europe_46m f, flickr_europe_region_id_combine r
            where f.photo_id = r.photo_id
            ERROR:  relation "flickr_europe_region_id_combine" does not exist
            LINE 14: from flickr_europe_46m f, flickr_europe_region_id_combine r
                                               ^
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-24 04:43:54.625+02")
[WARNING  ] alter table flickr_europe add primary key 
            (photo_id)
            ERROR:  relation "flickr_europe" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe
              USING btree
              (region_375_id, taken_date DESC)
            ERROR:  relation "flickr_europe" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe
              USING btree
              (region_750_id, taken_date DESC)
            ERROR:  relation "flickr_europe" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe
              USING btree
              (region_1250_id, taken_date DESC)
            ERROR:  relation "flickr_europe" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe
              USING btree
              (region_2500_id, taken_date DESC)
            ERROR:  relation "flickr_europe" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe
              USING btree
              (region_5000_id, taken_date DESC)
            ERROR:  relation "flickr_europe" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe
              USING btree
              (region_10000_id, taken_date DESC)
            ERROR:  relation "flickr_europe" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe
              USING btree
              (region_20000_id, taken_date DESC)
            ERROR:  relation "flickr_europe" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe
              USING btree
              (region_40000_id, taken_date DESC)
            ERROR:  relation "flickr_europe" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe
              USING btree
              (region_80000_id, taken_date DESC)
            ERROR:  relation "flickr_europe" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe
              USING btree
              (region_160000_id, taken_date DESC)
            ERROR:  relation "flickr_europe" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe
              USING btree
              (region_320000_id, taken_date DESC)
            ERROR:  relation "flickr_europe" does not exist
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-24 04:43:55.453+02")
[WARNING  ] create table flickr_europe_taken_date_hour as
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
            from flickr_europe
            ERROR:  relation "flickr_europe" does not exist
            LINE 14: from flickr_europe
                          ^
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-24 04:43:55.562+02")
[WARNING  ] CREATE INDEX
              ON flickr_europe_taken_date_hour
              USING btree
              (region_375_id, date_str)
            ERROR:  relation "flickr_europe_taken_date_hour" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe_taken_date_hour
              USING btree
              (region_750_id, date_str)
            ERROR:  relation "flickr_europe_taken_date_hour" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe_taken_date_hour
              USING btree
              (region_1250_id, date_str)
            ERROR:  relation "flickr_europe_taken_date_hour" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe_taken_date_hour
              USING btree
              (region_2500_id, date_str)
            ERROR:  relation "flickr_europe_taken_date_hour" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe_taken_date_hour
              USING btree
              (region_5000_id, date_str)
            ERROR:  relation "flickr_europe_taken_date_hour" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe_taken_date_hour
              USING btree
              (region_10000_id, date_str)
            ERROR:  relation "flickr_europe_taken_date_hour" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe_taken_date_hour
              USING btree
              (region_20000_id, date_str)
            ERROR:  relation "flickr_europe_taken_date_hour" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe_taken_date_hour
              USING btree
              (region_40000_id, date_str)
            ERROR:  relation "flickr_europe_taken_date_hour" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe_taken_date_hour
              USING btree
              (region_80000_id, date_str)
            ERROR:  relation "flickr_europe_taken_date_hour" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe_taken_date_hour
              USING btree
              (region_160000_id, date_str)
            ERROR:  relation "flickr_europe_taken_date_hour" does not exist
[WARNING  ] CREATE INDEX
              ON flickr_europe_taken_date_hour
              USING btree
              (region_320000_id, date_str)
            ERROR:  relation "flickr_europe_taken_date_hour" does not exist
[QUERY    ] select now()
[PGSCRIPT ] ("2011-08-24 04:43:55.828+02")
