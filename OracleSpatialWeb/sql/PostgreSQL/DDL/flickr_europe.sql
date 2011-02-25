-- Table: flickr_europe

-- DROP TABLE flickr_europe;

CREATE TABLE flickr_europe
(
  photo_id numeric NOT NULL,
  user_id character varying(20),
  longitude numeric(15,0) NOT NULL,
  latitude numeric(15,0) NOT NULL,
  taken_date date,
  upload_date date,
  viewed numeric,
  title character varying(255),
  smallurl character varying(100),
  place_id character varying(20),
  woe_id character varying(20),
  accuracy numeric,
  region_checked integer NOT NULL DEFAULT 0,
  region_5000_id numeric,
  region_10000_id numeric,
  region_20000_id numeric,
  region_40000_id numeric,
  region_80000_id numeric,
  region_160000_id numeric,
  region_320000_id numeric,
  CONSTRAINT flickr_euorpe_pk PRIMARY KEY (photo_id),
  CONSTRAINT flickr_europe_fk FOREIGN KEY (user_id)
      REFERENCES flickr_people (user_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE flickr_europe OWNER TO gennady_flickr;

-- Index: flickr_europe_lonlat_idx

-- DROP INDEX flickr_europe_lonlat_idx;

CREATE INDEX flickr_europe_lonlat_idx
  ON flickr_europe
  USING btree
  (longitude, latitude);

-- Index: flickr_europe_taken_date_idx

-- DROP INDEX flickr_europe_taken_date_idx;

CREATE INDEX flickr_europe_taken_date_idx
  ON flickr_europe
  USING btree
  (taken_date DESC);

