-- Table: flickr_europe

-- DROP TABLE flickr_europe;

CREATE TABLE flickr_europe
(
  photo_id numeric NOT NULL,
  user_id character varying(20),
  longitude double precision NOT NULL,
  latitude double precision NOT NULL,
  taken_date timestamp without time zone,
  upload_date timestamp without time zone,
  viewed integer,
  title character varying(1024),
  tags character varying(1024),
  smallurl character varying(100),
  place_id character varying(20),
  woe_id character varying(20),
  accuracy integer,
  region_checked integer NOT NULL DEFAULT 0,
  region_320000_id integer,
  region_160000_id integer,
  region_80000_id integer,
  region_40000_id integer,
  region_20000_id integer,
  region_10000_id integer,
  region_5000_id integer,
  region_2500_id integer,
  region_1250_id integer,
  region_750_id integer,
  region_375_id integer,
  CONSTRAINT flickr_euorpe_pk PRIMARY KEY (photo_id),
  CONSTRAINT flickr_europe_user_fk FOREIGN KEY (user_id)
      REFERENCES flickr_people (user_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,

  CONSTRAINT flickr_europe_r10000_fk FOREIGN KEY (region_10000_id)
      REFERENCES flickr_europe_area_10000 (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT flickr_europe_r160000_fk FOREIGN KEY (region_160000_id)
      REFERENCES flickr_europe_area_160000 (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT flickr_europe_r20000_fk FOREIGN KEY (region_20000_id)
      REFERENCES flickr_europe_area_20000 (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT flickr_europe_r320000_fk FOREIGN KEY (region_320000_id)
      REFERENCES flickr_europe_area_320000 (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT flickr_europe_r40000_fk FOREIGN KEY (region_40000_id)
      REFERENCES flickr_europe_area_40000 (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT flickr_europe_r5000_fk FOREIGN KEY (region_5000_id)
      REFERENCES flickr_europe_area_5000 (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT flickr_europe_r80000_fk FOREIGN KEY (region_80000_id)
      REFERENCES flickr_europe_area_80000 (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
  CONSTRAINT flickr_europe_r2500_fk FOREIGN KEY (region_2500_id)
      REFERENCES flickr_europe_area_2500 (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT flickr_europe_r1250_fk FOREIGN KEY (region_1250_id)
      REFERENCES flickr_europe_area_1250 (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT flickr_europe_r750_fk FOREIGN KEY (region_750_id)
      REFERENCES flickr_europe_area_750 (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT flickr_europe_r375_fk FOREIGN KEY (region_375_id)
      REFERENCES flickr_europe_area_375 (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
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

CREATE INDEX flickr_europe_taken_date_idx
  ON flickr_europe
  USING btree
  (taken_date DESC);

CREATE INDEX flickr_europe_taken_date_hour_idx
  ON flickr_europe
  USING btree
  (taken_date_hour DESC);

CREATE INDEX flickr_europe_r5000_id_idx
  ON flickr_europe
  USING btree
  (taken_date DESC, region_5000_id);
CREATE INDEX flickr_europe_r10000_id_idx
  ON flickr_europe
  USING btree
  (taken_date DESC, region_10000_id);
CREATE INDEX flickr_europe_r20000_id_idx
  ON flickr_europe
  USING btree
  (taken_date DESC, region_20000_id);
CREATE INDEX flickr_europe_r40000_id_idx
  ON flickr_europe
  USING btree
  (taken_date DESC, region_40000_id);
CREATE INDEX flickr_europe_r80000_id_idx
  ON flickr_europe
  USING btree
  (taken_date DESC, region_80000_id);
CREATE INDEX flickr_europe_r160000_id_idx
  ON flickr_europe
  USING btree
  (taken_date DESC, region_160000_id);
CREATE INDEX flickr_europe_r320000_id_idx
  ON flickr_europe
  USING btree
  (taken_date DESC, region_320000_id);