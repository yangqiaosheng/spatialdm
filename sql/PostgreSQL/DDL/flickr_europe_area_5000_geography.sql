-- Table: flickr_europe_area_5000

-- DROP TABLE flickr_europe_area_5000;

CREATE TABLE flickr_europe_area_5000
(
  id integer NOT NULL,
  "name" character varying(5) NOT NULL,
  center_x numeric NOT NULL,
  center_y numeric NOT NULL,
  area numeric NOT NULL,
  geom geography(MultiPolygonZM,4326) NOT NULL,
  CONSTRAINT flickr_europe_area_5000_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE flickr_europe_area_5000 OWNER TO gennady_flickr;

-- Index: flickr_europe_area_5000_geom_gist

-- DROP INDEX flickr_europe_area_5000_geom_gist;

CREATE INDEX flickr_europe_area_5000_geom_gist
  ON flickr_europe_area_5000
  USING gist
  (geom);

