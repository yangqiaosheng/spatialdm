CREATE TABLE flickr_world
(
  photo_id numeric NOT NULL,
  user_id character varying(20),
  longitude double precision,
  latitude double precision,
  taken_date timestamp without time zone,
  upload_date timestamp without time zone,
  viewed integer,
  title character varying(1024),
  description text,
  tags character varying(1024),
  smallurl character varying(100),
  place_id character varying(20),
  woe_id character varying(20),
  accuracy integer,
  region_2560000_id integer,
  region_1280000_id integer,
  region_640000_id integer,
  region_320000_id integer,
  region_160000_id integer,
  region_80000_id integer,
  region_40000_id integer,
  region_20000_id integer,
  region_10000_id integer,
  region_5000_id integer,
  region_2500_id integer,
  region_1250_id integer,
  region_625_id integer,
  CONSTRAINT flickr_world_pkey PRIMARY KEY (photo_id)
)


CREATE INDEX
  ON flickr_world
  USING btree
  (region_625_id, taken_date DESC);
CREATE INDEX
  ON flickr_world
  USING btree
  (region_1250_id, taken_date DESC);
CREATE INDEX
  ON flickr_world
  USING btree
  (region_2500_id, taken_date DESC);
CREATE INDEX
  ON flickr_world
  USING btree
  (region_5000_id, taken_date DESC);
CREATE INDEX
  ON flickr_world
  USING btree
  (region_10000_id, taken_date DESC);
CREATE INDEX
  ON flickr_world
  USING btree
  (region_20000_id, taken_date DESC);
CREATE INDEX
  ON flickr_world
  USING btree
  (region_40000_id, taken_date DESC);
CREATE INDEX
  ON flickr_world
  USING btree
  (region_80000_id, taken_date DESC);
CREATE INDEX
  ON flickr_world
  USING btree
  (region_160000_id, taken_date DESC);
CREATE INDEX
  ON flickr_world
  USING btree
  (region_320000_id, taken_date DESC);
CREATE INDEX
  ON flickr_world
  USING btree
  (region_640000_id, taken_date DESC);
CREATE INDEX
  ON flickr_world
  USING btree
  (region_1280000_id, taken_date DESC);
CREATE INDEX
  ON flickr_world
  USING btree
  (region_2560000_id, taken_date DESC);
