create table flickr_world_dth as
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


CREATE INDEX fw_dth_R625_idx
  ON flickr_world_dth
  (region_625_id, date_str);

CREATE INDEX fw_dth_R1250_idx
  ON flickr_world_dth
  (region_1250_id, date_str);

CREATE INDEX fw_dth_R2500_idx
  ON flickr_world_dth
  (region_2500_id, date_str);

CREATE INDEX fw_dth_R5000_idx
  ON flickr_world_dth
  (region_5000_id, date_str);

CREATE INDEX fw_dth_R10000_idx
  ON flickr_world_dth
  (region_10000_id, date_str);

CREATE INDEX fw_dth_R20000_idx
  ON flickr_world_dth
  (region_20000_id, date_str);

CREATE INDEX fw_dth_R40000_idx
  ON flickr_world_dth
  (region_40000_id, date_str);

CREATE INDEX fw_dth_R80000_idx
  ON flickr_world_dth
  (region_80000_id, date_str);

CREATE INDEX fw_dth_R160000_idx
  ON flickr_world_dth
  (region_160000_id, date_str);

CREATE INDEX fw_dth_R320000_idx
  ON flickr_world_dth
  (region_320000_id, date_str);

CREATE INDEX fw_dth_R640000_idx
  ON flickr_world_dth
  (region_640000_id, date_str);

CREATE INDEX fw_dth_R1280000_idx
  ON flickr_world_dth
  (region_1280000_id, date_str);

CREATE INDEX fw_dth_R2560000_idx
  ON flickr_world_dth
  (region_2560000_id, date_str);

CREATE INDEX fw_dth_R40000_idx
  ON flickr_world_dth
  (region_40000_id, date_str);