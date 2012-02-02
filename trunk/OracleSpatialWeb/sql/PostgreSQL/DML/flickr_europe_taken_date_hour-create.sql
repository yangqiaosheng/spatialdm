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

-----------------------------------------------------------------  
create table flickr_europe_count_320000_seperate as
select region_320000_id, date_str, count(*)
from flickr_europe_taken_date_hour t group by region_320000_id, date_str;
set @date = select now();
print @date;

create table flickr_europe_count_160000_seperate as
select region_160000_id, date_str, count(*)
from flickr_europe_taken_date_hour t group by region_160000_id, date_str;
set @date = select now();
print @date;

create table flickr_europe_count_80000_seperate as
select region_80000_id, date_str, count(*)
from flickr_europe_taken_date_hour t group by region_80000_id, date_str;
set @date = select now();
print @date;

create table flickr_europe_count_40000_seperate as
select region_40000_id, date_str, count(*)
from flickr_europe_taken_date_hour t group by region_40000_id, date_str;
set @date = select now();
print @date;

create table flickr_europe_count_20000_seperate as
select region_20000_id, date_str, count(*)
from flickr_europe_taken_date_hour t group by region_20000_id, date_str;
set @date = select now();
print @date;

create table flickr_europe_count_10000_seperate as
select region_10000_id, date_str, count(*)
from flickr_europe_taken_date_hour t group by region_10000_id, date_str;
set @date = select now();
print @date;

create table flickr_europe_count_5000_seperate as
select region_5000_id, date_str, count(*)
from flickr_europe_taken_date_hour t group by region_5000_id, date_str;
set @date = select now();
print @date;

create table flickr_europe_count_2500_seperate as
select region_2500_id, date_str, count(*)
from flickr_europe_taken_date_hour t group by region_2500_id, date_str;
set @date = select now();
print @date;

create table flickr_europe_count_1250_seperate as
select region_1250_id, date_str, count(*)
from flickr_europe_taken_date_hour t group by region_1250_id, date_str;
set @date = select now();
print @date;

create table flickr_europe_count_750_seperate as
select region_750_id, date_str, count(*)
from flickr_europe_taken_date_hour t group by region_750_id, date_str;
set @date = select now();
print @date;

create table flickr_europe_count_375_seperate as
select region_375_id, date_str, count(*)
from flickr_europe_taken_date_hour t group by region_375_id, date_str;
set @date = select now();
print @date;

CREATE INDEX
  ON flickr_europe_count_320000_seperate
  USING btree
  (region_320000_id); 
CREATE INDEX
  ON flickr_europe_count_160000_seperate
  USING btree
  (region_160000_id); 
CREATE INDEX
  ON flickr_europe_count_80000_seperate
  USING btree
  (region_80000_id); 
CREATE INDEX
  ON flickr_europe_count_40000_seperate
  USING btree
  (region_40000_id); 
CREATE INDEX
  ON flickr_europe_count_20000_seperate
  USING btree
  (region_20000_id); 
CREATE INDEX
  ON flickr_europe_count_10000_seperate
  USING btree
  (region_10000_id); 
CREATE INDEX
  ON flickr_europe_count_5000_seperate
  USING btree
  (region_5000_id); 
CREATE INDEX
  ON flickr_europe_count_2500_seperate
  USING btree
  (region_2500_id); 
CREATE INDEX
  ON flickr_europe_count_1250_seperate
  USING btree
  (region_1250_id);
CREATE INDEX
  ON flickr_europe_count_750_seperate
  USING btree
  (region_750_id); 
CREATE INDEX
  ON flickr_europe_count_375_seperate
  USING btree
  (region_375_id);

 

