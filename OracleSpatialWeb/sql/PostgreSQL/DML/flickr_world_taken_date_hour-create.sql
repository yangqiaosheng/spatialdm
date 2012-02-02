set @date = select now();
print @date;

create table flickr_world_taken_date_hour as
select  region_2560000_id,
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
       region_625_id,
       to_char(TAKEN_DATE, 'yyyy-mm-dd@hh24') as date_str
from flickr_world;

set @date = select now();
print @date;

CREATE INDEX
  ON flickr_world_taken_date_hour
  USING btree
  (region_625_id, date_str);
CREATE INDEX
  ON flickr_world_taken_date_hour
  USING btree
  (region_1250_id, date_str);
CREATE INDEX
  ON flickr_world_taken_date_hour
  USING btree
  (region_2500_id, date_str);
CREATE INDEX
  ON flickr_world_taken_date_hour
  USING btree
  (region_5000_id, date_str);
CREATE INDEX
  ON flickr_world_taken_date_hour
  USING btree
  (region_10000_id, date_str);
CREATE INDEX
  ON flickr_world_taken_date_hour
  USING btree
  (region_20000_id, date_str);
CREATE INDEX
  ON flickr_world_taken_date_hour
  USING btree
  (region_40000_id, date_str);
CREATE INDEX
  ON flickr_world_taken_date_hour
  USING btree
  (region_80000_id, date_str);
CREATE INDEX
  ON flickr_world_taken_date_hour
  USING btree
  (region_160000_id, date_str);
CREATE INDEX
  ON flickr_world_taken_date_hour
  USING btree
  (region_320000_id, date_str);
CREATE INDEX
  ON flickr_world_taken_date_hour
  USING btree
  (region_640000_id, date_str);
CREATE INDEX
  ON flickr_world_taken_date_hour
  USING btree
  (region_1280000_id, date_str);
CREATE INDEX
  ON flickr_world_taken_date_hour
  USING btree
  (region_2560000_id, date_str);  

  
set @date = select now();
print @date;
