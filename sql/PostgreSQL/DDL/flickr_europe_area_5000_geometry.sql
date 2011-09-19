-- Table: flickr_europe_area_5000

-- DROP TABLE flickr_europe_area_5000;

CREATE TABLE flickr_europe_area_5000
(
  id integer NOT NULL,
  "name" character varying(5),
  center_x numeric,
  center_y numeric,
  area numeric,
  geom geometry,
  CONSTRAINT flickr_europe_area_5000_pkey PRIMARY KEY (id),
  CONSTRAINT enforce_dims_geom CHECK (st_ndims(geom) = 4),
  CONSTRAINT enforce_geotype_geom CHECK (geometrytype(geom) = 'MULTIPOLYGON'::text OR geom IS NULL),
  CONSTRAINT enforce_srid_geom CHECK (st_srid(geom) = 4326)
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

