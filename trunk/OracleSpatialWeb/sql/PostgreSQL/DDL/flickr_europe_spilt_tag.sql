-- Table: flickr_europe_spilt_tag

-- DROP TABLE flickr_europe_spilt_tag;

CREATE TABLE flickr_europe_spilt_tag
(
  photo_id numeric NOT NULL,
  tag character varying(30) NOT NULL,
  taken_date timestamp without time zone,
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
  CONSTRAINT flickr_europe_spilt_tag_pkey PRIMARY KEY (photo_id, tag),
  CONSTRAINT flickr_europe_spilt_tag_photo_id_fkey FOREIGN KEY (photo_id)
      REFERENCES flickr_europe (photo_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE flickr_europe_spilt_tag OWNER TO gennady_flickr;

--INDEX

CREATE INDEX
  ON flickr_europe_spilt_tag
  USING btree
  (region_320000_id, tag, taken_date);

CREATE INDEX
  ON flickr_europe_spilt_tag
  USING btree
  (region_160000_id, tag, taken_date);

CREATE INDEX
  ON flickr_europe_spilt_tag
  USING btree
  (region_80000_id, tag, taken_date);

CREATE INDEX
  ON flickr_europe_spilt_tag
  USING btree
  (region_40000_id, tag, taken_date);

CREATE INDEX
  ON flickr_europe_spilt_tag
  USING btree
  (region_20000_id, tag, taken_date);

CREATE INDEX
  ON flickr_europe_spilt_tag
  USING btree
  (region_10000_id, tag, taken_date);

CREATE INDEX
  ON flickr_europe_spilt_tag
  USING btree
  (region_5000_id, tag, taken_date);

CREATE INDEX
  ON flickr_europe_spilt_tag
  USING btree
  (region_2500_id, tag, taken_date);

CREATE INDEX
  ON flickr_europe_spilt_tag
  USING btree
  (region_1250_id, tag, taken_date);

CREATE INDEX
  ON flickr_europe_spilt_tag
  USING btree
  (region_750_id, tag, taken_date);

CREATE INDEX
  ON flickr_europe_spilt_tag
  USING btree
  (region_375_id, tag, taken_date);
